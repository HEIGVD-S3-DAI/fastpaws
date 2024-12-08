package ch.heigvd.dai.logic.server;

import ch.heigvd.dai.logic.shared.BaseState;
import java.util.concurrent.ConcurrentHashMap;

public class ServerState extends BaseState {
  private final ConcurrentHashMap<String, ClientInfo> connectedClients = new ConcurrentHashMap<>();

  public ConcurrentHashMap<String, ClientInfo> getConnectedClients() {
    return connectedClients;
  }

  public boolean usernameExists(String username) {
    return connectedClients.containsKey(username);
  }

  public void registerClient(String username, ClientInfo clientInfo) {
    connectedClients.put(username, clientInfo);
  }

  public void setUserReady(String username) {
    connectedClients.get(username).player.setReady(true);
  }

  public boolean isUserReady(String username) {
    return connectedClients.get(username).player.isReady();
  }

  public synchronized boolean areAllUsersReady() {
    for (ClientInfo clientInfo : connectedClients.values()) {
      if (!clientInfo.player.isReady()) {
        return false;
      }
    }
    return true;
  }

  public int getNumPlayers() {
    return connectedClients.size();
  }

  public void removeUser(String username) {
    connectedClients.remove(username);
  }

  public void setPlayerProgress(String username, int progress) {
    connectedClients.get(username).player.setProgress(progress);
  }

  public synchronized void resetPlayers() {
    for (ClientInfo clientInfo : connectedClients.values()) {
      clientInfo.player.reset();
    }
  }
}
