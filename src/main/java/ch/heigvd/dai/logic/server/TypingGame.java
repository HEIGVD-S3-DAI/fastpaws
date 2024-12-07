package ch.heigvd.dai.logic.server;

import java.util.Random;

public class TypingGame {
  public static final int MIN_PLAYERS_FOR_GAME = 2;
  public static final int GAME_START_DELAY = 5; // In seconds
  private static String[] PARAGRAPHS = {
    "Cats are one of the most popular pets in the world, known for their independent nature and playful personalities. They are often considered low-maintenance companions, as they are litter-trained and groom themselves. Despite their independence, cats form strong bonds with their owners and enjoy affection in their own way.",
    "Cats have an incredible ability to adapt to various environments, whether they live indoors or outdoors. They are highly skilled hunters, using their sharp claws and keen eyesight to catch prey. Even domestic cats often display hunting behavior, stalking toys or small objects around the house.",
    "Cats communicate in many ways, from meowing and purring to tail movements and body language. A cat's purr is often associated with contentment, but it can also signal comfort when they are feeling stressed or unwell. Each cat has its own unique way of expressing its emotions.",
    "While cats are often seen as solitary creatures, many enjoy socializing with other animals and humans. They can be very affectionate, seeking attention and even cuddling with their owners. Cats have been companions to humans for thousands of years, and their mysterious charm continues to captivate people around the world.",
    "Cats are known for their agility and grace. They can jump several times their body length, and their flexible spines allow them to twist and contort with ease. Whether climbing trees or jumping to high places, cats demonstrate impressive physical capabilities."
  };

  public static String getParagraph() {
    Random random = new Random();
    int indexChosen = random.nextInt(PARAGRAPHS.length);
    return PARAGRAPHS[indexChosen].toLowerCase();
  }
}
