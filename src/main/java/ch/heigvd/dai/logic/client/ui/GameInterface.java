package ch.heigvd.dai.logic.client.ui;

import ch.heigvd.dai.logic.client.ClientState;
import ch.heigvd.dai.logic.shared.Player;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

public class GameInterface extends Thread {

  private static final Logger LOGGER = Logger.getLogger(GameInterface.class.getName());
  private boolean running = true;
  private final ClientState state;
  private String[] text;

  private static final String[] HELLO_ASCII = {
    "   ____         __  ___                ",
    "  / __/__ ____ / /_/ _ \\___ __    _____",
    " / _// _ `(_-</ __/ ___/ _ `/ |/|/ (_-<",
    "/_/  \\_,_/___/\\__/_/   \\_,_/|__,__/___/",
  };

  public GameInterface(ClientState state) {
    this.state = state;
  }

  public void run() {
    try (Terminal terminal = new DefaultTerminalFactory().createTerminal();
        Screen screen = new TerminalScreen(terminal)) {

      // Save initial terminal state
      terminal.clearScreen();

      screen.startScreen();
      screen.setCursorPosition(null); // Hide cursor

      // Set background color and clear
      screen.clear();

      // Main loop
      while (running) {
        KeyStroke keyStroke = screen.pollInput();
        if (keyStroke != null) {
          handleKeyInput(keyStroke);
        }
        render(screen);
        Thread.sleep(100); // Example of a simple delay in the loop
      }

      // Reset terminal to initial state
      reset(screen);

    } catch (IOException | InterruptedException e) {
      LOGGER.severe("Could not initialize or run the game: " + e.getMessage());
    }
  }

  private void handleKeyInput(KeyStroke keyStroke) {
    // TODO: Not implemented
  }

  public void end() {
    running = false;
  }

  public void setText(String text) {
    this.text = text.split("\n");
  }

  private void render(Screen screen) throws IOException {
    TextGraphics tg = screen.newTextGraphics();
    tg.fill(' ');

    if (state.isGameWaiting()) {
      diplayHello(tg);
      tg.putString(0, 6, "Players:");
      tg.putString(0, 7, "  * " + state.getSelfUsername() + " (me)");
      int offset = 8;
      for (Map.Entry<String, Player> entry : state.getPlayers().entrySet()) {
        String username = entry.getKey();
        if (username.equals(state.getSelfUsername())) {
          continue;
        }
        Player player = entry.getValue();
        tg.putString(
            0, offset, "  * " + username + (player.isReady() ? " (ready)" : " (not ready)"));
        offset++;
      }
    } else if (state.isGameRunning()) {
      for (int i = 0; i < text.length; ++i) {
        tg.putString(0, i, text[i]);
        // TODO: Not implemented: Use terminal.getTerminalSize() to split the text by
        // words to fit on the screen
      }
    }
    screen.refresh();
  }

  private void diplayHello(TextGraphics tg) {
    for (int i = 0; i < HELLO_ASCII.length; ++i) {
      tg.putString(0, i, HELLO_ASCII[i]);
    }
  }

  private void reset(Screen screen) {
    try {
      screen.stopScreen(); // Stop the screen to reset the terminal
    } catch (IOException e) {
      LOGGER.severe("Error resetting the screen: " + e.getMessage());
    }
  }
}
