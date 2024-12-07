package ch.heigvd.dai.logic.client.ui.display;

import ch.heigvd.dai.commands.Client;
import ch.heigvd.dai.commands.Server;
import ch.heigvd.dai.logic.client.ui.TerminalRenderer;
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
  };

  private long gameStartTime = -1;

  public LobbyDisplayState(TerminalRenderer renderer) {
    super(renderer);
  }

  public void render(TextGraphics tg) throws IOException {
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
    tg.setForegroundColor(TextColor.ANSI.DEFAULT);
  }

  private void displayPlayerList(TextGraphics tg) {
    String selfUsername = renderer.getClientState().getSelfUsername();
    Player self = renderer.getClientState().getPlayers().get(selfUsername);
    int numConnected = renderer.getClientState().getPlayers().size();
    boolean allPlayersReady = renderer.getClientState().allPlayersReady();

    tg.enableModifiers(SGR.BOLD);
    tg.enableModifiers(SGR.UNDERLINE);
    tg.putString(
        0, 6, "Connected Players (" + numConnected + "/" + TypingGame.MIN_PLAYERS_FOR_GAME + "):");
    tg.disableModifiers(SGR.BOLD);
    tg.disableModifiers(SGR.UNDERLINE);
    tg.putString(
        0, 7, "  - " + selfUsername + " (you)" + (self.isReady() ? " [ready]" : " [not ready]"));

    int offset = 8;
    for (Map.Entry<String, Player> entry : renderer.getClientState().getPlayers().entrySet()) {
      String username = entry.getKey();
      if (username.equals(selfUsername)) {
        continue;
      }
      Player player = entry.getValue();
      tg.putString(0, offset, "  - " + username + (player.isReady() ? " [ready]" : " [not ready]"));
      offset++;
    }

    if (!self.isReady()) {
      offset++;
      tg.putString(0, offset, "Press ENTER when ready");
    } else if (allPlayersReady && numConnected >= TypingGame.MIN_PLAYERS_FOR_GAME) {
      offset++;
      if (gameStartTime == -1) {
        gameStartTime = System.currentTimeMillis() + TypingGame.GAME_START_DELAY * 1000;
      }

      long remainingTime = Math.max(0, (gameStartTime - System.currentTimeMillis()) / 1000);
      tg.putString(0, offset, "Game starting in " + remainingTime + " seconds...");
    } else {
      gameStartTime = -1;
    }
  }

  @Override
  public void handleInput(KeyStroke keyStroke) throws IOException {
    super.handleInput(keyStroke);

    if (keyStroke.getKeyType() == KeyType.Enter && !renderer.getClientState().getSelfIsReady()) {
      String selfUsername = renderer.getClientState().getSelfUsername();
      renderer.getClientState().setPlayerReady(selfUsername);
      Message res =
          renderer.getProtocol().sendWithResponseUnicast(Client.Command.USER_READY, selfUsername);

      String[] parts = res.getParts();
      Server.Command command = null;
      try {
        command = Server.Command.valueOf(parts[0]);
      } catch (Exception e) {
        // Do nothing
      }
      if (command == Server.Command.CURRENT_USERS_READY) {
        for (int i = 1; i < parts.length; i++) {
          if (renderer.getClientState().playerExists(parts[i])) continue;
          renderer.getClientState().addPlayer(parts[i]);
          renderer.getClientState().setPlayerReady(parts[i]);
        }
      }
    }
  }
}
