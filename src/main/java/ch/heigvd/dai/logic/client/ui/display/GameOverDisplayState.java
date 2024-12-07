package ch.heigvd.dai.logic.client.ui.display;

import ch.heigvd.dai.logic.client.ui.TerminalRenderer;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import java.io.IOException;

public class GameOverDisplayState extends DisplayState {

  public GameOverDisplayState(TerminalRenderer renderer) {
    super(renderer);
  }

  public void render(TextGraphics tg) throws IOException {}

  @Override
  public void handleInput(KeyStroke keyStroke) throws IOException {
    super.handleInput(keyStroke);
  }
}
