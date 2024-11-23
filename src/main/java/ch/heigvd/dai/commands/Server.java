package ch.heigvd.dai.commands;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Logger;
import picocli.CommandLine;

@CommandLine.Command(
    name = "server",
    description = "Start a server to connect to the server",
    mixinStandardHelpOptions = true)
public class Server implements Callable<Integer> {

  private static final Logger LOGGER = Logger.getLogger(Server.class.getName());
  private static final int BUFFER_SIZE = 1024;

  @CommandLine.ParentCommand private Root parent;

  @CommandLine.Option(
      names = {"-M", "--multicast-address"},
      description = "Multicast address to use for the clients (default: ${DEFAULT-VALUE}).",
      defaultValue = "230.0.0.0")
  protected String multicastAddress;

  @CommandLine.Option(names = {"-H", "--host"}, description = "Server host (default: ${DEFAULT-VALUE}).", defaultValue = "localhost")
  protected String host;

  @CommandLine.Option(
      names = {"-p", "--port"},
      description = "Port to use for the clients (default: ${DEFAULT-VALUE}).",
      defaultValue = "4445")
  protected int port;

  @CommandLine.Option(
      names = {"-pm", "--multicastPort"},
      description = "Port to use for the server (default: ${DEFAULT-VALUE}).",
      defaultValue = "4446")
  protected int multicastPort;

  private final Map<String, ClientInfo> connectedClients = new HashMap<>();
  private boolean isRunning = true;
  private DatagramSocket unicastSocket;
  private MulticastSocket multicastSocket;
  private InetAddress multicastGroup;

  private static class ClientInfo {
    InetAddress address;
    int port;
    boolean isReady;
    int score;

    ClientInfo(InetAddress address, int port) {
      this.address = address;
      this.port = port;
      this.isReady = false;
      this.score = 0;
    }
  }

  public enum Message {
    OK,
    USER_JOIN_ERR,
    NEW_USER,
    START_GAME,
    END_GAME,
  }

  @Override
  public Integer call() {
    int exit = 0;
    try {
      startServer();
    } catch (IOException e) {
      LOGGER.severe("Error starting the server: " + e.getMessage());
      exit = 1;
    } finally {
      if (unicastSocket != null && !unicastSocket.isClosed()) unicastSocket.close();
      if (multicastSocket != null && !multicastSocket.isClosed()) multicastSocket.close();
    }
    return exit;
  }

  private void startServer() throws IOException {
    unicastSocket = new DatagramSocket(port);
    multicastSocket = new MulticastSocket();
    multicastGroup = InetAddress.getByName(multicastAddress);

    LOGGER.info("Listening on address http://" + host + ":" + port + "...");

    // Handle messages
    while (!unicastSocket.isClosed() && isRunning) {
      byte[] buffer = new byte[BUFFER_SIZE];

      DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

      unicastSocket.receive(packet);

      String message =
          new String(
              packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
      handleMessage(message, packet.getAddress(), packet.getPort());
    }
  }

  private void handleMessage(String message, InetAddress address, int port) {
    String[] parts = message.split("\\s+");

    if (parts.length < 2) {
      // TODO: Handle error
      return;
    }
    Client.Message command = null;
    try {
      command = Client.Message.valueOf(parts[0]);
    } catch (Exception e) {
      // Do nothing
    }

    switch (command) {
      case Client.Message.USER_JOIN:
        handleUserJoin(parts[1], address, port);
        break;
      case null:
      default:
        LOGGER.warning("Unknown command, skipping...");
    }
  }

  private void handleUserJoin(String username, InetAddress address, int port) {
    if (connectedClients.containsKey(username)) {
      sendToClient(address, port, Message.USER_JOIN_ERR + " Username already taken");
      return;
    }
    connectedClients.put(username, new ClientInfo(address, port));
    sendToClient(address, port, Message.OK.toString());
    broadcast(Message.NEW_USER + " " + username);
  }

  private void sendToClient(InetAddress address, int port, String message) {
    try (DatagramSocket socket = new DatagramSocket()) {
      byte[] buffer = message.getBytes(StandardCharsets.UTF_8);
      DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
      socket.send(packet);
      LOGGER.info("Sent message to client " + address + ":" + port + ": " + message);
    } catch (IOException e) {
      LOGGER.severe("Error sending message to client: " + e.getMessage());
    }
  }

  private void broadcast(String message) {
    try {
      byte[] buffer = message.getBytes(StandardCharsets.UTF_8);
      DatagramPacket packet =
          new DatagramPacket(buffer, buffer.length, multicastGroup, multicastPort);
      multicastSocket.send(packet);
    } catch (IOException e) {
      LOGGER.severe("Error broadcasting message to clients: " + e.getMessage());
    }
  }
}
