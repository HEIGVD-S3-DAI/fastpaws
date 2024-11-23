package ch.heigvd.dai.logic.shared;

import java.net.InetAddress;

public class Message {
  public final String str;
  public final InetAddress address;
  public final int port;

  public Message(String str, InetAddress address, int port) {
    this.str = str;
    this.address = address;
    this.port = port;
  }

  public String[] getParts() {
    return str.split("\\s+");
  }
}
