package ch.heigvd.dai.logic.client.ui.display;

import ch.heigvd.dai.commands.Client;
import ch.heigvd.dai.commands.Server;
import ch.heigvd.dai.logic.client.ui.TerminalUI;
import ch.heigvd.dai.logic.server.TypingGame;
import ch.heigvd.dai.logic.shared.Message;
import ch.heigvd.dai.logic.shared.Player;
import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import java.io.IOException;
import java.util.Map;

public class LobbyDisplayState extends DisplayState {

  private static final String[] HELLO_ASCII = {
    "   ____         __  ___                ",
    "  / __/__ ____ / /_/ _ \\___ __    _____",
    " / _// _ `(_-</ __/ ___/ _ `/ |/|/ (_-<",
    "/_/  \\_,_/___/\\__/_/   \\_,_/|__,__/___/",
    "",
  };

  private static final String[] GAME_INSTRUCTIONS = {
    "Type the given paragraph without any",
    "mistakes to win the game. Good luck!",
    "At anytime, press ESC or [q] to quit."
  };

  private long gameStartTime = -1;

  public LobbyDisplayState(TerminalUI ui) {
    super(ui);
  }

  public void render(TextGraphics tg) {
    displayHello(tg);
    displayPlayerList(tg);
  }

  private void displayHello(TextGraphics tg) {
    tg.setForegroundColor(TextColor.ANSI.MAGENTA);
    tg.enableModifiers(SGR.BOLD);
    for (int i = 0; i < HELLO_ASCII.length; ++i) {
      tg.putString(0, i, HELLO_ASCII[i]);
    }
    tg.disableModifiers(SGR.BOLD);
    tg.setForegroundColor(TextColor.ANSI.BLACK_BRIGHT);
    for (int i = 0; i < GAME_INSTRUCTIONS.length; ++i) {
      tg.putString(0, HELLO_ASCII.length + i, GAME_INSTRUCTIONS[i]);
    }
    tg.setForegroundColor(TextColor.ANSI.DEFAULT);
  }

  private void displayPlayerList(TextGraphics tg) {
    int offset = HELLO_ASCII.length + GAME_INSTRUCTIONS.length;
    String selfUsername = ui.getClientState().getSelfUsername();
    Player self = ui.getClientState().getPlayers().get(selfUsername);
    int numConnected = ui.getClientState().getPlayers().size();
    boolean allPlayersReady = ui.getClientState().allPlayersReady();

    tg.enableModifiers(SGR.BOLD);
    tg.enableModifiers(SGR.UNDERLINE);
    tg.putString(
        0,
        ++offset,
        "Connected Players (" + numConnected + "/" + TypingGame.MIN_PLAYERS_FOR_GAME + "):");
    tg.disableModifiers(SGR.BOLD);
    tg.disableModifiers(SGR.UNDERLINE);
    tg.putString(
        0,
        ++offset,
        "  - " + selfUsername + (self.isReady() ? " [ready]" : " [not ready]") + " (you)");
    for (Map.Entry<String, Player> entry : ui.getClientState().getPlayers().entrySet()) {
      String username = entry.getKey();
      if (username.equals(selfUsername)) {
        continue;
      }
      Player player = entry.getValue();
      String status;
      if (player.isInGame()) {
        status = " [in game " + player.getProgress() + "%]";
      } else {
        status = player.isReady() ? " [ready]" : " [not ready]";
      }
      tg.putString(0, ++offset, "  - " + username + status);
    }
    offset++;

    if (!self.isReady() && !ui.getClientState().isPlayerInGame()) {
      tg.putString(0, ++offset, "Press ENTER when ready");
    } else if (allPlayersReady && numConnected >= TypingGame.MIN_PLAYERS_FOR_GAME) {
      if (gameStartTime == -1) {
        gameStartTime = System.currentTimeMillis() + TypingGame.GAME_START_DELAY * 1000;
      }
      long remainingTime = Math.max(0, (gameStartTime - System.currentTimeMillis()) / 1000);
      tg.putString(0, ++offset, "Game starting in " + remainingTime + " seconds...");
    } else {
      gameStartTime = -1;
    }
  }

  @Override
  public void handleInput(KeyStroke keyStroke) throws IOException {
    super.handleInput(keyStroke);

    // If the user presses enter, set the player ready
    if (keyStroke.getKeyType() == KeyType.Enter
        && !ui.getClientState().getSelf().isReady()
        && !ui.getClientState().isPlayerInGame()) {
      String selfUsername = ui.getClientState().getSelfUsername();
      ui.getClientState().setPlayerReady(selfUsername);
      Message res =
          ui.getNetwork().sendWithResponseUnicast(Client.Command.USER_READY, selfUsername);

      String[] parts = res.getParts();
      Server.Command command = Server.Command.fromString(parts[0]);
      if (command == Server.Command.CURRENT_USERS_READY) {
        for (int i = 1; i < parts.length; i++) {
          if (ui.getClientState().playerExists(parts[i])) continue;
          ui.getClientState().addPlayer(parts[i]);
          ui.getClientState().setPlayerReady(parts[i]);
        }
      }
    }
  }
}
