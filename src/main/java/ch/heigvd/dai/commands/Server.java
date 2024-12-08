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
    description = "Start the server",
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

  private ServerProtocol network;
  private ServerState state;

  public enum Command {
    OK,
    USER_JOIN_ERR,
    NEW_USER,
    USER_READY,
    START_GAME,
    ALL_USERS_PROGRESS,
    END_GAME,
    DEL_USER,
    ERROR;

    public static Command fromString(String text) {
      try {
        return valueOf(text);
      } catch (IllegalArgumentException e) {
        return null;
      }
    }
  }

  public enum CommandPlayerState {
    NOT_READY,
    READY,
    IN_GAME;

    public static CommandPlayerState fromString(String text) {
      try {
        return valueOf(text);
      } catch (IllegalArgumentException e) {
        return null;
      }
    }
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
      if (network != null) {
        network.closeSockets();
      }
    }
    return exitCode;
  }

  /**
   * Start the server and listen for unicast messages.
   *
   * @throws IOException if an error occurs while starting the server
   */
  private void startServer() throws IOException {
    state = new ServerState();
    network = new ServerProtocol(port, multicastAddress, multicastPort);
    LOGGER.info("Listening on http://" + host + ":" + port + "...");

    network.listenForUnicastMessages(this::handleMessage);
  }

  /**
   * Handle a unicast message from a client.
   *
   * @param message the message to handle
   */
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
      case USER_JOIN -> handleUserJoin(parts[1], message.address, message.port);
      case USER_READY -> handleUserReady(parts[1], message.address, message.port);
      case USER_PROGRESS ->
          handleUserProgress(parts[1], message.address, message.port, Integer.parseInt(parts[2]));
      case USER_QUIT -> handleUserQuit(parts[1], message.address, message.port);
      default -> {
        LOGGER.warning("Unhandled command: " + command);
        handleUnknownCommand(message.address, message.port);
      }
    }
  }

  /**
   * Parse a client command from an array of parts.
   *
   * @param parts the parts of the command
   * @return the parsed command or null if the command is unknown
   */
  private Client.Command parseClientCommand(String[] parts) {
    if (parts.length == 0) return null;
    try {
      return Client.Command.valueOf(parts[0]);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  /**
   * Check if the number of arguments for a client command is valid.
   *
   * @param command the command to check
   * @param parts the parts of the command
   * @return true if the number of arguments is valid, false otherwise
   */
  private boolean hasValidArgumentCount(Client.Command command, String[] parts) {
    return switch (command) {
      case USER_JOIN, USER_READY, USER_QUIT -> parts.length == 2;
      case USER_PROGRESS -> parts.length == 3;
    };
  }

  /**
   * Handle a user join to the server.
   *
   * @param username the username of the player
   * @param address the address of the player
   * @param port the port of the player
   */
  private void handleUserJoin(String username, InetAddress address, int port) {
    // Validate username
    if (state.usernameExists(username)) {
      network.sendUnicast(
          new Message(Command.USER_JOIN_ERR + " Username already taken", address, port));
      return;
    }
    if (username.length() > 15) {
      network.sendUnicast(
          new Message(
              Command.USER_JOIN_ERR + " Username must be maximux 15 characters long",
              address,
              port));
      return;
    }
    if (!username.matches("[A-Za-z0-9]+")) {
      network.sendUnicast(
          new Message(
              Command.USER_JOIN_ERR + " Username can only contain letters and digits",
              address,
              port));
      return;
    }

    // Always allow joining, regardless of game state
    handleSuccessfulJoin(username, address, port);
  }

  /**
   * Handle a successful join to the server.
   *
   * @param username the username of the player
   * @param address the address of the player
   * @param port the port of the player
   */
  private void handleSuccessfulJoin(String username, InetAddress address, int port) {
    state.registerClient(username, new ClientInfo(address, port));

    // Build list of current players with their ready state or in-game state
    StringBuilder currentUsers = new StringBuilder();
    for (Map.Entry<String, ClientInfo> entry : state.getConnectedClients().entrySet()) {
      String existingUser = entry.getKey();
      if (!existingUser.equals(username)) {
        CommandPlayerState playerState =
            state.isGameRunning()
                ? CommandPlayerState.IN_GAME
                : (state.isUserReady(existingUser)
                    ? CommandPlayerState.READY
                    : CommandPlayerState.NOT_READY);
        currentUsers.append(" ").append(existingUser).append(" ").append(playerState);
      }
    }

    // Send OK with current users list and their states
    network.sendUnicast(new Message(Command.OK + currentUsers.toString(), address, port));
    // Notify others of new user
    network.multicast(Command.NEW_USER + " " + username);
  }

  /**
   * Handle a user ready to the server.
   *
   * @param username the username of the player
   * @param address the address of the player
   * @param port the port of the player
   */
  private void handleUserReady(String username, InetAddress address, int port) {
    if (!state.usernameExists(username)) {
      network.sendUnicast(new Message(Command.ERROR + " " + "User doesn't exist.", address, port));
      return;
    }

    if (state.isGameFinished()) {
      state.setGameState(BaseState.GameState.WAITING);
    }

    state.setUserReady(username);
    network.multicast(Command.USER_READY + " " + username);

    if (canStartGame()) {
      startGame();
    }
  }

  /**
   * Check if the game can start.
   *
   * @return true if the game can start, false otherwise
   */
  private boolean canStartGame() {
    return state.isGameWaiting()
        && state.getNumPlayers() >= TypingGame.MIN_PLAYERS_FOR_GAME
        && state.areAllUsersReady();
  }

  /** Start the game. Starts the game and a new thread to multicast progress updates. */
  private void startGame() {
    for (ClientInfo client : state.getConnectedClients().values()) {
      client.player.setInGame(true);
    }
    LOGGER.info("Starting game in " + TypingGame.GAME_START_DELAY + " seconds...");
    try {
      TimeUnit.SECONDS.sleep(TypingGame.GAME_START_DELAY);
      network.multicast(Command.START_GAME + " " + TypingGame.getParagraph());
      state.setGameState(BaseState.GameState.RUNNING);
      new Thread(this::multicastProgress).start();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Handle a user progress update from a client.
   *
   * @param username the username of the player
   * @param address the address of the player
   * @param port the port of the player
   * @param progress the progress of the player
   */
  private void handleUserProgress(String username, InetAddress address, int port, int progress) {
    LOGGER.info("USER_PROGRESS: " + username + " " + progress);
    if (!state.usernameExists(username)) {
      network.sendUnicast(new Message(Command.ERROR + " " + "User doesn't exist.", address, port));
    } else if (!state.getConnectedClients().get(username).player.isInGame()) {
      network.sendUnicast(new Message(Command.ERROR + " " + "User is not in game.", address, port));
    } else if (progress < 0 || progress > 100) {
      network.sendUnicast(new Message(Command.ERROR + " " + "Invalid score.", address, port));
    } else {
      if (state.isGameRunning()) {
        state.setPlayerProgress(username, progress);
        if (progress == 100) {
          state.setGameState(BaseState.GameState.FINISHED);
          network.multicast(Command.END_GAME + " " + username);
          state.resetPlayers();
        }
      }
    }
  }

  /** Multicast progress updates to all clients. */
  private void multicastProgress() {
    while (state.isGameRunning()) {
      StringBuilder sb = new StringBuilder();
      for (Map.Entry<String, ClientInfo> entry : state.getConnectedClients().entrySet()) {
        if (entry.getValue().player.isInGame()) {
          sb.append(" ");
          sb.append(entry.getKey());
          sb.append(" ");
          sb.append(entry.getValue().player.getProgress());
        }
      }
      network.multicast(Command.ALL_USERS_PROGRESS + sb.toString());
      try {
        TimeUnit.SECONDS.sleep(2);
      } catch (InterruptedException e) {
        throw new RuntimeException(e); // We want to crash the server, something is going wrong
      }
    }
  }

  /**
   * Handle a user quit from a client.
   *
   * @param username the username of the player
   * @param address the address of the player
   * @param port the port of the player
   */
  private void handleUserQuit(String username, InetAddress address, int port) {
    if (!state.usernameExists(username)) {
      network.sendUnicast(new Message(Command.ERROR + " " + "User doesn't exist.", address, port));
    } else {
      network.multicast(Command.DEL_USER + " " + username);
      state.removeUser(username);
      if (!state.isPlayerInGame()) {
        state.setGameState(BaseState.GameState.FINISHED);
      }
    }
  }

  /**
   * Handle an illegal number of arguments from a client.
   *
   * @param address the address of the player
   * @param port the port of the player
   */
  private void handleIllegalNumberOfArguments(InetAddress address, int port) {
    network.sendUnicast(
        new Message(Command.ERROR + " Illegal number of arguments.", address, port));
  }

  /**
   * Handle an unknown command from a client.
   *
   * @param address the address of the player
   * @param port the port of the player
   */
  private void handleUnknownCommand(InetAddress address, int port) {
    network.sendUnicast(new Message(Command.ERROR + " Unknown command.", address, port));
  }
}
