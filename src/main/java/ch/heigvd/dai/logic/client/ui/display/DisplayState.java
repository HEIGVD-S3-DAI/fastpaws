package ch.heigvd.dai.logic.client.ui.display;

import ch.heigvd.dai.logic.client.ui.TerminalRenderer;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import java.io.IOException;

public abstract class DisplayState {
  protected final TerminalRenderer renderer;

  public DisplayState(TerminalRenderer renderer) {
    this.renderer = renderer;
  }

  public abstract void render(TextGraphics tg) throws IOException;

  public void handleInput(KeyStroke keyStroke) throws IOException {
    if ((keyStroke.getKeyType() == KeyType.Character && keyStroke.getCharacter() == 'q')
        || keyStroke.getKeyType() == KeyType.Escape) {
      renderer.end();
      renderer.getProtocol().closeMulticast();
    }
  }
}
