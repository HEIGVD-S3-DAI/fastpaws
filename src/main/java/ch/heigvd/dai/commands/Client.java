package ch.heigvd.dai.commands;

import ch.heigvd.dai.logic.client.ClientProtocol;
import ch.heigvd.dai.logic.client.ClientState;
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
      setReady();
      protocol.listenToMulticast(this::handleMulticastMessage);
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

      Message res = protocol.sendWithResponseUnicast(Command.USER_JOIN, username);
      String[] parts = res.getParts();

      Server.Command command = null;
      try {
        command = Server.Command.valueOf(parts[0]);
      } catch (Exception e) {
        // Do nothing
      }

      switch (command) {
        case OK:
          LOGGER.info("Successfully joined the server!");
          state = new ClientState(username);
          state.setGameState(BaseState.GameState.WAITING);
          success = true;
          break;
        case USER_JOIN_ERR:
          String err = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
          LOGGER.info("Could not join server: " + err);
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

  private void quit() {
    try {
      protocol.sendUnicast(Command.USER_QUIT, state.getSelfUsername());
    } catch (IOException e) {
      LOGGER.severe("Failed to disconnect from server");
      // TODO: For now we ignore, but how should we handle this?
    }
  }

  private void setReady() throws Exception {
    System.out.println("Press ENTER when your are ready...");
    scanner.nextLine(); // Wait enter key
    state.setPlayerReady(state.getSelfUsername());
    Message res = protocol.sendWithResponseUnicast(Command.USER_READY, state.getSelfUsername());

    LOGGER.info("Received message: " + res.str);
    String[] parts = res.getParts();
    Server.Command command = null;
    try {
      command = Server.Command.valueOf(parts[0]);
    } catch (Exception e) {
      // Do nothing
    }
    if (command == Server.Command.CURRENT_USERS_READY) {
      for (int i = 1; i < parts.length; i++) {
        if (state.playerExists(parts[i])) continue;
        state.addPlayer(parts[i]);
        state.setPlayerReady(parts[i]);
      }
    } else {
      LOGGER.warning("Unknown message.");
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

    Server.Command command = null;
    try {
      command = Server.Command.valueOf(parts[0]);
    } catch (Exception e) {
      LOGGER.warning("Received unknown command: " + message);
    }

    switch (command) {
      case NEW_USER:
        if (!parts[1].equals(state.getSelfUsername())) state.addPlayer(parts[1]);
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
      case null:
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
    System.out.println(text);
    System.out.print("> ");
    String userInput = scanner.nextLine();
    try {
      protocol.sendUnicast(Command.USER_PROGRESS, state.getSelfUsername() + " " + "100");
    } catch (IOException e) {
      LOGGER.severe("Failed to update progress");
    }
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
    if (winner.equals(state.getSelfUsername())) {
      LOGGER.info("End of the game. You're the winner !!!\n");
    } else {
      LOGGER.info("End of the game, the winner is : " + winner);
    }
    state.resetPlayers();
    state.setGameState(BaseState.GameState.FINISHED);
    try {
      setReady();
    } catch (Exception e) {
      throw new RuntimeException(e); // Crash the client
    }
  }
}
