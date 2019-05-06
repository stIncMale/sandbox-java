package stincmale.sandbox.examples.makeappbehaviorconsistent;

import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.TimeZone;
import java.util.stream.Collectors;

final class ConsistentAppExample {
  static {
    {//do the application-wide setup in order to render the behaviour consistent in different environments
      Locale.setDefault(Locale.ENGLISH);
      TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.from(ZoneOffset.UTC)));
      /*
       * Set UTF-8 charset for the standard out/err PrintStreams.
       * The standard in is an InputStream and the concept of charset is not directly applicable to it.
       */
      System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
      System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));
    }
  }

  /**
   * Run, for example, as
   * <pre>{@code
   * java \
   * -Dline.separator=$'\n' \
   * -Dfile.encoding=UTF-8 \
   * stincmale.sandbox.examples.makeappbehaviorconsistent.ConsistentAppExample
   * }</pre>
   * <ul>
   * <li>{@code line.separator} is a standard Java system property (see {@link System#getProperties()} and {@link System#lineSeparator()}).
   * See <a href="https://www.gnu.org/software/bash/manual/bash.html#ANSI_002dC-Quoting">Bash ANSI-C Quoting</a>
   * to clarify the {@code $'\n'} syntax.</li>
   * <li>{@code file.encoding} is not a standard Java system property, but it is used by some parts of OpenJDK JDK
   * (e.g. it defines the {@link Charset#defaultCharset()} in OpenJDK JDK 12) and therefore likely by all other JDKs.</li>
   * </ul>
   * Be careful when starting this application from an IDE: it most likely do not interpret the {@code $'\n'} correctly.
   */
  public static final void main(final String... args) {
    System.out.print(String.format("JVM-wide defaults: charset=%s, locale=%s, time zone=%s, line separator={%s}",
        Charset.defaultCharset(),
        Locale.getDefault().toLanguageTag(),
        TimeZone.getDefault().getID(),
        System.lineSeparator().codePoints().mapToObj(Character::getName).collect(Collectors.joining(", "))));
    System.out.print("\n");
  }
}
