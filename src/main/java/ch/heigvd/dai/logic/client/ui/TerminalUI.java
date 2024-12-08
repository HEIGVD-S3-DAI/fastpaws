package ch.heigvd.dai.logic.client.ui;

import ch.heigvd.dai.logic.client.ClientProtocol;
import ch.heigvd.dai.logic.client.ClientState;
import ch.heigvd.dai.logic.client.ui.display.DisplayState;
import ch.heigvd.dai.logic.client.ui.display.GameOverDisplayState;
import ch.heigvd.dai.logic.client.ui.display.LobbyDisplayState;
import ch.heigvd.dai.logic.client.ui.display.RaceDisplayState;
import ch.heigvd.dai.logic.client.ui.event.UIEvent;
import ch.heigvd.dai.logic.client.ui.event.UIEventListener;
import ch.heigvd.dai.logic.shared.BaseState;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import java.io.IOException;
import java.util.logging.Logger;

public class TerminalUI extends Thread implements UIEventListener {
  private static final Logger LOGGER = Logger.getLogger(TerminalUI.class.getName());
  private static final int REFRESH_INTERVAL_MS = 50;
  private boolean running = true;
  private final ClientState state;
  private final ClientProtocol network;
  private DisplayState currentDisplay;

  private Terminal terminal;
  private Screen screen;

  public TerminalUI(ClientState state, ClientProtocol network) {
    this.state = state;
    this.network = network;
    this.currentDisplay = new LobbyDisplayState(this);
    state.setUIEventListener(this);
  }

  @Override
  public void onUIEvent(UIEvent event) {
    switch (event.getType()) {
      case GAME_STATE_CHANGED:
        BaseState.GameState newState = (BaseState.GameState) event.getData();
        switch (newState) {
          case WAITING:
            currentDisplay = new LobbyDisplayState(this);
            break;
          case RUNNING:
            currentDisplay = new RaceDisplayState(this);
            break;
        }
        break;
      case END_GAME:
        currentDisplay = new GameOverDisplayState(this, (String) event.getData());
        break;
      case RACE_TEXT_RECEIVED:
        if (currentDisplay instanceof RaceDisplayState) {
          ((RaceDisplayState) currentDisplay).setText((String) event.getData());
        }
        break;
      default:
        break;
    }
  }

  public void run() {
    try {
      initializeTerminal();
      gameLoop();
    } catch (IOException | InterruptedException e) {
      LOGGER.severe("Could not initialize or run the game: " + e.getMessage());
    } finally {
      cleanup();
    }
  }

  private void initializeTerminal() throws IOException {
    terminal = new DefaultTerminalFactory().createTerminal();
    screen = new TerminalScreen(terminal);

    screen.startScreen();
    screen.setCursorPosition(null); // Hide cursor
  }

  private void gameLoop() throws IOException, InterruptedException {
    long lastRenderTime = 0;

    while (running) {
      KeyStroke keyStroke = screen.pollInput();
      if (keyStroke != null) {
        currentDisplay.handleInput(keyStroke);
      }

      long currentTime = System.currentTimeMillis();
      if (currentTime - lastRenderTime >= REFRESH_INTERVAL_MS) {
        TextGraphics tg = screen.newTextGraphics();
        tg.fill(' ');
        currentDisplay.render(tg);
        screen.refresh();
        Thread.yield(); // Allow other threads to take cpu time
        lastRenderTime = currentTime;
      }

      Thread.sleep(10);
    }
  }

  public void end() {
    running = false;
  }

  private void cleanup() {
    try {
      if (screen != null) {
        screen.close();
      }
    } catch (IllegalStateException e) {
      // We ignore for now (exception exiting private mode)
    } catch (IOException e) {
      LOGGER.severe("Error cleaning up terminal: " + e.getMessage());
    }
  }

  // Getters for display states to access
  public Terminal getTerminal() {
    return terminal;
  }

  public ClientState getClientState() {
    return state;
  }

  public ClientProtocol getNetwork() {
    return network;
  }
}
