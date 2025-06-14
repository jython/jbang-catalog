
///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS org.tomlj:tomlj:1.1.1

import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.tomlj.Toml;
import org.tomlj.TomlParseResult;

public class JythonCli {

    /**
     * Default version of Jython to use.
     */
    String jythonVersion = "2.7.4";
    /**
     * Default version of Java to use as determined by the JVM version running
     * {@code jython-cli}. Only Java 8 or higher is supported.
     */
    String javaVersion = getJvmMajorVersion();
    /**
     * List of Maven Central JAR dependencies
     */
    List<String> deps = new ArrayList<>();
    /**
     * Java VM runtime options
     */
    List<String> ropts = new ArrayList<>();
    /**
     * (optional) TOML text block extracted from the Jython script specified on
     * the command-line
     */
    StringBuilder tomlText = new StringBuilder();
    /**
     * (optional) TOML parsed result object from which runtime information is
     * extracted
     */
    TomlParseResult tpr = null;
    /**
     * Debug flag that can be specified in the TOML configuration as
     * {@code debug = true} or {@code debug = false}. If set to true then at
     * runtime the arguments passed to ProcessBuilder is displayed before the
     * Jython process is started.
     */
    boolean debug = false;

    /**
     * Determine the major version number of the JVM {@code jython-cli} is
     * running on.
     *
     * @return the major version number of the current JVM, that is "8", "9",
     * "10", etc.
     */
    static String getJvmMajorVersion() {
        String version = System.getProperty("java.version");
        String major = "";
        if (version.startsWith("1.")) {
            major = version.substring(2, 3);
        } else {
            int dotIndex = version.indexOf(".");
            if (dotIndex != -1) {
                major = version.substring(0, dotIndex);
            } else {
                major = version;
            }
        }
        return major;
    }

    /**
     * Extract additional runtime options from the (optional) Jython script
     * specified on the command-line containing (optional) TOML data. The
     * runtime options that are extracted from the TOML data will override
     * default version specifications determined earlier.
     *
     * @param args program arguments as specified on the command-line
     * @throws IOException
     */
    void initEnvironment(String[] args) throws IOException {
        // Check that that Java 8 (1.8) or higher is used
        if (Integer.parseInt(javaVersion) < 8) {
            System.err.println("jython-cli: error, Java 8 or higher is required");
            System.exit(1);
        }

        // Determine the Jython script filename (if specified)
        String scriptFilename = "";
        for (String arg : args) {
            if (arg.endsWith(".py")) {
                scriptFilename = arg;
                break;
            }
        }

        // Extract TOML data as a String (if present)
        if (!scriptFilename.isEmpty()) {
            List<String> lines = Files.readAllLines(Paths.get(scriptFilename));
            boolean found = false;
            for (String line : lines) {
                if (found && !line.startsWith("# ")) {
                    found = false;
                    tomlText = new StringBuilder();
                }
                if (!found && line.startsWith("# /// jbang")) {
                    found = true;
                } else if (found && line.startsWith("# ///")) {
                    break;
                } else if (found && line.startsWith("# ")) {
                    if (tomlText.length() > 0) {
                        tomlText.append("\n");
                    }
                    tomlText.append(line.substring(2));
                }
            }
        }

        // Parse the TOML data
        if (tomlText.length() > 0) {
            tpr = Toml.parse(tomlText.toString());
        }

        // Process the TOML data
        if (tpr != null) {

            // requires-jython
            if (tpr.isString("requires-jython")) {
                jythonVersion = tpr.getString("requires-jython");
            }

            // requires-java
            if (tpr.isString("requires-java")) {
                javaVersion = tpr.getString("requires-java");
            }

            // dependencies
            for (Object e : tpr.getArrayOrEmpty("dependencies").toList()) {
                String dep = (String) e;
                deps.add(dep);
            }

            // runtime-options
            for (Object e : tpr.getArrayOrEmpty("runtime-options").toList()) {
                String ropt = (String) e;
                ropts.add(ropt);
            }

            // debug
            if (tpr.isBoolean("debug")) {
                debug = Boolean.TRUE.equals(tpr.getBoolean("debug"));
            }
        }
    }

    /**
     * Run the Jython jar using JBang passing along the required Maven
     * dependencies and JVM runtime options.
     *
     * @param args program arguments as specified on the command-line
     * @throws IOException
     * @throws InterruptedException
     */
    void runProcess(String[] args) throws IOException, InterruptedException {
        List<String> cmd = new LinkedList<>();
        boolean windows = System.getProperty("os.name").toLowerCase().startsWith("win");
        cmd.add("jbang" + (windows ? ".cmd" : ""));

        cmd.add("run");

        cmd.add("--java");
        cmd.add(javaVersion);

        for (String ropt : ropts) {
            cmd.add("--runtime-option");
            cmd.add(ropt);
        }

        for (String dep : deps) {
            cmd.add("--deps");
            cmd.add(dep);
        }

        cmd.add("--main");
        cmd.add("org.python.util.jython");

        cmd.add("org.python:jython-slim:" + jythonVersion);

        Collections.addAll(cmd, args);

        if (debug) {
            System.err.println("[jython-cli] " + cmd.toString());
        }

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.inheritIO();
        pb.start().waitFor();
    }

    /**
     * Main {@code jython-cli} (JythonCli.java) program. The arguments are exactly the same
     * command-line arguments Jython itself supports as documented in
     * <a href="https://www.jython.org/jython-old-sites/docs/using/cmdline.html">Jython
     * Command Line</a>
     *
     * @param args arguments to the program. 
     * @throws IOException
     * @throws InterruptedException
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        JythonCli jythonCli = new JythonCli();
        jythonCli.initEnvironment(args);
        jythonCli.runProcess(args);
    }
}
