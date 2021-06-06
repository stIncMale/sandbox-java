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
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import static jdk.net.ExtendedSocketOptions.TCP_KEEPIDLE;
import static jdk.net.ExtendedSocketOptions.TCP_KEEPINTERVAL;

/**
 * A TCP server that implements a trivial protocol mostly compliant with
 * the echo protocol specified by the
 * <a href="https://www.rfc-editor.org/rfc/rfc862.html">RFC 862</a>.
 * The peculiarities of the protocol compared to the echo protocol are:
 * <ol>
 *   <li>
 *     Each byte is treated as a separate message.
 *   </li>
 *   <li>
 *     The following two bytes have special meaning:
 *     <ul>
 *       <li>
 *         0x68 (0b1101000 or 104 in decimal notation, represents LATIN SMALL LETTER H in both
 *         <a href="https://www.rfc-editor.org/rfc/rfc20">US-ASCII</a>
 *         and <a href="https://www.rfc-editor.org/rfc/rfc3629">UTF-8</a>)
 *         is a {@code hello} message;
 *       </li>
 *       <li>
 *         0x62 (0b1100010 or 98 in decimal notation, represents LATIN SMALL LETTER B)
 *         is a {@code bye} message.
 *       </li>
 *     </ul>
 *   </li>
 *   <li>
 *     The first message sent by a client after connecting must be {@code hello}.
 *     This is the only part not compliant with the echo protocol.
 *   </li>
 *   <li>
 *     When a client decides to disconnect, it must send the {@code bye} message.
 *     The server replies by sending the same message back,
 *     and upon receiving it the client must gracefully close
 *     (see <a href="https://www.rfc-editor.org/rfc/rfc793.html#section-3.8">
 *     TCP CLOSE user command</a>) the connection.
 *   </li>
 * </ol>
 * <p>
 * Server-side connection termination:
 * <ul>
 *   <li>
 *     If the first message sent by a client is not {@code hello},
 *     then the client is considered not well-behaved and the server forcefully closes
 *     (see <a href="https://www.rfc-editor.org/rfc/rfc793.html#section-3.8">
 *     TCP ABORT user command</a>) the connection.
 *   </li>
 *   <li>
 *     If the server does not receive any data from a client within server's read timeout,
 *     then the server forcefully closes
 *     (see <a href="https://www.rfc-editor.org/rfc/rfc793.html#section-3.8">
 *     TCP ABORT user command</a>) the connection.
 *   </li>
 *   <li>
 *     When the server detects that a client has closed the connection, it gracefully closes
 *     (see <a href="https://www.rfc-editor.org/rfc/rfc793.html#section-3.8">
 *     TCP CLOSE user command</a>) the connection.
 *     Because a well-behaved client initiates the process of closing the connection,
 *     its socket ends up in the
 *     <a href="https://www.rfc-editor.org/rfc/rfc793.html#section-3.2">TIME-WAIT state</a>,
 *     while the corresponding socket on the server side ends up in the fictional
 *     <a href="https://www.rfc-editor.org/rfc/rfc793.html#section-3.2">CLOSED state</a>,
 *     thus promptly releasing resources.
 *     This way we prevent accumulation of sockets in the TIME-WAIT state on the server side.
 *   </li>
 * </ul>
 */
final class Server {
    private static final byte HELLO = (byte)0x68;// U+0068, LATIN SMALL LETTER H
    static final byte BYE = (byte)0x62;// U+0062, LATIN SMALL LETTER B
    static final int TCP_KEEP_ALIVE_IDLE_SECONDS = 5;
    static final int SO_READ_TIMEOUT_MILLIS =
            Math.toIntExact(TimeUnit.SECONDS.toMillis(5 * TCP_KEEP_ALIVE_IDLE_SECONDS));

    public static final void main(final String... args) throws IOException {
        final InetSocketAddress serverSocketAddress = parseCliArgs(args);
        final int acceptTimeoutMillis = 0;// infinitely wait for new incoming connections
        final ExecutorService executor =
                Executors.newCachedThreadPool(new NamingThreadFactory("server"));
        log("Starting listening on " + serverSocketAddress);
        try (ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.bind(serverSocketAddress);
            serverSocket.setSoTimeout(acceptTimeoutMillis);
            log("Accepting connections on " + serverSocket);
            // noinspection InfiniteLoopStatement
            while (true) {
                final Socket clientSocket = serverSocket.accept();
                boolean successfullyAccepted = false;
                try {
                    log("Accepted a new connection " + clientSocket);
                    enableTcpKeepAlive(clientSocket, TCP_KEEP_ALIVE_IDLE_SECONDS);
                    executor.submit(() -> {
                        try {
                            serve(clientSocket, SO_READ_TIMEOUT_MILLIS);
                        } catch (final RuntimeException | IOException e) {
                            log(String.format(Locale.ROOT, "Exception when serving %s. %s",
                                    clientSocket, printStackTraceToString(e)));
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
     * Forcefully closes (see
     * <a href="https://www.rfc-editor.org/rfc/rfc793.html#section-3.8">TCP ABORT user command</a>)
     * the connection for the specified {@code socket} and closes the {@code socket}.
     */
    private static final void abort(final Socket socket) {
        try (socket) {
            log("Forcefully closing " + socket);
            socket.setSoLinger(true, 0);// close forcefully with TCP RST
        } catch (final RuntimeException | IOException e) {
            log(printStackTraceToString(e));
        } finally {
            log("Disconnected " + socket);
        }
    }

    /**
     * Gracefully closes (see
     * <a href="https://www.rfc-editor.org/rfc/rfc793.html#section-3.8">TCP CLOSE user command</a>)
     * the connection for the specified {@code socket}
     * and closes the {@code socket}.
     */
    static final void close(final Socket socket) {
        try (socket) {
            log("Gracefully closing " + socket);
            socket.setSoLinger(false, -1);
        } catch (final RuntimeException | IOException e) {
            log(printStackTraceToString(e));
        } finally {
            log("Disconnected " + socket);
        }
    }

    private static final void enableTcpKeepAlive(
            final Socket socket, final int tcpKeepAliveIdleSeconds) throws IOException {
        socket.setKeepAlive(true);
        final long tcpKeepAliveIdleMillis = TimeUnit.SECONDS.toMillis(tcpKeepAliveIdleSeconds);
        final Set<SocketOption<?>> supportedOptions = socket.supportedOptions();
        if (supportedOptions.contains(TCP_KEEPIDLE)) {
            socket.setOption(TCP_KEEPIDLE, tcpKeepAliveIdleSeconds);
            log("Set " + TCP_KEEPIDLE + " " + tcpKeepAliveIdleMillis + " ms for " + socket);
        } else {
            log(TCP_KEEPIDLE + " is not supported for " + socket);
        }
        /* The documentation of TCP_KEEPINTERVAL does not seem to match the actual behavior.
         * At least in Linux it specifies the interval between all probes but the first one.
         * This actual behavior matches the one specified for TCP_KEEPINTVL
         * (see https://man7.org/linux/man-pages/man7/tcp.7.html). */
        if (supportedOptions.contains(TCP_KEEPINTERVAL)) {
            socket.setOption(TCP_KEEPINTERVAL, tcpKeepAliveIdleSeconds);
            log("Set " + TCP_KEEPINTERVAL + " " + tcpKeepAliveIdleMillis + " ms for " + socket);
        } else {
            log(TCP_KEEPINTERVAL + " is not supported for " + socket);
        }
    }

    private static final void serve(
            final Socket clientSocket, final int readTimeoutMillis) throws IOException {
        Boolean wellBehavedClient = null;
        boolean clientDisconnected = false;
        try {
            clientSocket.setSoTimeout(readTimeoutMillis);
            log("Set read timeout " + readTimeoutMillis + " ms for " + clientSocket);
            final InputStream in = clientSocket.getInputStream();
            final OutputStream out = clientSocket.getOutputStream();
            final byte[] inData = new byte[1];
            byte inMessage;
            do {
                int receivedLength = in.read(inData);
                if (receivedLength > 0) {
                    inMessage = inData[0];
                    if (wellBehavedClient == null) {
                        wellBehavedClient = inMessage == HELLO;
                    }
                    if (wellBehavedClient) {
                        processInMessage(inMessage, out, clientSocket.toString());
                    }
                } else if (receivedLength == -1) {
                    clientDisconnected = true;
                }
            } while (!clientDisconnected && (wellBehavedClient == null || wellBehavedClient));
        } finally {
            try {
                if (clientDisconnected) {
                    log("The client " + clientSocket + " disconnected");
                } else if (wellBehavedClient != null && !wellBehavedClient) {
                    log("The client " + clientSocket + " is not well-behaved");
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

    private static final void processInMessage(
            final byte message, final OutputStream out, final String connectionDescription)
            throws IOException {
        log("Received " + toUnsignedHexString(message) + " via " + connectionDescription);
        sendOutMessage(message, out, connectionDescription);
    }

    private static final void sendOutMessage(
            final byte message, final OutputStream out, final String connectionDescription)
            throws IOException {
        log("Sending " + toUnsignedHexString(message) + " via " + connectionDescription);
        out.write(message);
        out.flush();
    }

    static final String printStackTraceToString(final Exception t) {
        try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
            t.printStackTrace(pw);
            pw.flush();
            return sw.toString();
        } catch (final IOException e) {
            throw new AssertionError(e);
        }
    }

    static final void log(final Object msg) {
        System.err.printf(Locale.ROOT, "%-30s %-9.9s %s%n", DateTimeFormatter.ISO_INSTANT.format(
                Instant.now()), Thread.currentThread().getName(), msg);
    }

    static final String toUnsignedHexString(final byte b) {
        final int unsignedValue = Byte.toUnsignedInt(b);
        return String.format(Locale.ROOT, "0x%02x '%s'",
                unsignedValue, Character.getName(unsignedValue));
    }

    static final InetSocketAddress parseCliArgs(final String... args) {
        return new InetSocketAddress(args[0], Integer.parseInt(args[1]));
    }

    private Server() {
        throw new AssertionError();
    }

    private static final class NamingThreadFactory implements ThreadFactory {
        private final String name;
        private final AtomicLong counter;

        private NamingThreadFactory(final String name) {
            this.name = name;
            counter = new AtomicLong();
        }

        @Override
        public Thread newThread(Runnable r) {
            final Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setName(name + "-" + counter.getAndIncrement());
            return t;
        }
    }
}
