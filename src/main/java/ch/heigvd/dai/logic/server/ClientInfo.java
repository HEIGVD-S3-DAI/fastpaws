package ch.heigvd.dai.logic.server;

import ch.heigvd.dai.logic.shared.Player;
import java.net.InetAddress;

public class ClientInfo {
  public InetAddress address;
  public int port;
  public Player player;

  public ClientInfo(InetAddress address, int port) {
    this.address = address;
    this.port = port;
    this.player = new Player();
  }
}
