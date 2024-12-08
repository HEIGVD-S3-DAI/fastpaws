package ch.heigvd.dai.logic.client.ui.display;

import ch.heigvd.dai.commands.Client;
import ch.heigvd.dai.logic.client.ui.TerminalUI;
import ch.heigvd.dai.logic.shared.Player;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.terminal.Terminal;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RaceDisplayState extends DisplayState {
  private String text;
  private int cursorIndex = 0;
  private final StringBuilder userText = new StringBuilder();

  public RaceDisplayState(TerminalUI ui) {
    super(ui);
  }

  public void render(TextGraphics tg) throws IOException {
    int offset = renderPlayersProgress(tg);
    renderTypingText(tg, offset);
  }

  private int renderPlayersProgress(TextGraphics tg) throws IOException {
    int offset = 0;

    String selfUsername = ui.getClientState().getSelfUsername();
    Player self = ui.getClientState().getPlayers().get(selfUsername);

    renderPlayerProgress(tg, selfUsername + " (you)", self.getProgress(), offset);
    offset++;

    for (Map.Entry<String, Player> entry : ui.getClientState().getPlayers().entrySet()) {
      if (!entry.getValue().isInGame()) continue;
      String username = entry.getKey();
      if (username.equals(selfUsername)) {
        continue;
      }
      renderPlayerProgress(tg, username, entry.getValue().getProgress(), offset);
      offset++;
    }
    return offset + 1; // Add extra line spacing
  }

  private void renderPlayerProgress(TextGraphics tg, String username, int progress, int offset)
      throws IOException {
    int maxWidth = ui.getTerminal().getTerminalSize().getColumns();
    int fullLen = maxWidth - username.length() - 1;
    int progLen = (int) Math.min(Math.round((double) progress * fullLen / 100), 100);

    String sb = "#".repeat(progLen) + ".".repeat(fullLen - progLen) + " " + username;

    tg.putString(0, offset, sb);
  }

  private void renderTypingText(TextGraphics tg, int offset) throws IOException {
    String[] lines = splitText(ui.getTerminal());
    int currCharIndex = 0;

    for (int i = 0; i < lines.length; ++i) {
      for (int j = 0; j < lines[i].length(); ++j) {
        if (currCharIndex < cursorIndex) {
          tg.setBackgroundColor(TextColor.ANSI.DEFAULT);
          if (userText.charAt(currCharIndex) != lines[i].charAt(j)) {
            if (lines[i].charAt(j) == ' ') {
              tg.setBackgroundColor(TextColor.ANSI.RED_BRIGHT);
            } else {
              tg.setForegroundColor(TextColor.ANSI.RED_BRIGHT);
            }
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
        tg.putString(j, i + offset, String.valueOf(lines[i].charAt(j)));
        currCharIndex++;
      }
    }
  }

  @Override
  public void handleInput(KeyStroke keyStroke) throws IOException {
    super.handleInput(keyStroke);

    // If the user presses backspace, delete the character before the cursor
    if (keyStroke.getKeyType() == KeyType.Backspace && cursorIndex > 0) {
      cursorIndex--;
      userText.deleteCharAt(cursorIndex);
      return;
    }

    // If the user presses a character, insert it at the cursor and update the progress
    Character character = keyStroke.getCharacter();
    if (character != null && cursorIndex < text.length()) {
      userText.insert(cursorIndex, character);
      cursorIndex++;

      updateProgress();
    }
  }

  private void updateProgress() throws IOException {
    // Count correct characters
    int correctChars = 0;
    for (int i = 0; i < cursorIndex; i++) {
      if (userText.charAt(i) == text.charAt(i)) {
        correctChars++;
      }
    }

    int progress = (int) Math.min(Math.round((double) correctChars / text.length() * 100), 99);
    // Make sure to only be at 100% when all the letters are completed correctly
    if (cursorIndex == text.length() && correctChars == text.length()) {
      progress = 100;
    }

    ui.getNetwork()
        .sendUnicast(
            Client.Command.USER_PROGRESS, ui.getClientState().getSelfUsername() + " " + progress);
  }

  /**
   * Split the text into lines of a maximum width.
   * @param terminal the terminal to use for splitting
   * @return the lines of the text
   * @throws IOException if an error occurs while splitting
   */
  private String[] splitText(Terminal terminal) throws IOException {
    int maxWidth = terminal.getTerminalSize().getColumns();
    String[] words = text.split(" ");
    List<String> lines = new ArrayList<>();
    StringBuilder currentLine = new StringBuilder();

    for (String word : words) {
      if (currentLine.length() + word.length() + 1 > maxWidth) {
        currentLine.append(" ");
        lines.add(currentLine.toString());
        currentLine = new StringBuilder(word);
      } else {
        if (!currentLine.isEmpty()) {
          currentLine.append(" ");
        }
        currentLine.append(word);
      }
    }

    if (!currentLine.isEmpty()) {
      lines.add(currentLine.toString());
    }

    return lines.toArray(new String[0]);
  }

  public void setText(String text) {
    this.text = text;
    this.cursorIndex = 0;
    this.userText.setLength(0);
  }
}
