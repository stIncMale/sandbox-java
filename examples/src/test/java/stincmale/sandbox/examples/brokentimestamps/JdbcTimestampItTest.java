package stincmale.sandbox.examples.brokentimestamps;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.JDBCType;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.TimeZone;
import javax.annotation.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.opentest4j.AssertionFailedError;

/**
 * This test class illustrates the problem with {@link JDBCType#TIMESTAMP} (without timezone)
 * described in <a href="https://www.kovalenko.link/blog/jdbc-timestamp-pitfalls">
 * Pitfalls with JDBC PreparedStatement.setTimestamp/ResultSet.getTimestamp</a>.
 * <p>
 * You may see that regardless of the {@link TimeZone} craziness happening,
 * storing and reading data of type {@link JDBCType#TIMESTAMP_WITH_TIMEZONE} never has issues,
 * while {@link JDBCType#TIMESTAMP} does
 * (see {@link Assertions#assertThrows(Class, Executable)}).</p>
 */
final class JdbcTimestampItTest {
    private static final TimeZone ORIGINAL_TZ = TimeZone.getDefault();
    private static final TimeZone TZ_1 =
            TimeZone.getTimeZone(ZoneId.from(ZoneOffset.ofHoursMinutes(0, 1)));
    private static final TimeZone TZ_2 =
            TimeZone.getTimeZone(ZoneId.from(ZoneOffset.ofHoursMinutes(0, 2)));
    private static final TimeZone TZ_3 =
            TimeZone.getTimeZone(ZoneId.from(ZoneOffset.ofHoursMinutes(0, 3)));
    private static final TimeZone TZ_4 =
            TimeZone.getTimeZone(ZoneId.from(ZoneOffset.ofHoursMinutes(0, 4)));
    private static final Timestamp TIMESTAMP = new Timestamp(1000_000_000_000L);
    private static final Instant INSTANT = TIMESTAMP.toInstant();

    private JdbcTimestampItTest() {
    }

    @AfterAll
    static final void afterClass() {
        TimeZone.setDefault(ORIGINAL_TZ);
    }

    @BeforeEach
    final void before() {
        doWithConnection(connection -> {
            try (var statement = connection.createStatement()) {
                statement.addBatch("drop table if exists ts_test");
                statement.addBatch("create table ts_test ("
                        + "tsz timestamp with time zone, "
                        // without time zone
                        + "ts timestamp)");
                statement.executeBatch();
            }
        });
    }

    @AfterEach
    final void after() {
        doWithConnection(connection -> {
            try (var statement = connection.prepareStatement("drop table if exists ts_test")) {
                statement.execute();
            }
        });
    }

    @Test
    final void insertSelect_sameTzImplicit_preJdbc42() {
        insert_preJdbc42(TIMESTAMP, null);
        final Timestamps read = select_preJdbc42(null);
        assertEquals(TIMESTAMP, read.tsz);
        assertEquals(TIMESTAMP, read.ts);
    }

    @Test
    final void insertSelect_sameTzExplicit_preJdbc42() {
        TimeZone.setDefault(TZ_1);
        insert_preJdbc42(TIMESTAMP, TZ_3);
        TimeZone.setDefault(TZ_2);
        final Timestamps read = select_preJdbc42(TZ_3);
        assertEquals(TIMESTAMP, read.tsz);
        assertEquals(TIMESTAMP, read.ts);
    }

    @Test
    final void insertSelect_differentTzImplicit_preJdbc42() {
        TimeZone.setDefault(TZ_1);
        insert_preJdbc42(TIMESTAMP, null);
        TimeZone.setDefault(TZ_2);
        final Timestamps read = select_preJdbc42(null);
        assertEquals(TIMESTAMP, read.tsz);
        assertThrows(AssertionFailedError.class, () -> assertEquals(TIMESTAMP, read.ts));
    }

    @Test
    final void insertSelect_differentTzExplicit_preJdbc42() {
        TimeZone.setDefault(TZ_3);
        insert_preJdbc42(TIMESTAMP, TZ_1);
        TimeZone.setDefault(TZ_4);
        final Timestamps read = select_preJdbc42(TZ_2);
        assertEquals(TIMESTAMP, read.tsz);
        assertThrows(AssertionFailedError.class, () -> assertEquals(TIMESTAMP, read.ts));
    }

    @Test
    final void insertSelect_sameTzExplicit_postJdbc42() {
        TimeZone.setDefault(TZ_1);
        insert_postJdbc42(INSTANT, TZ_3);
        TimeZone.setDefault(TZ_2);
        final Instants read = select_postJdbc42(TZ_3);
        assertEquals(INSTANT, read.itz);
        assertEquals(INSTANT, read.it);
    }

    @Test
    final void insertSelect_differentTzExplicit_postJdbc42() {
        TimeZone.setDefault(TZ_3);
        insert_postJdbc42(INSTANT, TZ_1);
        TimeZone.setDefault(TZ_4);
        final Instants read = select_postJdbc42(TZ_2);
        assertEquals(INSTANT, read.itz);
        assertThrows(AssertionFailedError.class, () -> assertEquals(INSTANT, read.it));
    }

    private final void insert_preJdbc42(final Timestamp timestamp, @Nullable final TimeZone tz) {
        doWithConnection(connection -> {
            try (var statement = connection.prepareStatement(
                    "insert into ts_test (tsz, ts) values (?, ?)")) {
                if (tz == null) {
                    // do not store timestamps like this
                    statement.setObject(1, timestamp);
                    statement.setObject(2, timestamp);
                } else {
                    // better do this
                    final Calendar calendar = Calendar.getInstance(tz);
                    statement.setTimestamp(1, timestamp, calendar);
                    statement.setTimestamp(2, timestamp, calendar);
                }
                statement.executeUpdate();
            }
        });
    }

    private final void insert_postJdbc42(final Instant instant, final TimeZone tz) {
        doWithConnection(connection -> {
            try (var statement = connection.prepareStatement(
                    "insert into ts_test (tsz, ts) values (?, ?)")) {
                final ZoneId zoneId = tz.toZoneId();
                final OffsetDateTime offsetDateTime = OffsetDateTime.ofInstant(instant, zoneId);
                final LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, zoneId);
                /* JDBC 4.2 (see https://jcp.org/en/jsr/detail?id=221,
                 * https://jcp.org/aboutJava/communityprocess/mrel/jsr221/index2.html)
                 * maps LocalDateTime to JDBCType.TIMESTAMP
                 * and OffsetDateTime to JDBCType.TIMESTAMP_WITH_TIMEZONE. */
                statement.setObject(1, offsetDateTime);
                statement.setObject(2, localDateTime);
                statement.executeUpdate();
            }
        });
    }

    private final Timestamps select_preJdbc42(@Nullable final TimeZone tz) {
        return returnWithConnection(connection -> {
            try (var statement = connection.prepareStatement("select tsz, ts from ts_test");
                    var rs = statement.executeQuery()) {
                rs.next();
                final Timestamp tsz;
                final Timestamp ts;
                if (tz == null) {
                    // do not read timestamps like this
                    tsz = rs.getTimestamp(1);
                    ts = rs.getTimestamp(2);
                } else {
                    // better do this
                    final Calendar calendar = Calendar.getInstance(tz);
                    tsz = rs.getTimestamp(1, calendar);
                    ts = rs.getTimestamp(2, calendar);
                }
                return new Timestamps(tsz, ts);
            }
        });
    }

    private final Instants select_postJdbc42(final TimeZone tz) {
        return returnWithConnection(connection -> {
            try (var statement = connection.prepareStatement("select tsz, ts from ts_test");
                    var rs = statement.executeQuery()) {
                rs.next();
                final OffsetDateTime offsetDateTime = rs.getObject(1, OffsetDateTime.class);
                final LocalDateTime localDateTime = rs.getObject(2, LocalDateTime.class);
                final Instant itz = offsetDateTime.toInstant();
                final Instant it = localDateTime.toInstant(
                        tz.toZoneId().getRules().getOffset(localDateTime));
                return new Instants(itz, it);
            }
        });
    }

    @Nullable
    private final <R> R returnWithConnection(final JdbcAction<? extends R> action) {
        try (var connection = connection()) {
            try {
                return action.call(connection);
            } catch (final Exception e) {
                throw new RuntimeException(e);
            } finally {
                connection.commit();
            }
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private final void doWithConnection(final JdbcVoidAction action) {
        returnWithConnection(connection -> {
            action.call(connection);
            return null;
        });
    }

    /**
     * Obtains a {@link Connection}.
     *
     * @return A {@link Connection} with {@linkplain Connection#getAutoCommit() auto-commit}
     * disabled.
     */
    private final Connection connection() {
        try {
            final Connection connection = DriverManager.getConnection(
                    "jdbc:postgresql://localhost/postgres?user=postgres&password=");
            connection.setAutoCommit(false);
            return connection;
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private interface JdbcAction<R> {
        @Nullable
        R call(Connection connection) throws Exception;
    }

    private interface JdbcVoidAction {
        void call(Connection connection) throws Exception;
    }

    private record Timestamps(Timestamp tsz, Timestamp ts) {
    }

    private record Instants(Instant itz, Instant it) {
    }
}
