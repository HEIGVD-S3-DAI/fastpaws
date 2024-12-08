package ch.heigvd.dai.logic.client;

import ch.heigvd.dai.commands.Client;
import ch.heigvd.dai.logic.shared.Message;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.logging.Logger;

/** Client protocol for sending and receiving messages to the server. */
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

  /**
   * Create a new client protocol.
   *
   * @param serverHost the server host to connect to
   * @param serverPort the server port to connect to
   * @param multicastAddress the multicast address to use
   * @param multicastPort the multicast port to use
   * @param networkInterface the network interface to use
   * @throws IOException if an error occurs while creating the sockets
   */
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

  /**
   * Send a unicast message to the server.
   *
   * @param command the command to send
   * @param message the message to send
   * @throws IOException if an error occurs while sending the message
   */
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

  /**
   * Send a unicast message to the server and wait for a response.
   *
   * @param command the command to send
   * @param message the message to send
   * @return the response message
   * @throws SocketTimeoutException if the response is not received within the timeout
   * @throws IOException if an error occurs while sending the message
   */
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

  /**
   * Listen to multicast messages. This function will block until the socket is closed.
   *
   * @param messageHandler the handler to call for each message
   * @throws IOException if an error occurs while listening
   */
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

  /**
   * Close the multicast socket.
   *
   * @throws IOException if an error occurs while closing the sockets
   */
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
