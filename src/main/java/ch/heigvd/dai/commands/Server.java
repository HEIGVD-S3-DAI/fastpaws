package ch.heigvd.dai.commands;

import java.util.concurrent.Callable;
import picocli.CommandLine;

@CommandLine.Command(name = "server", description = "Start a server to connect to the server")
public class Server implements Callable<Integer> {

  @CommandLine.ParentCommand private Root parent;

  @Override
  public Integer call() {
    System.out.println("Server called");
    return 0;
  }
}
