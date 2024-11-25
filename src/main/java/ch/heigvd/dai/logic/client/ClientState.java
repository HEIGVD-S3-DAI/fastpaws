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

  public void addPlayer(String username) {
    players.put(username, new Player());
  }

  public void setPlayerReady(String username) {
    if (!players.containsKey(username)) {
      throw new RuntimeException("User does not exist");
    }
    players.get(username).setReady(true);
  }
}
