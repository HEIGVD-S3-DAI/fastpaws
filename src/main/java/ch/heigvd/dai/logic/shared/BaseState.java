package ch.heigvd.dai.logic.shared;

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

  public void setGameState(GameState gameState) {
    this.gameState = gameState;
  }
}
