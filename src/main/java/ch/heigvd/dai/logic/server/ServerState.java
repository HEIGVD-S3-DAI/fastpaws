package ch.heigvd.dai.logic.server;

import ch.heigvd.dai.logic.shared.BaseState;
import java.util.HashMap;
import java.util.Map;

public class ServerState extends BaseState {
  private final Map<String, ClientInfo> connectedClients = new HashMap<>();

  public Map<String, ClientInfo> getConnectedClients() {
    return connectedClients;
  }

  public boolean usernameExists(String username) {
    return connectedClients.containsKey(username);
  }

  public void registerClient(String username, ClientInfo clientInfo) {}
}
