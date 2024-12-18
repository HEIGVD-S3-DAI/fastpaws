package ch.heigvd.dai.logic.client.ui.display;

import ch.heigvd.dai.logic.client.ui.TerminalUI;
import ch.heigvd.dai.logic.shared.BaseState;
import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import java.io.IOException;

public class GameOverDisplayState extends DisplayState {
  private static final String[] GAME_OVER_ASCII = {
    "   _____               ____              ",
    "  / ___/__ ___ _  ___ / __ \\_  _____ ____",
    " / (_ / _ `/  ' \\/ -_) /_/ / |/ / -_) __/",
    " \\___/\\_,_/_/_/_/\\__/\\____/|___/\\__/_/",
  };

  private final String winner;

  public GameOverDisplayState(TerminalUI ui, String winner) {
    super(ui);
    this.winner = winner;
  }

  public void render(TextGraphics tg) {
    // Display GAME OVER ASCII art
    tg.setForegroundColor(TextColor.ANSI.RED_BRIGHT);
    tg.enableModifiers(SGR.BOLD);
    for (int i = 0; i < GAME_OVER_ASCII.length; ++i) {
      tg.putString(0, i, GAME_OVER_ASCII[i]);
    }
    tg.disableModifiers(SGR.BOLD);
    tg.setForegroundColor(TextColor.ANSI.DEFAULT);

    // Display winner message
    String selfUsername = ui.getClientState().getSelfUsername();
    String message =
        winner.equals(selfUsername)
            ? "Congratulations! You won the game!"
            : "Game Over! The winner is: " + winner;

    tg.putString(0, GAME_OVER_ASCII.length + 2, message);
    tg.putString(0, GAME_OVER_ASCII.length + 4, "Press ENTER to continue...");
  }

  @Override
  public void handleInput(KeyStroke keyStroke) throws IOException {
    super.handleInput(keyStroke);
    // If the user presses enter, reset the game state
    if (keyStroke.getKeyType() == KeyType.Enter) {
      ui.getClientState().setGameState(BaseState.GameState.WAITING);
    }
  }
}
