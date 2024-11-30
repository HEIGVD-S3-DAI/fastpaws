package ch.heigvd.dai.logic.server;

import ch.heigvd.dai.logic.shared.Message;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class ServerProtocol {

  private static final Logger LOGGER = Logger.getLogger(ServerProtocol.class.getName());
  private static final int BUFFER_SIZE = 1024;

  private final int multicastPort;
  private final DatagramSocket unicastSocket;
  private final MulticastSocket multicastSocket;
  private final InetAddress multicastGroup;

  public ServerProtocol(int port, String multicastAddress, int multicastPort) throws IOException {
    try {
      unicastSocket = new DatagramSocket(port);
      this.multicastPort = multicastPort;
      multicastSocket = new MulticastSocket();
      multicastGroup = InetAddress.getByName(multicastAddress);
    } catch (IOException e) {
      LOGGER.severe("Error creating sockets: " + e.getMessage());
      throw e;
    }
  }

  public void listenForUnicastMessages(Consumer<Message> messageHandler) {
    while (unicastSocket.isBound() && !unicastSocket.isClosed()) {
      try {
        Message message = receiveUnicast();
        messageHandler.accept(message);
      } catch (IOException e) {
        LOGGER.warning("Error receiving unicast message: " + e.getMessage());
      }
    }
  }

  private Message receiveUnicast() throws IOException {
    byte[] buffer = new byte[BUFFER_SIZE];
    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

    unicastSocket.receive(packet);
    String message =
        new String(
            packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);

    return new Message(message, packet.getAddress(), packet.getPort());
  }

  public void sendUnicast(Message message) {
    try (DatagramSocket socket = new DatagramSocket()) {
      byte[] buffer = message.str.getBytes(StandardCharsets.UTF_8);
      DatagramPacket packet =
          new DatagramPacket(buffer, buffer.length, message.address, message.port);
      socket.send(packet);
      LOGGER.info(
          "Sent message to client " + message.address + ":" + message.port + ": " + message.str);
    } catch (IOException e) {
      LOGGER.severe("Error sending message to client: " + e.getMessage());
    }
  }

  public void multicast(String message) {
    try {
      byte[] buffer = message.getBytes(StandardCharsets.UTF_8);
      DatagramPacket packet =
          new DatagramPacket(buffer, buffer.length, multicastGroup, multicastPort);
      multicastSocket.send(packet);
      LOGGER.info("Sent multicast to clients: " + message);
    } catch (IOException e) {
      LOGGER.severe("Error multicasting message to clients: " + e.getMessage());
    }
  }

  public void closeSockets() {
    if (unicastSocket != null && !unicastSocket.isClosed()) unicastSocket.close();
    if (multicastSocket != null && !multicastSocket.isClosed()) multicastSocket.close();
  }
}
