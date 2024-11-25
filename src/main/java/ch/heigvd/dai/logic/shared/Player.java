package ch.heigvd.dai.logic.shared;

public class Player {
  private boolean isReady = false;
  private int score = 0;

  public boolean isReady() {
    return isReady;
  }

  public void setReady(boolean ready) {
    isReady = ready;
  }

  public int getScore() {
    return score;
  }

  public void setScore(int score) {
    this.score = score;
  }

  public void reset() {
    isReady = false;
    score = 0;
  }
}
