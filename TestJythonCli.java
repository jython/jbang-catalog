
///usr/bin/env jbang "$0" "$@" ; exit $?

//SOURCES JythonCli.java

//DEPS org.tomlj:tomlj:1.1.1
//DEPS org.junit.jupiter:junit-jupiter:5.13.3
//DEPS org.junit.platform:junit-platform-console:1.13.3

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import org.junit.platform.console.ConsoleLauncher;

/**
 * A class to run tests on aspects of {@link JythonCli} by delegating to the
 * JUnit console {@link ConsoleLauncher}.
 */
public class TestJythonCli {

    static final String[] ARGS_DEBUG_FOO = {"--cli-debug", "foo.py", "bar", "baz"};
    static final String[] ARGS_FOO = {"--version", "foo.py", "bar.py", "baz"};

    /** The {@code --debug-cli} flag is spotted */
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

    /** Argumnents to the Jython command are assembled in order. */
    @Test
    void testJythonArgs() throws IOException {
        JythonCli cli = new JythonCli();
        cli.initEnvironment(ARGS_FOO);
        assertEquals("--version", cli.jythonArgs.get(0));
        assertEquals("foo.py", cli.jythonArgs.get(1));
        assertEquals("bar.py", cli.jythonArgs.get(2));
        assertEquals("baz", cli.jythonArgs.get(3));
    }

    /**
     * Run the JUnit console with arguments from our command line.
     *
     * @param args to pass to the JUnit console
     */
    public static void main(String[] args) {
        // Run the JUnit console
        ConsoleLauncher.main(args);
    }
}
