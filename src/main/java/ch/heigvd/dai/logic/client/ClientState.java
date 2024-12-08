package ch.heigvd.dai.logic.client;

import ch.heigvd.dai.logic.client.ui.event.UIEvent;
import ch.heigvd.dai.logic.client.ui.event.UIEventListener;
import ch.heigvd.dai.logic.shared.BaseState;
import ch.heigvd.dai.logic.shared.Player;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client state for the client logic.
 */
public class ClientState extends BaseState {
  private final ConcurrentHashMap<String, Player> players;
  private final String selfUsername;
  private UIEventListener uiEventListener;

  public ClientState(String selfUsername) {
    this.selfUsername = selfUsername;
    this.players = new ConcurrentHashMap<>();
    addPlayer(selfUsername);
  }

  public String getSelfUsername() {
    return selfUsername;
  }

  public ConcurrentHashMap<String, Player> getPlayers() {
    return players;
  }

  public Player getSelf() {
    assert players.containsKey(selfUsername);
    return players.get(selfUsername);
  }

  public boolean playerExists(String username) {
    return players.containsKey(username);
  }

  public void addPlayer(String username) {
    players.put(username, new Player());
  }

  public void removePlayer(String username) {
    assert players.containsKey(username);
    players.remove(username);
  }

  public synchronized void resetPlayers() {
    for (Player player : players.values()) {
      player.reset();
    }
  }

  public void setPlayerReady(String username) {
    players.get(username).setReady(true);
  }

  public synchronized boolean allPlayersReady() {
    for (Player player : players.values()) {
      if (!player.isReady()) {
        return false;
      }
    }
    return true;
  }

  public synchronized void setPlayerProgress(String username, int progress) {
    assert players.containsKey(username);
    players.get(username).setProgress(progress);
  }

  @Override
  public synchronized void setGameState(GameState gameState) {
    super.setGameState(gameState);
    fireUIEvent(new UIEvent(UIEvent.EventType.GAME_STATE_CHANGED, gameState));
  }

  public void setUIEventListener(UIEventListener listener) {
    uiEventListener = listener;
  }

  public synchronized void fireUIEvent(UIEvent event) {
    if (uiEventListener == null) {
      return;
    }
    uiEventListener.onUIEvent(event);
  }

  public void setEndGameWinner(String winner) {
    super.setGameState(GameState.FINISHED);
    fireUIEvent(new UIEvent(UIEvent.EventType.END_GAME, winner));
  }

  public synchronized boolean isPlayerInGame() {
    for (Player player : players.values()) {
      if (player.isInGame()) {
        return true;
      }
    }
    return false;
  }
}
