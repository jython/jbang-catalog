
///usr/bin/env jbang "$0" "$@" ; exit $?

//SOURCES JythonCli.java

//DEPS org.tomlj:tomlj:1.1.1
//DEPS org.junit.jupiter:junit-jupiter:5.13.3
//DEPS org.junit.platform:junit-platform-console:1.13.3

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringReader;

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
    static final String[] ARGS_NONE = {"--cli-debug"};

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

    /** Two {@code jbang} blocks is an error. */
    @Test
    void testTwoBlocks() throws IOException {
        JythonCli cli = new JythonCli();
        cli.initEnvironment(ARGS_NONE);
        Reader script = new StringReader("""
                # Valid but not for us
                # /// script
                # requires-python = ">=3.11"
                # dependencies = [
                #   "requests<3",
                #   "rich",
                # ]
                # ///

                # /// jbang
                # requires-jython = "2.7.2"
                # requires-java = "8"
                # ///

                # /// jbang
                # requires-jython = "2.7.3"
                # ///
         """);
        assertThrows(
            Exception.class,
            ()->cli.readJBangBlock(script)
        );
    }

    /** Invalid TOML is an error. */
    // Unfortunately, we doen't seem to notice
    @Test
    void testInvalidTOML() throws IOException {
        JythonCli cli = new JythonCli();
        cli.initEnvironment(ARGS_NONE);
        Reader script = new StringReader("""
                Good JBang block containing bad TOML
                # /// jbang
                # requires-java = "8"
                # stuff = {
                #   nonsense = 42
                #   Quatsch =::
                # }
                # ///
                print("Hello World!")
                """);
        assertThrows(
            Exception.class,
            ()->cli.readJBangBlock(script)
        );
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
