///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS org.tomlj:tomlj:1.1.1

import java.io.*;
import java.nio.file.*;
import java.util.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.tomlj.Toml;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlParseError;

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
     * Jython arguments
     */
    List<String> jythonArgs = new ArrayList<>();
    /**
     * Jython script filename (if specified) or null if not.
     */
    String scriptFilename;
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
     * Debug output can be specified by the {@code --cli-debug} command line
     * option. If set, various critical state is displayed including the
     * {@code jbang} block, its meaning as TOML and the arguments passed to
     * ProcessBuilder.
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
     * Process the command line arguments, giving special tratment to the
     * {@code --cli-debug} option and the (optional) Jython script specified.
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

        for (String arg : args) {
            if (scriptFilename==null && arg.endsWith(".py")) {
                scriptFilename = arg;
                jythonArgs.add(arg);
            } else if ("--cli-debug".equals(arg)) {
                debug = true;
            } else {
                jythonArgs.add(arg);
            }
        }
    }

    /**
     * Read the jbang block from the Jython script specified on the command-line
     * containing (optional) and interpret it as TOML data. The runtime options
     * that are extracted from the TOML data will override default version
     * specifications determined earlier.
     *
     * @throws IOException
     */
    void readJBangBlock() throws IOException {
        boolean errorReportingEnabled = true;
        String fileText = Files.readAllLines(Paths.get(scriptFilename))
                .stream()
                .collect(Collectors.joining("\n"));
        String tomlRegex = "^# /// (?<type>[a-zA-Z0-9-]+)$\\s(?<content>(^#(| .*)$\\s)+)^# ///$";
        Pattern pattern = Pattern.compile(tomlRegex, Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(fileText);
        while (matcher.find()) {
            String type = matcher.group("type");
            if (type.equals("jbang")) {
                if (!(tomlText.length() == 0)) {  // Java 8
                    tomlText = new StringBuilder();
                    errorReportingEnabled = false;
                    System.err.println("[jython-cli] error - multiple jbang content blocks detected and discarded");
                    break;
                }
                String jbangComment = matcher.group("content");
                List<String> tomlLines = new ArrayList<>();
                for (String line : jbangComment.split("\n")) {
                    if (line.startsWith("# ")) {
                        tomlLines.add(line.substring(2));
                    } else {
                        tomlLines.add(line.substring(1));
                    }
                }
                tomlText = new StringBuilder(String.join("\n", tomlLines));
            }
        }
        if ((tomlText.length() == 0) && errorReportingEnabled) {  // Java 8
            if (fileText.contains("# /// jbang")) {
                System.err.println("[jython-cli] error - malformed jbang content block detected");
                boolean found = false;
                for (String line : fileText.split("\n")) {
                    line = line.trim();  // Java 8
                    if (line.equals("# /// jbang")) {
                        System.err.println(line);
                        found = true;
                        continue;
                    }
                    if (found) {
                        if (line.startsWith("#")) {
                            System.err.println(line);
                        } else {
                            break;
                        }
                    }
                }
                throw new IOException();
            }
        }
    }

    /**
     * Interpret the jbang block from the Jython script specified on the
     * command-line as TOML data. The runtime options that are extracted from
     * the TOML data will override default version specifications determined
     * earlier.
     *
     * @throws IOException
     */
    void interpretJBangBlock() throws IOException {

        if (tomlText.length() > 0) {
            int lineno = 0;
            for (String line: tomlText.toString().split("\\n", -1)) {
                lineno += 1;
                printIfDebug(lineno, line);
            }
            tpr = Toml.parse(tomlText.toString());
            if (tpr.hasErrors()) {
                for (TomlParseError err: tpr.errors()) {
                    System.err.println(err.toString());
                }
            }
            throw new IOException();
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
    void runProcess() throws IOException, InterruptedException {
        // Compose the launch command here
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

        cmd.addAll(jythonArgs);

        printIfDebug(cmd.toString());

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.inheritIO();
        pb.start().waitFor();
    }

    /**
     * Shorthand to print a line of the source jbang block if {@link #debug} is
     * set.
     *
     * @param lineno source line number
     * @param line text captured to {@link #tomlText}
     */
    void printIfDebug(int lineno, String line) {
        if (debug) {
            System.err.printf("{%3d} %s\n", lineno, line);
        }
    }

    /**
     * Shorthand to print something if {@link #debug} is set.
     *
     * @param text to print
     */
    void printIfDebug(String text) {
        if (debug) {
            System.err.println(text);
        }
    }

    /**
     * Main {@code jython-cli} (JythonCli.java) program. The arguments are
     * exactly the same command-line arguments Jython itself supports as
     * documented in
     * <a href="https://www.jython.org/jython-old-sites/docs/using/cmdline.html">Jython
     * Command Line</a>
     *
     * @param args arguments to the program.
     * @throws IOException
     * @throws InterruptedException
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        // Create an instance of the class in which to compose argument list
        JythonCli jythonCli = new JythonCli();

        try {
            jythonCli.initEnvironment(args);

            // Normally we have a script file (but it's optional)
            if (jythonCli.scriptFilename != null) {
                jythonCli.readJBangBlock();
                jythonCli.interpretJBangBlock();
            }

            // Finally launch Jython via JBang
            jythonCli.runProcess();

        } catch (IOException ioe) {
            System.err.println(ioe.toString());
            System.exit(1);

        } catch (InterruptedException ie) {
            System.exit(3);
        }
    }
}
