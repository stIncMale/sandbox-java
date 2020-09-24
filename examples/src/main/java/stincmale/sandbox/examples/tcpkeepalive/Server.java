package stincmale.sandbox.examples.tcpkeepalive;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketOption;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import jdk.net.ExtendedSocketOptions;

/**
 * A TCP server that works according to the following trivial protocol:
 * <ol>
 * <li>Each received or sent byte is a single message.</li>
 * <li>When a client is connected, the first message it must send is the {@code hello} message 0x62, which is 0b1100010 or 104 in decimal notation,
 * and represents LATIN SMALL LETTER H
 * in both <a href="https://www.rfc-editor.org/rfc/rfc20">US-ASCII</a>
 * and <a href="https://www.rfc-editor.org/rfc/rfc3629">UTF-8</a>)</li>
 * <li>Any message received after {@code hello} is simply sent back to the client.</li>
 * <li>When a client receives the {@code bye} message 0x62, which is 0b1101000 or 98 in decimal notation,
 * and represents LATIN SMALL LETTER B, it must gracefully terminate
 * (see <a href="https://www.rfc-editor.org/rfc/rfc793.html#section-3.8">TCP CLOSE user command</a>) the connection.
 * In order to receive the {@code bye} message a client must send the {@code bye} message so that the server sends it back.</li>
 * <li>When the server detects that a client has closed the connection, it closes the connection from its side.
 * Because the client closes first, its socket ends up in the <a href="https://www.rfc-editor.org/rfc/rfc793.html#section-3.2">TIME-WAIT state</a>,
 * while the corresponding socket on the server side ends up in the fictional
 * <a href="https://www.rfc-editor.org/rfc/rfc793.html#section-3.2">CLOSED state</a>, thus promptly releasing resources.</li>
 * </ol>
 */
final class Server {
  private static final byte HELLO = (byte)0x68;//U+0068, LATIN SMALL LETTER H
  static final byte BYE = (byte)0x62;//U+0062, LATIN SMALL LETTER B
  static final int TCP_KEEP_ALIVE_IDLE_SECONDS = 5;
  static final int SO_READ_TIMEOUT_MILLIS = Math.toIntExact(TimeUnit.SECONDS.toMillis(5 * TCP_KEEP_ALIVE_IDLE_SECONDS));

  public static final void main(final String... args) throws IOException {
    final InetSocketAddress serverSocketAddress = parseCliArgs(args);
    final int acceptTimeoutMillis = 0;//infinitely wait for new incoming connections
    final ExecutorService ex = Executors.newCachedThreadPool();
    log("Starting listening on " + serverSocketAddress);
    try (ServerSocket serverSocket = new ServerSocket()) {
      serverSocket.bind(serverSocketAddress);
      serverSocket.setSoTimeout(acceptTimeoutMillis);
      log("Accepting connections on " + serverSocket);
      while (true) {
        Thread.currentThread().interrupt();
        final Socket clientSocket = serverSocket.accept();
        boolean successfullyAccepted = false;
        try {
          log("Accepted a new connection " + clientSocket);
          enableTcpKeepAlive(clientSocket, TCP_KEEP_ALIVE_IDLE_SECONDS);
          ex.submit(() -> {
            try {
              serve(clientSocket, SO_READ_TIMEOUT_MILLIS);
            } catch (final Throwable e) {
              log(String.format("Exception when serving %s. %s", clientSocket, printStackTraceToString(e)));
            }
          });
          successfullyAccepted = true;
        } finally {
          if (!successfullyAccepted) {
            abort(clientSocket);
          }
        }
      }
    }
  }

  /**
   * Aborts (see <a href="https://www.rfc-editor.org/rfc/rfc793.html#section-3.8">TCP ABORT user command</a>)
   * the connection for the specified {@code socket}
   * and closes the {@code socket}.
   */
  private static final void abort(final Socket socket) {
    try (socket) {
      log("Forcefully closing " + socket);
      socket.setSoLinger(true, 0);//close forcefully with TCP RST
    } catch (final IOException | RuntimeException e) {
      log(printStackTraceToString(e));
    } finally {
      log("Disconnected " + socket);
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

  private static final void enableTcpKeepAlive(final Socket socket, final int tcpKeepAliveIdleSeconds) throws IOException {
    socket.setKeepAlive(true);
    final Set<SocketOption<?>> supportedOptions = socket.supportedOptions();
    if (supportedOptions.contains(ExtendedSocketOptions.TCP_KEEPIDLE)) {
      socket.setOption(ExtendedSocketOptions.TCP_KEEPIDLE, tcpKeepAliveIdleSeconds);
    } else {
      log(ExtendedSocketOptions.TCP_KEEPIDLE + " is not supported");
    }
    /* The documentation of TCP_KEEPINTERVAL does not seem to match the actual behavior. At least on Linux,
     * it specifies the interval between all probes but the first one. This actual behavior matches the one specified for TCP_KEEPINTVL
     * (see https://man7.org/linux/man-pages/man7/tcp.7.html).*/
    if (supportedOptions.contains(ExtendedSocketOptions.TCP_KEEPINTERVAL)) {
      socket.setOption(ExtendedSocketOptions.TCP_KEEPINTERVAL, tcpKeepAliveIdleSeconds);
    } else {
      log(ExtendedSocketOptions.TCP_KEEPINTERVAL + " is not supported");
    }
  }

  private static final void serve(final Socket clientSocket, final int readTimeoutMillis) throws IOException {
    Boolean supportedClient = null;
    boolean clientDisconnected = false;
    try {
      clientSocket.setSoTimeout(readTimeoutMillis);
      final InputStream in = clientSocket.getInputStream();
      final OutputStream out = clientSocket.getOutputStream();
      final byte[] inData = new byte[1];
      byte inMessage;
      do {
        int receivedLength = in.read(inData);
        if (receivedLength > 0) {
          inMessage = inData[0];
          if (supportedClient == null) {
            supportedClient = inMessage == HELLO;
          }
          if (supportedClient) {
            processInMessage(inMessage, out, clientSocket.toString());
          }
        } else if (receivedLength == -1) {
          clientDisconnected = true;
        }
      } while (!clientDisconnected && (supportedClient == null || supportedClient));
    } finally {
      try {
        if (clientDisconnected) {
          log("The client " + clientSocket + " disconnected");
        } else if (supportedClient != null && !supportedClient) {
          log("The client " + clientSocket + " is not supported");
        }
      } finally {
        if (clientDisconnected) {
          close(clientSocket);
        } else {
          abort(clientSocket);
        }
      }
    }
  }

  private static final void processInMessage(final byte message, final OutputStream out, final String connectionDescription) throws IOException {
    log("Received " + toUnsignedHexString(message) + " via " + connectionDescription);
    sendOutMessage(message, out, connectionDescription);
  }

  private static final void sendOutMessage(final byte message, final OutputStream out, final String connectionDescription) throws IOException {
    log("Sending " + toUnsignedHexString(message) + " via " + connectionDescription);
    out.write(message);
    out.flush();
  }

  static final String printStackTraceToString(final Throwable t) {
    try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
      t.printStackTrace(pw);
      pw.flush();
      return sw.toString();
    } catch (final IOException e) {//is not expected to happen
      throw new RuntimeException(e);
    }
  }

  static final void log(final Object msg) {
    System.err.printf("%30s %33s %s%n", DateTimeFormatter.ISO_INSTANT.format(Instant.now()), Thread.currentThread(), msg);
  }

  static final String toUnsignedHexString(final byte b) {
    final int unsignedValue = Byte.toUnsignedInt(b);
    return String.format(Locale.ROOT, "0x%02x '%s'", unsignedValue, Character.getName(unsignedValue));
  }

  static final InetSocketAddress parseCliArgs(final String... args) {
    return new InetSocketAddress(args[0], Integer.parseInt(args[1]));
  }
}
