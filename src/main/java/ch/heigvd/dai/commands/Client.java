package ch.heigvd.dai.commands;

import ch.heigvd.dai.logic.client.ClientProtocol;
import ch.heigvd.dai.logic.client.ClientState;
import ch.heigvd.dai.logic.client.ui.TerminalRenderer;
import ch.heigvd.dai.logic.client.ui.event.UIEvent;
import ch.heigvd.dai.logic.shared.BaseState;
import ch.heigvd.dai.logic.shared.Message;
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
  private ClientProtocol protocol;
  private ClientState state;
  private TerminalRenderer renderer;

  @Override
  public Integer call() {
    try {
      protocol =
          new ClientProtocol(
              serverHost,
              serverPort,
              serverMulticastAddress,
              serverMulticastPort,
              networkInterface);
      join();
      // Signal the server when quitting
      Runtime.getRuntime().addShutdownHook(new Thread(() -> quit()));
      renderer = new TerminalRenderer(state, protocol);
      renderer.start();
      protocol.listenToMulticast(this::handleMulticastMessage);
      renderer.end();
      renderer.join();
    } catch (Exception e) {
      LOGGER.severe("Error in client: " + e.getMessage());
      return 1;
    }
    return 0;
  }

  private void join() throws IOException {
    boolean success = false;
    do {
      System.out.print("Enter your username: ");
      String username = scanner.nextLine();

      if (username.length() == 0) {
        System.out.println("ERROR: Username cannot be empty");
        continue;
      }

      Message res = protocol.sendWithResponseUnicast(Command.USER_JOIN, username);
      Server.Command command = parseServerCommand(res.getParts());

      switch (command) {
        case OK:
          handleSuccessfulJoin(username, res.getParts());
          success = true;
          break;
        case USER_JOIN_ERR:
          handleError("Could not join server", res.getParts());
          break;
        case WAIT:
          LOGGER.info("Waiting for the current game to finish...");
          protocol.listenToMulticast(this::waitForNextGame);
          break;
        case null:
        default:
          LOGGER.severe("Unknown message");
      }
    } while (!success);
  }

  private Server.Command parseServerCommand(String[] parts) {
    if (parts.length == 0) return null;
    try {
      return Server.Command.valueOf(parts[0]);
    } catch (Exception e) {
      return null;
    }
  }

  private void handleSuccessfulJoin(String username, String[] parts) {
    LOGGER.info("Successfully joined the server!");
    state = new ClientState(username);
    state.setGameState(BaseState.GameState.WAITING);

    // Add existing users to state
    for (int i = 1; i < parts.length; i++) {
      if (!parts[i].isEmpty()) {
        state.addPlayer(parts[i]);
      }
    }
  }

  private void handleError(String context, String[] parts) {
    String err = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
    LOGGER.info(context + ": " + err);
    System.out.println("ERROR: " + err);
  }

  private void quit() {
    try {
      protocol.sendUnicast(Command.USER_QUIT, state.getSelfUsername());
    } catch (IOException e) {
      // Note: We ignore the exception as this is called through an exit signal
      LOGGER.severe("Failed to disconnect from server");
    }
  }

  private void waitForNextGame(String message) {
    String[] parts = message.split("\\s+");
    LOGGER.info("Received message: " + message);

    Server.Command command = null;
    try {
      command = Server.Command.valueOf(parts[0]);
    } catch (Exception e) {
      LOGGER.warning("Received unknown command: " + message);
    }
    if (command == Server.Command.END_GAME) {
      protocol.closeMulticast();
    }
  }

  private void handleMulticastMessage(String message) {
    String[] parts = message.split("\\s+");
    LOGGER.info("Received message: " + message);

    Server.Command command = parseServerCommand(parts);
    if (command == null) {
      LOGGER.warning("Received unknown command: " + message);
      return;
    }

    switch (command) {
      case NEW_USER:
        if (!parts[1].equals(state.getSelfUsername())) {
          state.addPlayer(parts[1]);
        }
        break;
      case USER_READY:
        handleUserReady(parts[1]);
        break;
      case START_GAME:
        handleStartGame(message.split("\\s+", 2)[1]);
        break;
      case ALL_USERS_PROGRESS:
        handleUpdateUsersProgress(parts);
        break;
      case END_GAME:
        handleEndGame(parts[1]);
        break;
      case DEL_USER:
        handleUserDelete(parts[1]);
        break;
      case ERROR:
        LOGGER.warning("Error : " + message.split("\\s+", 2)[1]);
        break;
      default:
        LOGGER.warning("Unhandled multicast message: " + message);
    }
  }

  private void handleUserReady(String username) {
    if (username.equals(state.getSelfUsername())) {
      return;
    }
    if (!state.playerExists(username)) {
      state.addPlayer(username);
    }
    state.setPlayerReady(username);
  }

  private void handleStartGame(String text) {
    state.setGameState(BaseState.GameState.RUNNING);
    state.fireUIEvent(new UIEvent(UIEvent.EventType.RACE_TEXT_RECEIVED, text));
  }

  private void handleUpdateUsersProgress(String[] message) {
    for (int i = 1; i + 1 < message.length; i += 2) {
      state.setPlayerProgress(message[i], Integer.parseInt(message[i + 1]));
    }
  }

  private void handleUserDelete(String username) {
    state.removePlayer(username);
  }

  private void handleEndGame(String winner) {
    // TODO: Move this to the TerminalRenderer
    if (winner.equals(state.getSelfUsername())) {
      LOGGER.info("End of the game. You're the winner !!!\n");
    } else {
      LOGGER.info("End of the game, the winner is : " + winner);
    }
    state.resetPlayers();
    state.setGameState(BaseState.GameState.FINISHED);
  }
}
