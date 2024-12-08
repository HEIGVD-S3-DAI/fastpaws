package ch.heigvd.dai.logic.shared;

/**
 * Base state for the client and server logic.
 */
public abstract class BaseState {
  private GameState gameState = GameState.WAITING;

  public enum GameState {
    WAITING,
    RUNNING,
    FINISHED,
  }

  public boolean isGameWaiting() {
    return gameState == GameState.WAITING;
  }

  public boolean isGameRunning() {
    return gameState == GameState.RUNNING;
  }

  public boolean isGameFinished() {
    return gameState == GameState.FINISHED;
  }

  public synchronized void setGameState(GameState gameState) {
    this.gameState = gameState;
  }
}
