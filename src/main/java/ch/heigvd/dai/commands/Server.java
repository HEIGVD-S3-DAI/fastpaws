package ch.heigvd.dai.commands;

import ch.heigvd.dai.logic.server.ClientInfo;
import ch.heigvd.dai.logic.server.ServerProtocol;
import ch.heigvd.dai.logic.server.ServerState;
import ch.heigvd.dai.logic.server.TypingGame;
import ch.heigvd.dai.logic.shared.BaseState;
import ch.heigvd.dai.logic.shared.Message;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
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
    WAIT,
    NEW_USER,
    USER_READY,
    CURRENT_USERS_READY,
    START_GAME,
    ALL_USERS_PROGRESS,
    END_GAME,
    DEL_USER,
    ERROR,
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
    Client.Command command = parseClientCommand(parts);

    if (command == null) {
      LOGGER.warning("Received unknown command: " + parts[0]);
      handleUnknownCommand(message.address, message.port);
      return;
    }

    if (!hasValidArgumentCount(command, parts)) {
      handleIllegalNumberOfArguments(message.address, message.port);
      return;
    }

    switch (command) {
      case USER_JOIN:
        handleUserJoin(parts[1], message.address, message.port);
        break;
      case USER_READY:
        handleUserReady(parts[1], message.address, message.port);
        break;
      case USER_PROGRESS:
        handleUserProgress(parts[1], message.address, message.port, Integer.parseInt(parts[2]));
        break;
      case USER_QUIT:
        handleUserQuit(parts[1], message.address, message.port);
        break;
      default:
        LOGGER.warning("Unhandled command: " + command);
        handleUnknownCommand(message.address, message.port);
    }
  }

  private Client.Command parseClientCommand(String[] parts) {
    if (parts.length == 0) return null;
    try {
      return Client.Command.valueOf(parts[0]);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private boolean hasValidArgumentCount(Client.Command command, String[] parts) {
    return switch (command) {
      case USER_JOIN, USER_READY, USER_QUIT -> parts.length == 2;
      case USER_PROGRESS -> parts.length == 3;
    };
  }

  private void sendErrorResponse(InetAddress address, int port, String message) {
    protocol.sendUnicast(new Message(Command.ERROR + " " + message, address, port));
  }

  private void handleUserJoin(String username, InetAddress address, int port) {
    // Validate username
    if (state.usernameExists(username)) {
      protocol.sendUnicast(
          new Message(Command.USER_JOIN_ERR + " Username already taken", address, port));
      return;
    }
    if (username.length() > 15) {
      protocol.sendUnicast(
          new Message(
              Command.USER_JOIN_ERR + " Username must be maximux 15 characters long",
              address,
              port));
      return;
    }
    if (!username.matches("[A-Za-z0-9]+")) {
      protocol.sendUnicast(
          new Message(
              Command.USER_JOIN_ERR + " Username can only contain letters and digits",
              address,
              port));
      return;
    }

    if (!state.isGameRunning()) {
      handleSuccessfulJoin(username, address, port);
    } else {
      protocol.sendUnicast(new Message(Command.WAIT.toString(), address, port));
    }
  }

  private void handleSuccessfulJoin(String username, InetAddress address, int port) {
    state.setGameState(BaseState.GameState.WAITING);
    state.registerClient(username, new ClientInfo(address, port));

    // Build list of current players
    StringBuilder currentUsers = new StringBuilder();
    for (String existingUser : state.getConnectedClients().keySet()) {
      if (!existingUser.equals(username)) {
        currentUsers.append(" ").append(existingUser);
      }
    }

    // Send OK with current users list
    protocol.sendUnicast(new Message(Command.OK + currentUsers.toString(), address, port));
    // Notify others of new user
    protocol.multicast(Command.NEW_USER + " " + username);
  }

  private void handleUserReady(String username, InetAddress address, int port) {
    if (!state.usernameExists(username)) {
      sendErrorResponse(address, port, "User doesn't exist.");
      return;
    }

    state.setUserReady(username);
    sendReadyStatusUpdates(username, address, port);

    if (canStartGame()) {
      startGame();
    }
  }

  private void sendReadyStatusUpdates(String username, InetAddress address, int port) {
    String currentUsersReady =
        state.getConnectedClients().keySet().stream()
            .filter(state::isUserReady)
            .reduce("", (acc, player) -> acc + " " + player);

    protocol.sendUnicast(
        new Message(Command.CURRENT_USERS_READY + currentUsersReady, address, port));
    protocol.multicast(Command.USER_READY + " " + username);
  }

  private boolean canStartGame() {
    return state.getNumPlayers() >= TypingGame.MIN_PLAYERS_FOR_GAME && state.areAllUsersReady();
  }

  private void startGame() {
    LOGGER.info("Starting game in " + TypingGame.GAME_START_DELAY + " seconds...");
    try {
      TimeUnit.SECONDS.sleep(TypingGame.GAME_START_DELAY);
      protocol.multicast(Command.START_GAME + " " + TypingGame.getParagraph());
      state.setGameState(BaseState.GameState.RUNNING);
      new Thread(this::multicastProgress).start();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private void handleUserProgress(String username, InetAddress address, int port, int progress) {
    LOGGER.info("USER_PROGRESS: " + username + " " + progress);
    if (!state.usernameExists(username)) {
      protocol.sendUnicast(new Message(Command.ERROR + " " + "User doesn't exist.", address, port));
    } else if (progress < 0 || progress > 100) {
      protocol.sendUnicast(new Message(Command.ERROR + " " + "Invalid score.", address, port));
    } else {
      if (state.isGameRunning()) {
        state.setPlayerProgress(username, progress);
        if (progress == 100) {
          state.setGameState(BaseState.GameState.FINISHED);
          protocol.multicast(Command.END_GAME + " " + username);
          state.resetPlayers();
        }
      }
    }
  }

  private void multicastProgress() {
    while (state.isGameRunning()) {
      StringBuilder sb = new StringBuilder();
      for (Map.Entry<String, ClientInfo> entry : state.getConnectedClients().entrySet()) {
        sb.append(" ");
        sb.append(entry.getKey());
        sb.append(" ");
        sb.append(entry.getValue().player.getProgress());
      }
      protocol.multicast(Command.ALL_USERS_PROGRESS + sb.toString());
      try {
        TimeUnit.SECONDS.sleep(2);
      } catch (InterruptedException e) {
        throw new RuntimeException(e); // We want to crash the server, something is going wrong
      }
    }
  }

  private void handleUserQuit(String username, InetAddress address, int port) {
    if (!state.usernameExists(username)) {
      protocol.sendUnicast(new Message(Command.ERROR + " " + "User doesn't exist.", address, port));
    } else {
      protocol.multicast(Command.DEL_USER + " " + username);
      state.removeUser(username);
      if (state.getConnectedClients().isEmpty()) {
        state.setGameState(BaseState.GameState.FINISHED);
      }
    }
  }

  private void handleIllegalNumberOfArguments(InetAddress address, int port) {
    protocol.sendUnicast(
        new Message(Command.ERROR + " Illegal number of arguments.", address, port));
  }

  private void handleUnknownCommand(InetAddress address, int port) {
    protocol.sendUnicast(new Message(Command.ERROR + " Unknown command.", address, port));
  }
}
