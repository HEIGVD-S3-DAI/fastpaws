package ch.heigvd.dai.ui;

import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.*;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Game {
  public void run() {
    // Initialize terminal and screen using try-with-resources
    try (Screen screen = new DefaultTerminalFactory().createScreen()) {
      screen.startScreen();
      screen.setCursorPosition(null);

      List<String> words = generateRandomWords(10);
      String sentence = String.join(" ", words);

      displaySentence(screen, sentence);
      runGameLoop(screen, sentence);

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void runGameLoop(Screen screen, String sentence) throws IOException {
    int charIndex = 0;
    boolean running = true;

    // Draw custom cursor under the current character
    drawCustomCursor(screen, sentence.charAt(charIndex), charIndex);
    screen.refresh();

    while (running) {
      KeyStroke keyStroke = screen.readInput();
      if (keyStroke.getKeyType() == KeyType.Character) {
        char inputChar = keyStroke.getCharacter();
        if (inputChar == sentence.charAt(charIndex)) {
          updateCharacterColor(
              screen, sentence.charAt(charIndex), charIndex, TextColor.ANSI.WHITE_BRIGHT);
          charIndex++;
          if (charIndex >= sentence.length()) {
            running = false;
          } else {
            drawCustomCursor(screen, sentence.charAt(charIndex), charIndex);
          }
          screen.refresh();
        }
      } else if (keyStroke.getKeyType() == KeyType.Escape) {
        running = false;
      }
    }
  }

  private List<String> generateRandomWords(int count) {
    // Generate random words of length 3 to 5
    List<String> words = new ArrayList<>();
    Random random = new Random();
    String alphabet = "abcdefghijklmnopqrstuvwxyz";

    for (int i = 0; i < count; i++) {
      int length = 3 + random.nextInt(3); // Length between 3 and 5
      StringBuilder word = new StringBuilder();
      for (int j = 0; j < length; j++) {
        word.append(alphabet.charAt(random.nextInt(alphabet.length())));
      }
      words.add(word.toString());
    }

    return words;
  }

  private void displaySentence(Screen screen, String sentence) {
    // Display the sentence with gray text
    for (int i = 0; i < sentence.length(); i++) {
      screen.setCharacter(
          i,
          0,
          TextCharacter.fromCharacter(
              sentence.charAt(i), TextColor.Indexed.fromRGB(175, 175, 175), TextColor.ANSI.DEFAULT)[
              0]);
    }
  }

  private void updateCharacterColor(Screen screen, char character, int index, TextColor color) {
    screen.setCharacter(
        index, 0, TextCharacter.fromCharacter(character, color, TextColor.ANSI.DEFAULT)[0]);
  }

  private void drawCustomCursor(Screen screen, char character, int charIndex) {
    // Draw a white block under the current character
    screen.setCharacter(
        charIndex,
        0,
        TextCharacter.fromCharacter(character, TextColor.ANSI.BLACK, TextColor.ANSI.WHITE)[0]);
  }
}
