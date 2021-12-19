package stincmale.sandbox.examples.makeappbehaviorconsistent;

import java.io.Console;
import java.io.IOException;
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
        {
            /* Do the application-wide setup in order to render the behavior consistent
             * in different environments. */
            TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.from(ZoneOffset.UTC)));
            Locale.setDefault(Locale.ENGLISH);
            /* Set UTF-8 charset for the stdout/stderr PrintStreams.
             * The stdin is an InputStream and the concept of charset
             * is not directly applicable to it. */
            System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
            System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));
        }
    }

    /**
     * Run, for example, as
     * <pre>{@code
     * $ java \
     * -Dline.separator=$'\n' \
     * -Dfile.encoding=UTF-8 \
     * ConsistentAppExample.java
     * }</pre>
     * in <a href="https://www.gnu.org/software/bash/">Bash</a>
     * <ul>
     * <li>{@code line.separator} is a standard Java system property,
     * see {@link System#getProperties()} and {@link System#lineSeparator()}.
     * See <a href="https://www.gnu.org/software/bash/manual/bash.html#ANSI_002dC-Quoting">
     * Bash ANSI-C Quoting</a> for the details about the {@code $'\n'} syntax.</li>
     * <li>{@code file.encoding} is not a standard Java system property,
     * but it is used by OpenJDK JDK, see
     * <a href="https://openjdk.java.net/jeps/400">JEP 400: UTF-8 by Default</a>
     * for more details. As of Java SE 18 there is no need to specify
     * {@code file.encoding=UTF-8} as that is the default.</li>
     * </ul>
     * Be careful when starting this application from an IDE,
     * it most likely does not understand the syntax {@code $'\n'}.
     */
    public static final void main(final String... args) throws IOException {
        final Console console = System.console();
        System.out.printf("charset=%s"
                        + ", console charset=%s"
                        + ", native charset=%s"
                        + ", locale=%s"
                        + ", time zone=%s"
                        + ", line separator={%s}",
                Charset.defaultCharset(),
                console == null ? "console unavailable" : console.charset(),
                System.getProperty("native.encoding"),
                Locale.getDefault().toLanguageTag(),
                TimeZone.getDefault().getID(),
                System.lineSeparator().codePoints().mapToObj(Character::getName)
                        .collect(Collectors.joining(", ")));
        System.out.println();
        System.out.println(
                "Charset smoke test: latin:english___cyrillic:русский___hangul:한국어___math:μ∞θℤ");
    }

    private ConsistentAppExample() {
        throw new AssertionError();
    }
}
