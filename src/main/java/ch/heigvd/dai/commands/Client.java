package ch.heigvd.dai.commands;

import ch.heigvd.dai.ui.Game;
import java.util.concurrent.Callable;
import picocli.CommandLine;

@CommandLine.Command(name = "client", description = "Start a client to connect to the server")
public class Client implements Callable<Integer> {

  @CommandLine.ParentCommand private Root parent;

  @Override
  public Integer call() {
    Game game = new Game();
    game.run();
    return 0;
  }
}
