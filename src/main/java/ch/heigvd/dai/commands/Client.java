package ch.heigvd.dai.commands;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.logging.Logger;
import picocli.CommandLine;

@CommandLine.Command(name = "client", description = "Start a client to connect to the server")
public class Client implements Callable<Integer> {

  private static final Logger LOGGER = Logger.getLogger(Client.class.getName());
  private static final int BUFFER_SIZE = 1024;
  private static final int TIMEOUT_MS = 5000;

  private final Scanner scanner = new Scanner(System.in);

  @CommandLine.ParentCommand private Root parent;

  @CommandLine.Option(
      names = {"-M", "--multicast-address"},
      description = "Multicast address to use for the server (default: ${DEFAULT-VALUE}).",
      defaultValue = "230.0.0.0")
  protected String serverMulticastAddress;

  @CommandLine.Option(
      names = {"-H", "--serverHost"},
      description = "Server host (default: ${DEFAULT-VALUE}).",
      defaultValue = "localhost")
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

  public enum Message {
    USER_JOIN,
    USER_READY,
    USERS_PROGRESS,
    USER_QUIT,
  }

  private static class Player {
    boolean isReady = false;
    int score = 0;
  }

  private static class State {
    HashMap<String, Player> players;
    String selfUsername;
    boolean isGameFinished = false;

    State(String selfUsername) {
      this.selfUsername = selfUsername;
      players = new HashMap<>();
      addPlayer(selfUsername);
    }

    void addPlayer(String username) {
      players.put(username, new Player());
    }

    void setPlayerReady(String username) throws IllegalAccessException {
      if (!players.containsKey(username)) {
        throw new IllegalAccessException("User does not exits");
      }
      players.get(username).isReady = true;
    }
  }

  private State state;

  @Override
  public Integer call() {
    int exit = 0;
    join();
    try {
      startListening();
    } catch (Exception e) {
      exit = 1;
    }
    return exit;
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
          state = new State(username);
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

  private void startListening() throws Exception {
    try (MulticastSocket multicastSocket = new MulticastSocket(serverMulticastPort)) {

      InetAddress multicastAddress = InetAddress.getByName(serverMulticastAddress);
      InetSocketAddress multicastGroup =
          new InetSocketAddress(multicastAddress, serverMulticastPort);
      NetworkInterface netInterface = NetworkInterface.getByName(networkInterface);
      multicastSocket.joinGroup(multicastGroup, netInterface);

      LOGGER.info(
          "Listening multicast on address http://"
              + serverHost
              + ":"
              + serverMulticastPort
              + "...");

      while (!multicastSocket.isClosed()) {
        byte[] buffer = new byte[BUFFER_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        multicastSocket.receive(packet);
        String message = new String(packet.getData(), packet.getOffset(), packet.getLength());
        handleMulticastMessage(message);
      }
    } catch (IOException e) {
      LOGGER.severe("Error conntecting to the server: " + e.getMessage());
      throw e;
    }
  }

  private void handleMulticastMessage(String message) throws IllegalAccessException {
    String[] parts = message.split("\\s+");

    Server.Message command = null;
    try {
      command = Server.Message.valueOf(parts[0]);
    } catch (Exception e) {
      // Do nothing
    }

    switch (command) {
      case Server.Message.NEW_USER:
        state.addPlayer(parts[1]);
        break;
      case Server.Message.USER_READY:
        state.setPlayerReady(parts[1]);
      case null:
      default:
        break;
    }
  }
}
