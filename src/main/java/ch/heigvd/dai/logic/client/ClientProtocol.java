package ch.heigvd.dai.logic.client;

import ch.heigvd.dai.commands.Client;
import ch.heigvd.dai.logic.shared.Message;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class ClientProtocol {

  private static final Logger LOGGER = Logger.getLogger(ClientProtocol.class.getName());

  private static final int BUFFER_SIZE = 1024;
  private static final int TIMEOUT_MS = 5000;

  private final InetAddress serverAddress;
  private final int serverPort;
  private final InetAddress multicastAddress;
  private final int multicastPort;
  private MulticastSocket multicastSocket;
  private final NetworkInterface networkInterface;

  public ClientProtocol(
      String serverHost,
      int serverPort,
      String multicastAddress,
      int multicastPort,
      String networkInterface)
      throws IOException {
    this.serverAddress = InetAddress.getByName(serverHost);
    this.serverPort = serverPort;
    this.multicastAddress = InetAddress.getByName(multicastAddress);
    this.multicastPort = multicastPort;
    this.networkInterface = NetworkInterface.getByName(networkInterface);
  }

  public void sendUnicast(Client.Command command, String message) throws IOException {
    try (DatagramSocket socket = new DatagramSocket()) {
      String msg = command.toString() + " " + message;
      byte[] buffer = msg.getBytes(StandardCharsets.UTF_8);

      DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverAddress, serverPort);
      socket.send(packet);
    } catch (IOException e) {
      LOGGER.severe("Error sending message" + e.getMessage());
      throw e;
    }
  }

  public Message sendWithResponseUnicast(Client.Command command, String message)
      throws IOException {
    try (DatagramSocket socket = new DatagramSocket()) {
      String msg = command.toString() + " " + message;
      byte[] buffer = msg.getBytes(StandardCharsets.UTF_8);

      DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverAddress, serverPort);
      socket.send(packet);

      // Receive
      socket.setSoTimeout(TIMEOUT_MS);
      buffer = new byte[BUFFER_SIZE];
      packet = new DatagramPacket(buffer, buffer.length);
      socket.receive(packet);
      String res =
          new String(
              packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
      return new Message(res, packet.getAddress(), packet.getPort());
    } catch (SocketTimeoutException e) {
      LOGGER.severe("Timeout waiting for server response");
      throw e;
    } catch (IOException e) {
      LOGGER.severe("Error receiving server response: " + e.getMessage());
      throw e;
    }
  }

  public void listenToMulticast(Consumer<String> messageHandler) throws IOException {
    this.multicastSocket = new MulticastSocket(multicastPort);
    try {
      multicastSocket.joinGroup(
          new InetSocketAddress(multicastAddress, multicastPort), networkInterface);

      LOGGER.info("Listening to multicast messages on " + multicastAddress + ":" + multicastPort);

      while (multicastSocket.isBound() && !multicastSocket.isClosed()) {
        byte[] buffer = new byte[BUFFER_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        multicastSocket.receive(packet);

        String message =
            new String(
                packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
        messageHandler.accept(message);
      }
    } catch (IOException e) {
      LOGGER.severe("Error during multicast listening: " + e.getMessage());
      throw e;
    } finally {
      if (multicastSocket != null && !multicastSocket.isClosed()) {
        multicastSocket.close();
      }
    }
  }

  public void closeMulticast() {
    try {
      multicastSocket.leaveGroup(
          new InetSocketAddress(multicastAddress, multicastPort), networkInterface);
      multicastSocket.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
