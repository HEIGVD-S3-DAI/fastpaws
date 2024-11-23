package ch.heigvd.dai.commands;

import ch.heigvd.dai.logic.server.ClientInfo;
import ch.heigvd.dai.logic.server.ServerProtocol;
import ch.heigvd.dai.logic.server.ServerState;
import ch.heigvd.dai.logic.shared.Message;
import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.Callable;
import java.util.logging.Logger;
import picocli.CommandLine;

@CommandLine.Command(
    name = "server",
    description = "Start a server to connect to the server",
    mixinStandardHelpOptions = true)
public class Server implements Callable<Integer> {
  @CommandLine.ParentCommand private Root parent;

  @CommandLine.Option(
      names = {"-M", "--multicast-address"},
      description = "Multicast address to use for the clients (default: ${DEFAULT-VALUE}).",
      defaultValue = "230.0.0.0")
  protected String multicastAddress;

  @CommandLine.Option(
      names = {"-H", "--host"},
      description = "Server host (default: ${DEFAULT-VALUE}).",
      defaultValue = "localhost")
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

  private static final Logger LOGGER = Logger.getLogger(Server.class.getName());

  private ServerProtocol protocol;
  private ServerState state;

  public enum Command {
    OK,
    USER_JOIN_ERR,
    NEW_USER,
    USER_READY,
    START_GAME,
    END_GAME,
  }

  @Override
  public Integer call() {
    int exitCode = 0;
    try {
      startServer();
    } catch (IOException e) {
      LOGGER.severe("Server encountered an error: " + e.getMessage());
      exitCode = 1;
    } finally {
      if (protocol != null) {
        protocol.closeSockets();
      }
    }
    return exitCode;
  }

  private void startServer() throws IOException {
    state = new ServerState();
    protocol = new ServerProtocol(port, multicastAddress, multicastPort);
    LOGGER.info("Listening on http://" + host + ":" + port + "...");

    protocol.listenForUnicastMessages(this::handleMessage);
  }

  private void handleMessage(Message message) {
    String[] parts = message.getParts();
    Client.Command command = null;

    try {
      command = Client.Command.valueOf(parts[0]);
    } catch (IllegalArgumentException e) {
      LOGGER.warning("Received unknown command: " + parts[0]);
      return;
    }

    switch (command) {
      case USER_JOIN:
        handleUserJoin(parts[1], message.address, message.port);
        break;
      default:
        LOGGER.warning("Unhandled command: " + command);
    }
  }

  private void handleUserJoin(String username, InetAddress address, int port) {
    if (state.usernameExists(username)) {
      protocol.sendUnicast(
          new Message(Command.USER_JOIN_ERR + " Username already taken", address, port));
      return;
    }

    state.registerClient(username, new ClientInfo(address, port));
    protocol.sendUnicast(new Message(Command.OK.toString(), address, port));
    protocol.broadcast(Command.NEW_USER + " " + username);
  }
}
