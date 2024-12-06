package ch.heigvd.dai.logic.client;

import ch.heigvd.dai.logic.shared.BaseState;
import ch.heigvd.dai.logic.shared.Player;
import java.util.HashMap;

public class ClientState extends BaseState {
  HashMap<String, Player> players;
  String selfUsername;

  public ClientState(String selfUsername) {
    this.selfUsername = selfUsername;
    players = new HashMap<>();
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
}
