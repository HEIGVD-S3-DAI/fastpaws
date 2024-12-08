package ch.heigvd.dai.logic.shared;

public class Player {
  private boolean isReady = false;
  private int progress = 0;
  private boolean inGame = false;

  public boolean isReady() {
    return isReady;
  }

  public void setReady(boolean ready) {
    isReady = ready;
  }

  public int getProgress() {
    return progress;
  }

  public void setProgress(int progress) {
    this.progress = progress;
  }

  public boolean isInGame() {
    return inGame;
  }

  public void setInGame(boolean inGame) {
    this.inGame = inGame;
  }

  public void reset() {
    isReady = false;
    progress = 0;
    inGame = false;
  }
}
