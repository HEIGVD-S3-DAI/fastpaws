package ch.heigvd.dai.logic.server;

import java.net.InetAddress;

public class ClientInfo {
  InetAddress address;
  int port;
  boolean isReady;
  int score;

  public ClientInfo(InetAddress address, int port) {
    this.address = address;
    this.port = port;
    this.isReady = false;
    this.score = 0;
  }
}
