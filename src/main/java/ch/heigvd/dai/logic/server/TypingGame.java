package ch.heigvd.dai.logic.server;

import java.util.Random;

/**
 * Class for the typing game.
 * This class contains the "rules" for the typing game.
 */
public class TypingGame {
  public static final int MIN_PLAYERS_FOR_GAME = 2;
  public static final int GAME_START_DELAY = 5; // In seconds
  private static final String[] PARAGRAPHS = {
    "Cats are popular pets, known for independence and playfulness. They groom themselves and bond with owners.",
    "Cats adapt well to various environments and are skilled hunters. Domestic cats also display hunting behaviors.",
    "Cats communicate through meows, purrs, and body language. Purring can show happiness or comfort when stressed.",
    "Cats may seem solitary but enjoy socializing. They often seek attention and have been human companions for centuries.",
    "Cats are agile and graceful. They jump great heights and twist easily, showcasing impressive physical skills."
  };

  public static String getParagraph() {
    Random random = new Random();
    int indexChosen = random.nextInt(PARAGRAPHS.length);
    return PARAGRAPHS[indexChosen].toLowerCase();
  }
}
