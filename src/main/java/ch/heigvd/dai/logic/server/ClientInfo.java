package ch.heigvd.dai.logic.server;

import ch.heigvd.dai.logic.shared.Player;
import java.net.InetAddress;

public class ClientInfo {
  InetAddress address;
  int port;
  Player player;

  public ClientInfo(InetAddress address, int port) {
    this.address = address;
    this.port = port;
    this.player = new Player();
  }
}
