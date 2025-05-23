///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS org.tomlj:tomlj:1.1.1
//DEPS org.python:jython-slim:2.7.4

// jython-cli can be run with Java 8 from the command-line as follows:
//
// $ jbang --java 8 jython-cli
//
// [jbang] Building jar for JythonCli.java...
//        Jython 2.7.4 (tags/v2.7.4:3f256f4a7, Aug 18 2024, 16:49:39)
//        [OpenJDK 64-Bit Server VM (Temurin)] on java1.8.0_452
//        Type "help", "copyright", "credits" or "license" for more information.
//        >>>
//

import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.tomlj.Toml;
import org.tomlj.TomlParseResult;

import org.python.Version;

public class JythonCli {

    List<String> deps = new ArrayList<>();
    List<String> ropts = new ArrayList<>();
    String jythonVersion = Version.PY_VERSION;
    String javaVersion = getJvmMajorVersion();
    boolean debug = false;
    StringBuilder tomlText = new StringBuilder("");
    TomlParseResult tpr = null;

    static final String getJvmMajorVersion() {
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

    void initEnvironment(String[] args) throws IOException {  

        // Check that that Java 8 (1.8) or higher is used
        if (Integer.parseInt(javaVersion) < 8) {
            System.out.println("jython-cli: error, Java 8 or higher is required");
            System.exit(1);
        }

        // Determine the Jython script filename (if specified)
        String scriptFilename = "";
        for (String arg: args) {
            if (arg.endsWith(".py")) {
                scriptFilename = arg;
                break;
            }
        }
        
        // Extract TOML data as a String (if present)
        if (scriptFilename.length() > 0) {
            List<String> lines = Files.readAllLines(Paths.get(scriptFilename));
            boolean found = false;
            for (String line: lines) {
                if (found == true && !line.startsWith("# ")) {
                    found = false;
                    tomlText = new StringBuilder("");
                }
                if (found == false && line.startsWith("# /// jbang")) {
                    found = true;
                }
                else if (found == true && line.startsWith("# ///")) {
                    found = false;
                    break;
                } else if (found == true && line.startsWith("# ")) {
                    if (found) {
                        if (tomlText.length() == 0) {
                            tomlText.append(line.substring(2));
                        } else {
                            tomlText.append("\n" + line.substring(2));
                        }
                    }
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
                debug = tpr.getBoolean("debug");
            }

        }

    }   

    void runProcess(String[] args) throws IOException, InterruptedException {
        // Construct the JBang command to be executed
        String cmd = "jbang";
        String ext = System.getProperty("os.name").toLowerCase().startsWith("win") ? ".cmd" : "";
        cmd = cmd + ext;
        StringBuilder params = new StringBuilder("run");
        params.append(" --java " + javaVersion);
        for (String ropt: ropts) {
            params.append(" --runtime-option " + ropt);
        }
        for (String dep: deps) {
            params.append(" --deps " + dep);
        }
        params.append(" --main org.python.util.jython");
        params.append(" org.python:jython-standalone:" + jythonVersion);
        for (String arg: args) {
            params.append(" " + arg);
        }
        cmd = cmd + " " + params.toString();
        if (debug) {
            System.out.println("[jython-cli] " + cmd);
        }
        
        // Execute the JBang command
        ProcessBuilder pb = new ProcessBuilder(cmd.split("\\s+"));
        pb.inheritIO();
        pb.start().waitFor();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        JythonCli jythonCli = new JythonCli();
        jythonCli.initEnvironment(args);
        jythonCli.runProcess(args);
    }
}
