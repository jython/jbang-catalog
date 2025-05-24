/// usr/bin/env jbang "$0" "$@" ; exit $?

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
import org.python.Version;
import org.tomlj.Toml;
import org.tomlj.TomlParseResult;

public class JythonCli {

  List<String> deps = new ArrayList<>();
  List<String> ropts = new ArrayList<>();
  String jythonVersion = Version.PY_VERSION;
  String javaVersion = getJvmMajorVersion();
  boolean debug = false;
  StringBuilder tomlText = new StringBuilder();
  TomlParseResult tpr = null;

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

  void initEnvironment(String[] args) throws IOException {

    // Check that that Java 8 (1.8) or higher is used
    if (Integer.parseInt(javaVersion) < 8) {
      System.out.println("jython-cli: error, Java 8 or higher is required");
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
          found = false;
          break;
        } else if (found && line.startsWith("# ")) {
            if (tomlText.isEmpty()) {
                tomlText.append(line.substring(2));
            } else {
                tomlText.append("\n").append(line.substring(2));
            }
        }
      }
    }

    // Parse the TOML data
    if (!tomlText.isEmpty()) {
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

  void runProcess(String[] args) throws IOException, InterruptedException {
    // Construct the JBang command to be executed
    List<String> cmd = new LinkedList<>();

    String ext = System.getProperty("os.name").toLowerCase().startsWith("win") ? ".cmd" : "";
    cmd.add("jbang" + ext);

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

    cmd.add("org.python:jython-standalone:" + jythonVersion);

    Collections.addAll(cmd, args);

    if (debug) {
      System.err.println("[jython-cli] " + cmd.toString());
    }

    // Execute the JBang command
    ProcessBuilder pb = new ProcessBuilder(cmd);
    pb.inheritIO();
    pb.start().waitFor();
  }

  public static void main(String[] args) throws IOException, InterruptedException {
    JythonCli jythonCli = new JythonCli();
    jythonCli.initEnvironment(args);
    jythonCli.runProcess(args);
  }
}
