package ch.heigvd.dai.commands;

import ch.heigvd.dai.logic.client.ClientProtocol;
import ch.heigvd.dai.logic.client.ClientState;
import ch.heigvd.dai.logic.client.ui.TerminalUI;
import ch.heigvd.dai.logic.client.ui.event.UIEvent;
import ch.heigvd.dai.logic.shared.BaseState;
import ch.heigvd.dai.logic.shared.Message;
import ch.heigvd.dai.logic.shared.Player;
import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.logging.Logger;
import picocli.CommandLine;

@CommandLine.Command(name = "client", description = "Start a client to connect to the server")
public class Client implements Callable<Integer> {
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

  public enum Command {
    USER_JOIN,
    USER_READY,
    USER_PROGRESS,
    USER_QUIT,
  }

  private static final Logger LOGGER = Logger.getLogger(Client.class.getName());

  private final Scanner scanner = new Scanner(System.in);
  private ClientProtocol network;
  private ClientState state;

  @Override
  public Integer call() {
    try {
      network =
          new ClientProtocol(
              serverHost,
              serverPort,
              serverMulticastAddress,
              serverMulticastPort,
              networkInterface);
      connectToGame();
      startGameUI();
    } catch (Exception e) {
      LOGGER.severe("Error in client: " + e.getMessage());
      return 1;
    }
    return 0;
  }

  /**
   * Connect to the game by sending a USER_JOIN command to the server.
   *
   * @throws IOException if an error occurs while sending the command
   */
  private void connectToGame() throws IOException {
    boolean success = false;
    do {
      System.out.print("Enter your username: ");
      String username = scanner.nextLine().strip();

      if (username.isEmpty()) {
        System.out.println("ERROR: Username cannot be empty");
        continue;
      }
      if (username.contains(" ")) {
        System.out.println("ERROR: Username cannot contain spaces");
        continue;
      }

      Message res = network.sendWithResponseUnicast(Command.USER_JOIN, username);
      Server.Command command = Server.Command.fromString(res.getParts()[0]);

      switch (command) {
        case OK:
          handleSuccessfulJoin(username, res.getParts());
          success = true;
          break;
        case USER_JOIN_ERR:
          handleJoinError(res.getParts());
          break;
        case null:
        default:
          LOGGER.severe("Unknown message");
      }
    } while (!success);
    // Signal the server when quitting
    Runtime.getRuntime().addShutdownHook(new Thread(this::quit));
  }

  /**
   * Start the game UI and listen to multicast messages from the server.
   *
   * @throws IOException if an error occurs while starting the UI
   * @throws InterruptedException if the UI is interrupted
   */
  private void startGameUI() throws IOException, InterruptedException {
    TerminalUI ui = new TerminalUI(state, network);
    ui.start();
    network.listenToMulticast(this::handleMulticastMessage); // Blocking until the socket is closed
    ui.end();
    ui.join();
  }

  /**
   * Handle a successful join to the server.
   *
   * @param username the username of the player
   * @param parts the parts of the command
   */
  private void handleSuccessfulJoin(String username, String[] parts) {
    LOGGER.info("Successfully joined the server!");
    state = new ClientState(username);
    state.setGameState(BaseState.GameState.WAITING);

    // Add existing users to state
    for (int i = 1; i + 1 < parts.length; i += 2) {
      String player = parts[i];
      String status = parts[i + 1];
      if (!player.isEmpty()) {
        state.addPlayer(player);
        Server.CommandPlayerState playerState = Server.CommandPlayerState.fromString(status);
        if (playerState == null) {
          throw new RuntimeException("Unknown player state: " + status);
        }
        switch (playerState) {
          case READY -> state.setPlayerReady(player);
          case IN_GAME -> state.getPlayers().get(player).setInGame(true);
        }
      }
    }
  }

  /**
   * Handle a join error from the server.
   *
   * @param parts the parts of the command
   */
  private void handleJoinError(String[] parts) {
    String err = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
    LOGGER.info("Could not join server" + ": " + err);
    System.out.println("ERROR: " + err);
  }

  /** Send a USER_QUIT command to the server to signal the end of the game. */
  private void quit() {
    try {
      network.sendUnicast(Command.USER_QUIT, state.getSelfUsername());
    } catch (IOException e) {
      // Note: We ignore the exception as this is called through an exit signal
      LOGGER.severe("Failed to disconnect from server");
    }
  }

  /**
   * Handle a multicast message from the server.
   *
   * @param message the message to handle
   */
  private void handleMulticastMessage(String message) {
    String[] parts = message.split("\\s+");
    LOGGER.info("Received message: " + message);

    Server.Command command = Server.Command.fromString(parts[0]);
    if (command == null) {
      LOGGER.warning("Received unknown command: " + message);
      return;
    }

    switch (command) {
      case NEW_USER -> handleUserJoin(parts[1]);
      case USER_READY -> handleUserReady(parts[1]);
      case START_GAME -> handleStartGame(message.split("\\s+", 2)[1]);
      case ALL_USERS_PROGRESS -> handleUpdateUsersProgress(parts);
      case END_GAME -> handleEndGame(parts[1]);
      case DEL_USER -> handleUserDelete(parts[1]);
      case ERROR -> LOGGER.warning("Error: " + message.split("\\s+", 2)[1]);
      default -> LOGGER.warning("Unhandled multicast message: " + message);
    }
  }

  /**
   * Handle a new user joining the server.
   *
   * @param username the username of the player
   */
  private void handleUserJoin(String username) {
    if (!username.equals(state.getSelfUsername())) {
      state.addPlayer(username);
    }
  }

  /**
   * Handle a user readying up to the server.
   *
   * @param username the username of the player
   */
  private void handleUserReady(String username) {
    if (username.equals(state.getSelfUsername())) {
      return;
    }
    if (!state.playerExists(username)) {
      state.addPlayer(username);
    }
    state.setPlayerReady(username);
  }

  /**
   * Handle a start game command from the server.
   *
   * @param text the text of the game
   */
  private void handleStartGame(String text) {
    state.setGameState(BaseState.GameState.RUNNING);
    for (Player player : state.getPlayers().values()) {
      player.setInGame(true);
    }
    state.fireUIEvent(new UIEvent(UIEvent.EventType.RACE_TEXT_RECEIVED, text));
  }

  /**
   * Handle a user progress update from the server.
   *
   * @param message the message to handle
   */
  private void handleUpdateUsersProgress(String[] message) {
    for (int i = 1; i + 1 < message.length; i += 2) {
      state.setPlayerProgress(message[i], Integer.parseInt(message[i + 1]));
    }
  }

  /**
   * Handle a user delete command from the server.
   *
   * @param username the username of the player
   */
  private void handleUserDelete(String username) {
    state.removePlayer(username);
  }

  /**
   * Handle an end game command from the server.
   *
   * @param winner the username of the winner
   */
  private void handleEndGame(String winner) {
    if (state.getSelf().isInGame()) {
      state.setEndGameWinner(winner);
    }
    state.resetPlayers();
  }
}
