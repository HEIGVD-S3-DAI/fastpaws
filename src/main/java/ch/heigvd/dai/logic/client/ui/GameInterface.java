package ch.heigvd.dai.logic.client.ui;

import ch.heigvd.dai.logic.client.ClientState;
import ch.heigvd.dai.logic.shared.Player;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class GameInterface extends Thread {

  private static final Logger LOGGER = Logger.getLogger(GameInterface.class.getName());
  private boolean running = true;
  private final ClientState state;
  private String text;
  private int cursorIndex = 0;
  private final StringBuilder userText = new StringBuilder();

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

      long lastRenderTime = 0;
      int renderIntervalMs = 50;

      // Main loop
      while (running) {
        handleKeyInput(screen.pollInput());

        // Render at fixed intervals
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastRenderTime >= renderIntervalMs) {
          render(terminal, screen);
          lastRenderTime = currentTime;
        }

        Thread.sleep(10); // Small sleep to avoid busy-waiting
      }
      // Reset terminal to initial state
      reset(screen);

    } catch (IOException | InterruptedException e) {
      LOGGER.severe("Could not initialize or run the game: " + e.getMessage());
    }
  }

  private void handleKeyInput(KeyStroke keyStroke) {
    if (keyStroke == null) {
      return;
    }

    if (state.isGameRunning()) {
      if (keyStroke.getKeyType() == KeyType.Backspace && cursorIndex > 0) {
        cursorIndex--;
        userText.deleteCharAt(cursorIndex);
        return;
      }
      Character character = keyStroke.getCharacter();
      if (character != null && cursorIndex < text.length()) {
        userText.insert(cursorIndex, character);
        cursorIndex++;
      }
    }
  }

  public void end() {
    running = false;
  }

  public void setText(String text) {
    this.text = text;
  }

  private void render(Terminal terminal, Screen screen) throws IOException {
    TextGraphics tg = screen.newTextGraphics();
    tg.fill(' ');

    if (state.isGameWaiting()) {
      displayHello(tg);
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
      String[] lines = splitText(terminal);
      int currCharIndex = 0;
      for (int i = 0; i < lines.length; ++i) {
        for (int j = 0; j < lines[i].length(); ++j) {
          if (currCharIndex < cursorIndex) {
            tg.setBackgroundColor(TextColor.ANSI.DEFAULT);
            if (userText.charAt(currCharIndex) != lines[i].charAt(j)) {
              tg.setForegroundColor(TextColor.ANSI.RED_BRIGHT);
            } else {
              tg.setForegroundColor(TextColor.ANSI.WHITE_BRIGHT);
            }
          } else if (currCharIndex == cursorIndex) {
            tg.setBackgroundColor(TextColor.ANSI.WHITE_BRIGHT);
            tg.setForegroundColor(TextColor.ANSI.BLACK);
          } else {
            tg.setBackgroundColor(TextColor.ANSI.DEFAULT);
            tg.setForegroundColor(TextColor.ANSI.BLACK_BRIGHT);
          }
          tg.putString(j, i, String.valueOf(lines[i].charAt(j)));
          currCharIndex++;
        }
      }
    }
    screen.refresh();
  }

  private void displayHello(TextGraphics tg) {
    for (int i = 0; i < HELLO_ASCII.length; ++i) {
      tg.putString(0, i, HELLO_ASCII[i]);
    }
  }

  private String[] splitText(Terminal terminal) throws IOException {
    int maxWidth = terminal.getTerminalSize().getColumns();
    String[] words = text.split(" ");
    List<String> lines = new ArrayList<>();

    StringBuilder currentLine = new StringBuilder();

    for (String word : words) {
      if (currentLine.length() + word.length() + 1 > maxWidth) {
        currentLine.append(" ");
        lines.add(currentLine.toString());
        currentLine = new StringBuilder(word); // Start new line with current word
      } else {
        // Add word to the current line
        if (currentLine.length() > 0) {
          currentLine.append(" ");
        }
        currentLine.append(word);
      }
    }

    // Add the last line if there's any remaining text
    if (currentLine.length() > 0) {
      lines.add(currentLine.toString());
    }

    return lines.toArray(new String[0]);
  }

  private void reset(Screen screen) {
    try {
      screen.stopScreen(); // Stop the screen to reset the terminal
    } catch (IOException e) {
      LOGGER.severe("Error resetting the screen: " + e.getMessage());
    }
  }
}
