package ch.heigvd.dai.logic.server;

import ch.heigvd.dai.logic.shared.Player;
import java.net.InetAddress;

/**
 * Client information for the server logic.
 */
public class ClientInfo {
  public final InetAddress address;
  public final int port;
  public final Player player;

  public ClientInfo(InetAddress address, int port) {
    this.address = address;
    this.port = port;
    this.player = new Player();
  }
}
