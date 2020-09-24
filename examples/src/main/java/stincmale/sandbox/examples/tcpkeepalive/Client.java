package stincmale.sandbox.examples.tcpkeepalive;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import static stincmale.sandbox.examples.tcpkeepalive.Server.BYE;
import static stincmale.sandbox.examples.tcpkeepalive.Server.log;
import static stincmale.sandbox.examples.tcpkeepalive.Server.parseCliArgs;
import static stincmale.sandbox.examples.tcpkeepalive.Server.printStackTraceToString;
import static stincmale.sandbox.examples.tcpkeepalive.Server.toUnsignedHexString;

/**
 * A trivial TCP client working according to the protocol specified by the {@link Server} class.
 */
final class Client {
  private static final int SO_CONNECT_TIMEOUT_MILLIS = Math.toIntExact(TimeUnit.SECONDS.toMillis(1));

  public static final void main(final String... args) throws IOException {
    final InetSocketAddress serverSocketAddress = parseCliArgs(args);
    //as much as possible, a client should initiate closing the connection, so that its socket stays in TIME-WAIT state, not the server's socket
    final int readTimeoutMillis = Server.SO_READ_TIMEOUT_MILLIS > 1
        ? Math.round(Server.SO_READ_TIMEOUT_MILLIS * 0.8f)
        : Server.SO_READ_TIMEOUT_MILLIS;
    boolean serverDisconnected = false;
    Socket socket = new Socket();
    socket.setKeepAlive(true);
    try {
      log("Connecting to " + serverSocketAddress);
      socket.connect(serverSocketAddress, SO_CONNECT_TIMEOUT_MILLIS);
      log("Connected via " + socket);
      startDaemonUserInputHandler(socket.getOutputStream(), socket.toString());
      socket.setSoTimeout(readTimeoutMillis);
      final InputStream in = socket.getInputStream();
      final byte[] inData = new byte[1];
      int receivedLength;
      byte inMessage;
      boolean receivedBye = false;
      do {
        receivedLength = in.read(inData);
        if (receivedLength > 0) {
          inMessage = inData[0];
          receivedBye = inMessage == BYE;
          processInMessage(inMessage, socket.toString());
        } else {
          serverDisconnected = true;
        }
      } while (!serverDisconnected && !receivedBye);
    } finally {
      if (serverDisconnected) {
        log("The server " + socket + " disconnected");
      }
      close(socket);
    }
  }

  private static final void startDaemonUserInputHandler(final OutputStream out, final String connectionDescription) {
    final Thread userInputHandler = new Thread(() -> {
      try {
        handleUserInput(out, connectionDescription);
      } catch (final Throwable e) {
        log(String.format("Exception when handling user input for %s. %s", connectionDescription, printStackTraceToString(e)));
      }
    });
    userInputHandler.setDaemon(true);
    userInputHandler.setName("user-input-handler");
    userInputHandler.start();
  }

  private static final void handleUserInput(final OutputStream out, final String connectionDescription) throws IOException, InterruptedException {
    final Scanner userInputScanner = new Scanner(System.in, Charset.defaultCharset());
    try {
      while (true) {
        final byte[] userInput = userInput(userInputScanner);
        for (byte outMessage : userInput) {
          sendOutMessage(outMessage, out, connectionDescription);
        }
        Thread.sleep(500);
      }
    } finally {
      close(userInputScanner);
    }
  }

  private static final byte[] userInput(final Scanner scanner) {
    System.out.printf("Specify data to be sent:%n");
    return scanner.next().getBytes(StandardCharsets.UTF_8);
  }

  private static final void processInMessage(final byte message, final String connectionDescription) {
    log("Received " + toUnsignedHexString(message) + " via " + connectionDescription);
  }

  static final void sendOutMessage(final byte message, final OutputStream out, final String connectionDescription) throws IOException {
    log("Sending " + toUnsignedHexString(message) + " via " + connectionDescription);
    out.write(message);
    out.flush();
  }

  private static final void close(final Scanner scanner) throws IOException {
    try (scanner) {
      final IOException e = scanner.ioException();
      if (e != null) {
        throw e;
      }
    }
  }

  /**
   * Gracefully terminates (see <a href="https://www.rfc-editor.org/rfc/rfc793.html#section-3.8">TCP CLOSE user command</a>)
   * the connection for the specified {@code socket}
   * and closes the {@code socket}.
   */
  private static final void close(final Socket socket) {
    try (socket) {
      log("Gracefully closing " + socket);
      socket.setSoLinger(false, -1);
    } catch (final IOException | RuntimeException e) {
      log(printStackTraceToString(e));
    } finally {
      log("Disconnected " + socket);
    }
  }
}
