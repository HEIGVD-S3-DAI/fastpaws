package ch.heigvd.dai.logic.server;

import java.util.Random;

public class TypingGame {
  public static final int MIN_PLAYERS_FOR_GAME = 2;
  private static String[] PARAGRAPHS = {"Text 1 ", "Text 2", "Text 3", "Text 4", "Text 5"};

  public static String getParagraph() {
    Random random = new Random();
    int indexChosen = random.nextInt(PARAGRAPHS.length);
    return PARAGRAPHS[indexChosen];
  }
}
