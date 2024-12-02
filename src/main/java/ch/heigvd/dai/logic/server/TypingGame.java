package ch.heigvd.dai.logic.server;

import java.util.Random;

public class TypingGame {
  public static final int MIN_PLAYERS_FOR_GAME = 2;
  private static final int NUM_WORDS = 20;

  public static String getParagraph() {
    Random random = new Random();
    StringBuilder paragraph = new StringBuilder();

    for (int i = 0; i < TypingGame.NUM_WORDS; i++) {
      int wordLength = 2 + random.nextInt(5); // Random word length between 2 and 6
      StringBuilder word = new StringBuilder();

      for (int j = 0; j < wordLength; j++) {
        char letter = (char) ('a' + random.nextInt(26)); // Generate a random letter
        word.append(letter);
      }

      paragraph.append(word).append(" ");
    }

    return paragraph.toString().trim();
  }
}
