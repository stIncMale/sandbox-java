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
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

/**
 * This test illustrates the problem with {@link JDBCType#TIMESTAMP} (without timezone) described in
 * <a href="https://www.kovalenko.link/blog/jdbc-timestamp-pitfalls">Pitfalls with JDBC PreparedStatement.setTimestamp/ResultSet.getTimestamp</a>.
 * <p>
 * You may see that regardless of the {@link TimeZone} craziness going on around,
 * storing and reading data of type {@link JDBCType#TIMESTAMP_WITH_TIMEZONE} never has issues,
 * while {@link JDBCType#TIMESTAMP} does.
 */
@Disabled("there are tests that fail intentionally, those are marked with //fails")
@TestInstance(Lifecycle.PER_METHOD)
final class JdbcTimestampTest {
  private static final TimeZone originalTz = TimeZone.getDefault();
  private static final TimeZone tz1 = TimeZone.getTimeZone(ZoneId.from(ZoneOffset.ofHoursMinutes(0, 1)));
  private static final TimeZone tz2 = TimeZone.getTimeZone(ZoneId.from(ZoneOffset.ofHoursMinutes(0, 2)));
  private static final TimeZone tz3 = TimeZone.getTimeZone(ZoneId.from(ZoneOffset.ofHoursMinutes(0, 3)));
  private static final TimeZone tz4 = TimeZone.getTimeZone(ZoneId.from(ZoneOffset.ofHoursMinutes(0, 4)));
  private static final Timestamp ts = new Timestamp(1000_000_000_000L);
  private static final Instant it = ts.toInstant();

  private JdbcTimestampTest() {
  }

  @AfterAll
  static final void afterClass() {
    TimeZone.setDefault(originalTz);
  }

  @BeforeEach
  final void before() {
    doWithConnection(connection -> {
      try (var statement = connection.createStatement()) {
        statement.addBatch("drop table if exists ts_test");
        statement.addBatch("create table ts_test (\n" +
            "tsz timestamp with time zone,\n" +
            "ts timestamp)");//without time zone
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
    insert_preJdbc42(ts, null);
    final Timestamps read = select_preJdbc42(null);
    assertEquals(ts, read.tsz);
    assertEquals(ts, read.ts);
  }

  @Test
  final void insertSelect_sameTzExplicit_preJdbc42() {
    TimeZone.setDefault(tz1);
    insert_preJdbc42(ts, tz3);
    TimeZone.setDefault(tz2);
    final Timestamps read = select_preJdbc42(tz3);
    assertEquals(ts, read.tsz);
    assertEquals(ts, read.ts);
  }

  @Test
  final void insertSelect_differentTzImplicit_preJdbc42() {
    TimeZone.setDefault(tz1);
    insert_preJdbc42(ts, null);
    TimeZone.setDefault(tz2);
    final Timestamps read = select_preJdbc42(null);
    assertEquals(ts, read.tsz);
    assertEquals(ts, read.ts);//fails
  }

  @Test
  final void insertSelect_differentTzExplicit_preJdbc42() {
    TimeZone.setDefault(tz3);
    insert_preJdbc42(ts, tz1);
    TimeZone.setDefault(tz4);
    final Timestamps read = select_preJdbc42(tz2);
    assertEquals(ts, read.tsz);
    assertEquals(ts, read.ts);//fails
  }

  @Test
  final void insertSelect_sameTzExplicit_postJdbc42() {
    TimeZone.setDefault(tz1);
    insert_postJdbc42(it, tz3);
    TimeZone.setDefault(tz2);
    final Instants read = select_postJdbc42(tz3);
    assertEquals(it, read.itz);
    assertEquals(it, read.it);
  }

  @Test
  final void insertSelect_differentTzExplicit_postJdbc42() {
    TimeZone.setDefault(tz3);
    insert_postJdbc42(it, tz1);
    TimeZone.setDefault(tz4);
    final Instants read = select_postJdbc42(tz2);
    assertEquals(it, read.itz);
    assertEquals(it, read.it);//fails
  }

  private final void insert_preJdbc42(final Timestamp timestamp, @Nullable final TimeZone tz) {
    doWithConnection(connection -> {
      try (var statement = connection.prepareStatement("insert into ts_test (tsz, ts) values (?, ?)")) {
        if (tz == null) {//don't store timestamps like this
          statement.setObject(1, timestamp);
          statement.setObject(2, timestamp);
        } else {//better do this
          Calendar calendar = Calendar.getInstance(tz);
          statement.setTimestamp(1, timestamp, calendar);
          statement.setTimestamp(2, timestamp, calendar);
        }
        statement.executeUpdate();
      }
    });
  }

  private final void insert_postJdbc42(final Instant instant, final TimeZone tz) {
    doWithConnection(connection -> {
      try (var statement = connection.prepareStatement("insert into ts_test (tsz, ts) values (?, ?)")) {
        final ZoneId zoneId = tz.toZoneId();
        final OffsetDateTime offsetDateTime = OffsetDateTime.ofInstant(instant, zoneId);
        final LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, zoneId);
        /*
         * JDBC 4.2 (see https://jcp.org/en/jsr/detail?id=221, https://jcp.org/aboutJava/communityprocess/mrel/jsr221/index2.html)
         * maps LocalDateTime to JDBCType.TIMESTAMP and OffsetDateTime to JDBCType.TIMESTAMP_WITH_TIMEZONE
         */
        statement.setObject(1, offsetDateTime);
        statement.setObject(2, localDateTime);
        statement.executeUpdate();
      }
    });
  }

  private final Timestamps select_preJdbc42(@Nullable final TimeZone tz) {
    return returnWithConnection(connection -> {
      try (var statement = connection.prepareStatement("select tsz, ts from ts_test"); var rs = statement.executeQuery()) {
        rs.next();
        final Timestamp tsz;
        final Timestamp ts;
        if (tz == null) {//don't read timestamps like this
          tsz = rs.getTimestamp(1);
          ts = rs.getTimestamp(2);
        } else {//better do this
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
      try (var statement = connection.prepareStatement("select tsz, ts from ts_test"); var rs = statement.executeQuery()) {
        rs.next();
        final OffsetDateTime offsetDateTime = rs.getObject(1, OffsetDateTime.class);
        final LocalDateTime localDateTime = rs.getObject(2, LocalDateTime.class);
        final Instant itz = offsetDateTime.toInstant();
        final Instant it = localDateTime.toInstant(tz.toZoneId().getRules().getOffset(localDateTime));
        return new Instants(itz, it);
      }
    });
  }

  @Nullable
  private final <R> R returnWithConnection(final JdbcAction<? extends R> action) {
    try (var connection = getConnection()) {
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
   * @return A {@link Connection} with {@linkplain Connection#getAutoCommit() auto-commit} disabled.
   */
  private final Connection getConnection() {
    try {
      //specify your DB URL here, perhaps add the JDBC driver dependency to pom.xml
      final Connection connection = DriverManager.getConnection("jdbc:postgresql://localhost/postgres?user=postgres&password=");
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

  private static final class Timestamps {
    private final Timestamp tsz;
    private final Timestamp ts;

    private Timestamps(final Timestamp tsz, final Timestamp ts) {
      this.tsz = tsz;
      this.ts = ts;
    }
  }

  private static final class Instants {
    private final Instant itz;
    private final Instant it;

    private Instants(final Instant itz, final Instant it) {
      this.itz = itz;
      this.it = it;
    }
  }
}
