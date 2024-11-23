package ch.heigvd.dai.commands;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.logging.Logger;
import picocli.CommandLine;

@CommandLine.Command(name = "client", description = "Start a client to connect to the server")
public class Client implements Callable<Integer> {

  private static final Logger LOGGER = Logger.getLogger(Client.class.getName());
  private static final int BUFFER_SIZE = 1024;
  private static final int TIMEOUT_MS = 5000;

  private Scanner scanner = new Scanner(System.in);

  @CommandLine.ParentCommand private Root parent;

  @CommandLine.Option(
      names = {"-M", "--multicast-address"},
      description = "Multicast address to use for the server (default: ${DEFAULT-VALUE}).",
      defaultValue = "230.0.0.0")
  protected String serverMulticastAddress;

  @CommandLine.Option(names = {"-H", "--serverHost"}, description = "Server host (default: ${DEFAULT-VALUE}).", defaultValue = "localhost")
  protected String serverHost;

  @CommandLine.Option(
      names = {"-p", "--serverPort"},
      description = "Port to use for the server (default: ${DEFAULT-VALUE}).",
      defaultValue = "4445")
  protected int serverPort;

  @CommandLine.Option(
      names = {"-pm", "--serverMulticastPort"},
      description = "Port to use for the server multicast (default: ${DEFAULT-VALUE}).",
      defaultValue = "4446")
  protected int serverMulticastPort;

  @CommandLine.Option(
      names = {"-I", "--network-interface"},
      description = "Network interface to use",
      required = true)
  protected String networkInterface;

  MulticastSocket multicastSocket;

  public enum Message {
    USER_JOIN,
    USER_READY,
    USERS_PROGRESS,
    USER_QUIT,
  }

  @Override
  public Integer call() {
    join();
    try {
      startListening();
    } catch (IOException e) {
      LOGGER.severe("Error conntecting to the server: " + e.getMessage());
    } finally {
      if (multicastSocket != null && !multicastSocket.isClosed()) {
        multicastSocket.close();
      }
    }
    return 0;
  }

  private void join() {
    boolean success = false;
    do {
      System.out.print("Enter your username: ");
      String username = scanner.nextLine();

      String res = sendToServer(Message.USER_JOIN, username);
      String[] parts = res.split("\\s+");

      Server.Message command = null;
      try {
        command = Server.Message.valueOf(parts[0]);
      } catch (Exception e) {
        // Do nothing
      }

      switch (command) {
        case Server.Message.OK:
          LOGGER.info("Successfully joined the server!");
          success = true;
          break;
        case Server.Message.USER_JOIN_ERR:
          String err = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
          LOGGER.info("Could not join server: " + err);
          break;
        case null:
        default:
          LOGGER.severe("Unknown message");
      }
    } while (!success);
  }

  private String sendToServer(Message command, String message) {
    try (DatagramSocket socket = new DatagramSocket()) {
      InetAddress serverAddress = InetAddress.getByName(serverHost);

      String messageToSend = command + " " + message;
      byte[] buffer = messageToSend.getBytes(StandardCharsets.UTF_8);

      DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverAddress, serverPort);

      socket.send(packet);
      // Receive
      socket.setSoTimeout(TIMEOUT_MS);
      buffer = new byte[BUFFER_SIZE];
      packet = new DatagramPacket(buffer, buffer.length);
      socket.receive(packet);
      return new String(
          packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
    } catch (SocketTimeoutException e) {
      LOGGER.severe("Timeout waiting for server response");
    } catch (IOException e) {
      LOGGER.severe("Error receiving server response: " + e.getMessage());
    }
    return "";
  }

  private void startListening() throws IOException {
    multicastSocket = new MulticastSocket(serverMulticastPort);
    InetAddress multicastAddress = InetAddress.getByName(serverMulticastAddress);
    InetSocketAddress multicastGroup = new InetSocketAddress(multicastAddress, serverMulticastPort);
    NetworkInterface netInterface = NetworkInterface.getByName(networkInterface);
    multicastSocket.joinGroup(multicastGroup, netInterface);

    LOGGER.info("Listening multicast on address http://" + serverHost + ":" + serverMulticastPort + "...");

    while (true) {
      byte[] buffer = new byte[BUFFER_SIZE];
      DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
      multicastSocket.receive(packet);
      String message = new String(packet.getData(), packet.getOffset(), packet.getLength());
      System.out.println("Received multicast: " + message);
    }
  }
}
