/// usr/bin/env jbang "$0" "$@" ; exit $?

//SOURCES JythonCli.java

//DEPS org.tomlj:tomlj:1.1.1
//DEPS org.junit.jupiter:junit-jupiter:5.13.3
//DEPS org.junit.platform:junit-platform-console:1.13.3

import java.io.IOException;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.platform.console.ConsoleLauncher;

/**
 * A class to run tests on aspects of {@link JythonCli} by delegating to
 * the JUnit console {@link ConsoleLauncher}.
 */
public class TestJythonCli {

    static final String[] ARGS_DEBUG_FOO =
            {"--cli-debug", "foo.py", "bar", "baz"};
    static final String[] ARGS_FOO =
            {"--version", "foo.py", "bar.py", "baz"};
    static final String[] ARGS_NONE = {"--cli-debug"};

    /** The {@code --cli-debug} flag is spotted */
    @Test
    void testCliDebugFlag() throws IOException {
        JythonCli cli = new JythonCli();
        cli.initEnvironment(ARGS_DEBUG_FOO);
        assertTrue(cli.debug);
    }

    /** A Python script argument is picked up. */
    @Test
    void testScriptFound() throws IOException {
        JythonCli cli = new JythonCli();
        cli.initEnvironment(ARGS_FOO);
        assertFalse(cli.debug);
        assertEquals("foo.py", cli.scriptFilename);
    }

    /** Arguments to the Jython command are assembled in order. */
    @Test
    void testJythonArgs() throws IOException {
        JythonCli cli = new JythonCli();
        cli.initEnvironment(ARGS_FOO);
        assertEquals("--version", cli.jythonArgs.get(0));
        assertEquals("foo.py", cli.jythonArgs.get(1));
        assertEquals("bar.py", cli.jythonArgs.get(2));
        assertEquals("baz", cli.jythonArgs.get(3));
    }

    /** An unterminated {@code jbang} block is an error. */
    @Test
    @Disabled("readJBangBlock does not throw on an unterminated block")
    void testUnterminated() throws IOException {
        String script = 
                """
                # /// jbang
                # requires-jython = "2.7.2"
                # requires-java = "17"
                import sys
                """;
        JythonCli cli = new JythonCli();
        assertThrows(Exception.class, () -> processScript(cli, script));
    }

    /**
     * An unterminated block may gobble up a {@code jbang} block. This
     * is not detectable by {@link JythonCli} as the text of a
     * {@code jbang} header could be legitimate content.
     */
    @Test
    @Disabled("readJBangBlock treats '/// jbang' inside another block as valid start")
    void testGobbledBlock() throws IOException {
        JythonCli cli = new JythonCli();
        processScript(cli,
                """
                # /// script
                # requires-python = ">=3.11"
                # /// jbang
                # requires-jython = "2.7.2"
                # requires-java = "8"
                # ///
                """);
        assertTrue(cli.tomlText.isEmpty(), "Check TOML text is empty");
        assertNull(cli.tpr, "Check TOML parse not done");
    }

    /**
     * An unterminated {@code jbang} block should gobble up a following
     * block. This ought to be detectable by {@link JythonCli}. It isn't
     * actually a fault with the block delimiting: but the gobbled
     * block-start is not valid TOML.
     */
    @Test
    @Disabled("interpretJBangBlock treats '/// script' as valid terminator")
    void testCollision() throws IOException {
        String script =
                """
                # /// jbang
                # requires-jython = "2.7.2"
                # requires-java = "8"
                # /// script
                # requires-python = ">=3.11"
                # ///
                """;
        JythonCli cli = new JythonCli();
        assertThrows(Exception.class, () -> processScript(cli, script));
        assertFalse(cli.tomlText.isEmpty(), "Detect TOML text is empty");
        assertTrue(cli.tpr.hasErrors(), "Check TOML parse reports errors");
    }

    /** Two {@code jbang} blocks is an error. */
    @Test
    @Disabled("readJBangBlock does not throw on a second jbang block")
    void testTwoBlocks() throws IOException {
        String script =
                """
                # /// jbang
                # requires-jython = "2.7.2"
                # requires-java = "8"
                # ///

                # Valid but not for us
                # /// script
                # requires-python = ">=3.11"
                # ///

                # /// jbang
                # requires-jython = "2.7.3"
                # ///
                """;
        JythonCli cli = new JythonCli();
        assertThrows(Exception.class, () -> processScript(cli, script));
    }

    /** Invalid TOML is an error. */
    @Test
    void testInvalidTOML() throws IOException {
        String script =
                """
                # /// jbang
                # requires-jython = "2.7.4"
                # requires-java = "21"
                # dependencies = [
                #   "io.leego:banana:2.1.0"
                # ]
                # runtime-options = [
                #   "-Dpython.console.encoding=UTF-8"
                # -
                # ///
                print("Hello World!")
                """;
        JythonCli cli = new JythonCli();
        assertThrows(Exception.class, () -> processScript(cli, script));
        assertTrue(cli.tpr.hasErrors(), "Check TOML parse reports errors");
    }

    /**
     * Take an initialised {@link JythonCli} and have it process (but
     * not run) the given {@code String} as if the contents of a file.
     *
     * @param cli to exercise
     * @param script to process as script
     * @throws IOException on StringReader errors
     */
    void processScript(JythonCli cli, String script) throws IOException {
        cli.initEnvironment(ARGS_NONE);
        cli.readJBangBlock(new StringReader(script));
        cli.interpretJBangBlock();
    }

    /**
     * Run the JUnit console with arguments from our command line.
     *
     * @param args to pass to the JUnit console
     */
    public static void main(String[] args) throws IOException {
        // Run the JUnit console
        if (true) {
            ConsoleLauncher.main(args);
        } else {
            // Debugging code
            new TestJythonCli().testInvalidTOML();
        }
    }
}
