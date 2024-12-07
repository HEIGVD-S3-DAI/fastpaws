package ch.heigvd.dai.logic.client;

import ch.heigvd.dai.logic.client.ui.event.UIEvent;
import ch.heigvd.dai.logic.client.ui.event.UIEventListener;
import ch.heigvd.dai.logic.shared.BaseState;
import ch.heigvd.dai.logic.shared.Player;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ClientState extends BaseState {
  private HashMap<String, Player> players;
  private String selfUsername;
  private final List<UIEventListener> uiEventListeners = new ArrayList<>();

  public ClientState(String selfUsername) {
    this.selfUsername = selfUsername;
    this.players = new HashMap<>();
    addPlayer(selfUsername);
  }

  public String getSelfUsername() {
    return selfUsername;
  }

  public HashMap<String, Player> getPlayers() {
    return players;
  }

  public boolean getSelfIsReady() {
    return players.get(selfUsername).isReady();
  }

  public boolean playerExists(String username) {
    return players.containsKey(username);
  }

  public void addPlayer(String username) {
    players.put(username, new Player());
  }

  public void removePlayer(String username) {
    players.remove(username);
  }

  public void resetPlayers() {
    for (Player player : players.values()) {
      player.reset();
    }
  }

  public void setPlayerReady(String username) {
    if (!players.containsKey(username)) {
      throw new RuntimeException("User does not exist");
    }
    players.get(username).setReady(true);
  }

  public void setPlayerProgress(String username, int progress) {
    players.get(username).setProgress(progress);
  }

  @Override
  public void setGameState(GameState gameState) {
    super.setGameState(gameState);
    fireUIEvent(new UIEvent(UIEvent.EventType.GAME_STATE_CHANGED, gameState));
  }

  public void addUIEventListener(UIEventListener listener) {
    uiEventListeners.add(listener);
  }

  public void removeUIEventListener(UIEventListener listener) {
    uiEventListeners.remove(listener);
  }

  public void fireUIEvent(UIEvent event) {
    for (UIEventListener listener : uiEventListeners) {
      listener.onUIEvent(event);
    }
  }
}
