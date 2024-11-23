package ch.heigvd.dai.commands;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import java.util.logging.Logger;
import picocli.CommandLine;

@CommandLine.Command(name = "client", description = "Start a client to connect to the server")
public class Client implements Callable<Integer> {

    private static final Logger LOGGER = Logger.getLogger(Client.class.getName());
    private static final int BUFFER_SIZE = 1024;
    private static final int TIMEOUT_MS = 5000;

    @CommandLine.ParentCommand
    private Root parent;

    @CommandLine.Option(names = { "-M",
            "--multicast-address" }, description = "Multicast address to use for the clients (default: ${DEFAULT-VALUE}).", defaultValue = "230.0.0.0")
    protected String multicastAddress;

    @CommandLine.Option(names = { "-p",
            "--port" }, description = "Port to use for the clients (default: ${DEFAULT-VALUE}).", defaultValue = "1732")
    protected int port;

    public enum Message {
        USER_JOIN,
        USER_READY,
        USERS_PROGRESS,
        USER_QUIT,
    }

    @Override
    public Integer call() {
        join();
        return 0;
    }

    private void join() {
        sendToServer(Message.USER_JOIN, "John");
        String res = waitServerResponse();
        String[] parts = res.split("\\s+");

        Server.Message command = null;
        try {
            command = Server.Message.valueOf(parts[0]);
        } catch (Exception e) {
            // Do nothing
        }

        switch (command) {
            case Server.Message.USER_JOIN_OK:
                LOGGER.info("Successfully joined the server!");
                break;
            case Server.Message.USER_JOIN_ERR:
                LOGGER.info("Could not join server");
                break;
            default:
                LOGGER.severe("Unkown message");
        }
    }

    private void sendToServer(Message command, String message) {
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress multicastAddress = InetAddress.getByName(this.multicastAddress);

            String messageToSend = command + " " + message;
            byte[] buffer = messageToSend.getBytes(StandardCharsets.UTF_8);

            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, multicastAddress, port);

            socket.send(packet);
        } catch (Exception e) {
            LOGGER.severe("An error occurred: " + e.getMessage());
        }
    }
}
