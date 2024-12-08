package ch.heigvd.dai.logic.shared;

/** Player class for the client and server logic. */
public class Player {
  private volatile boolean isReady = false;
  private volatile int progress = 0;
  private volatile boolean inGame = false;

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

  public synchronized void reset() {
    isReady = false;
    progress = 0;
    inGame = false;
  }
}
