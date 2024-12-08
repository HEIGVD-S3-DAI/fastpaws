package ch.heigvd.dai.logic.client.ui.display;

import ch.heigvd.dai.logic.client.ui.TerminalUI;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import java.io.IOException;

/** Base class for display states. */
public abstract class DisplayState {
  protected final TerminalUI ui;

  public DisplayState(TerminalUI ui) {
    this.ui = ui;
  }

  /**
   * Render the display state.
   *
   * @param tg the text graphics to render to
   * @throws IOException if an error occurs while rendering
   */
  public abstract void render(TextGraphics tg) throws IOException;

  /**
   * Handle input from the user.
   *
   * @param keyStroke the key stroke to handle
   * @throws IOException if an error occurs while handling the input
   */
  public void handleInput(KeyStroke keyStroke) throws IOException {
    if ((!ui.getClientState().isGameRunning()
            && keyStroke.getKeyType() == KeyType.Character
            && keyStroke.getCharacter() == 'q')
        || keyStroke.getKeyType() == KeyType.Escape) {
      ui.end();
      ui.getNetwork().closeMulticast();
    }
  }
}
