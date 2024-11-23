package ch.heigvd.dai.commands;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
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
  protected String clientsMulticastAddress;

  @CommandLine.Option(
      names = {"-p", "--port"},
      description = "Port to use for the clients (default: ${DEFAULT-VALUE}).",
      defaultValue = "1732")
  protected int clientsPort;

  @CommandLine.Option(
      names = {"-I", "--network-interface"},
      description = "Network interface to use",
      required = true)
  protected String networkInterface;

  private final Map<String, ClientInfo> connectedClients = new HashMap<>();
  private boolean isRunning = true;
  private MulticastSocket socket;

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
    USER_JOIN_OK,
    USER_JOIN_ERR,
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
      socket.close();
    }
    return exit;
  }

  private void startServer() throws IOException {
    socket = new MulticastSocket(clientsPort);
    InetAddress multicastAddress = InetAddress.getByName(clientsMulticastAddress);
    InetSocketAddress multicastGroup = new InetSocketAddress(multicastAddress, clientsPort);
    NetworkInterface netInterface = NetworkInterface.getByName(networkInterface);
    socket.joinGroup(multicastGroup, netInterface);

    LOGGER.info(
        "Listening for multicast messages on address "
            + clientsMulticastAddress
            + ", "
            + "network interface "
            + networkInterface
            + " and port "
            + clientsPort
            + "...");

    // Handle messages
    while (!socket.isClosed() && isRunning) {
      byte[] buffer = new byte[BUFFER_SIZE];

      DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

      socket.receive(packet);

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
      default:
        LOGGER.warning("Unknown command, skipping...");
    }
  }

  private void handleUserJoin(String username, InetAddress address, int port) {
    if (connectedClients.containsKey(username)) {
      sendToClient(address, port, Message.USER_JOIN_ERR + " Username already taken");
    }
    connectedClients.put(username, new ClientInfo(address, port));
    sendToClient(address, port, Message.USER_JOIN_OK.toString());
  }

  private void sendToClient(InetAddress address, int port, String message) {
    try {
      byte[] buffer = message.getBytes();
      DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
      socket.send(packet);
    } catch (IOException e) {
      LOGGER.warning("Failed to send message to client: " + e.getMessage());
    }
  }
}
