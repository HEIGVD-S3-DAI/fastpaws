package ch.heigvd.dai.logic.client.ui.display;

import ch.heigvd.dai.logic.client.ui.TerminalUI;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import java.io.IOException;

public abstract class DisplayState {
  protected final TerminalUI ui;

  public DisplayState(TerminalUI ui) {
    this.ui = ui;
  }

  public abstract void render(TextGraphics tg) throws IOException;

  public void handleInput(KeyStroke keyStroke) throws IOException {
    if ((keyStroke.getKeyType() == KeyType.Character && keyStroke.getCharacter() == 'q')
        || keyStroke.getKeyType() == KeyType.Escape) {
      ui.end();
      ui.getNetwork().closeMulticast();
    }
  }
}
