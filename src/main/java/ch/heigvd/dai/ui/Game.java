package ch.heigvd.dai.ui;

import com.googlecode.lanterna.screen.*;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import java.io.IOException;
import java.util.logging.Logger;

public class Game {

  private static final Logger LOGGER = Logger.getLogger(Game.class.getName());

  public void run() {
    // Initialize terminal and screen using try-with-resources
    try (Screen screen = new DefaultTerminalFactory().createScreen()) {
      screen.startScreen();
      screen.setCursorPosition(null);

    } catch (IOException e) {
      LOGGER.severe("Could not initialize game");
    }
  }
}
