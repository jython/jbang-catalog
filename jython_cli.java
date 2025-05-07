///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS dev.jbang:jash:0.0.3
//DEPS org.tomlj:tomlj:1.1.1
//DEPS org.python:jython-slim:2.7.4
//JAVA 21

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import dev.jbang.jash.Jash;
import org.tomlj.Toml;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlTable;

import org.python.util.jython;

public class jython_cli {

    // FIX_NUMBER appended to the Jython version forms the version of jython-cli as
    // [Jython version].[FIX_NUMBER], e.g. 2.7.4.0
    // Increment FIX_NUMBER with each new release of jython-cli.
    // If the version number of Jython changes (for example 2.7.4 becomes 2.7.5), then
    // FIX_NUMBER is reset to 0 again, e.g. 2.7.5.0
    private static final int FIX_NUMBER = 0;  

    private static final String textJythonApp = """
            
            import org.python.util.jython;
            
            public class __CLASSNAME__ {
            
                public static void main(String[] args) {
                    jython.main(args);
                }
            
            }            
            """;

    public static void main(String[] args) throws IOException {
        List<String> deps = new ArrayList<>();
        String jythonVersion = "2.7.4";
        String javaVersion = "21";
        String javaRuntimeOptions = "";
        String ls = System.lineSeparator();
        boolean debug = false;
        StringBuilder tomlText = new StringBuilder("");

        System.setProperty("python.console.encoding", "UTF-8");

        // --version
        if (args.length == 1 && args[0].equals("--version")) {
            System.out.println(jythonVersion + "." + FIX_NUMBER);
            System.exit(0); 
        }

        // Determine the Jython script filename, if it exists
        String scriptFilename = "";
        for (String arg: args) {
            if (arg.endsWith(".py")) {
                scriptFilename = arg;
                break;
            }
        }

        // Extract TOML data
        if (scriptFilename.length() > 0) {
            List<String> lines = Files.readAllLines(Paths.get(scriptFilename));
            boolean found = false;
            for (String line: lines) {
                if (line.startsWith("# /// jbang")) {
                    found = true;
                }
                else if (line.startsWith("# ///")) {
                    found = false;
                    break;
                } else if (line.startsWith("# ")) {
                    if (found) {
                        if (tomlText.length() == 0) {
                            tomlText.append(line.substring(2));
                        } else {
                            tomlText.append(ls + line.substring(2));
                        }
                    }
                }
            }
        }

        // Invoke the Jython interpreter if no Python script file is specified, or the script has no TOML data
        if (tomlText.toString().equals("")) {
            jython.main(args);
            System.exit(0);
        }

        String javaClassname = new File(scriptFilename).getName().replace(".", "_");
        String javaFilename = javaClassname + ".java";

        // Parse PEP 723 text block
        {
            TomlParseResult tpr = Toml.parse(tomlText.toString());
            // [jython-cli]
            TomlTable pythonjvmTable = tpr.getTable("jython-cli");
            if (pythonjvmTable != null) {
                if (pythonjvmTable.isBoolean("debug")) {
                    debug = pythonjvmTable.getBoolean("debug");
                }
            }
            if (debug) {
                System.out.println("");
                System.out.println("[ -----------------jbang-toml-config-begin-------------------- ]");
                System.out.println("");
                System.out.println(tpr.toToml());
                System.out.println("[ -----------------jbang-toml-config-end---------------------- ]");
                System.out.println("");
            }
            if (tpr.isString("requires-jython")) {
                jythonVersion = tpr.getString("requires-jython");
            }
            if (tpr.isString("requires-java")) {
                javaVersion = tpr.getString("requires-java");
            }
            // dependencies
            for (Object e : tpr.getArrayOrEmpty("dependencies").toList()) {
                String dep = (String) e;
                deps.add(dep);
            }
            // [java]
            TomlTable javaTable = tpr.getTable("java");
            if (javaTable != null) {
                String runtimeOptions = javaTable.getString("runtime-options");
                if (runtimeOptions != null) {
                    javaRuntimeOptions = runtimeOptions;
                }
            }
        }

        String dep = "org.python:jython-slim:" + jythonVersion;
        deps.add(dep);

        try (BufferedWriter jf = new BufferedWriter(new FileWriter(javaFilename))) {
            jf.write("///usr/bin/env jbang \"$0\" \"$@\" ; exit $?" + ls + ls);
            for (String dependency : deps) {
                jf.write("//DEPS " + dependency + ls);
            }
            jf.write("//JAVA " + javaVersion + ls);
            if (javaRuntimeOptions.length() > 0) {
                jf.write("//RUNTIME_OPTIONS " + javaRuntimeOptions + ls);
            }
            String text = textJythonApp;
            String jtext = text.replace("__CLASSNAME__", javaClassname);
            jf.write(jtext);
        }

        // register javaFilename to be deleted when the JVM exits
        new File(javaFilename).deleteOnExit();

        // display the Java shim file
        if (debug) {
            System.out.println("");
            System.out.println("[ -----------------java-shim-file-begin----------------------- ]");
            System.out.println("");
            try {
                // Step 2: Call Files.readAllLines() to read the file content
                List<String> lines = Files.readAllLines(Paths.get(javaFilename));

                // Print each line to the console
                lines.forEach(System.out::println);
            } catch (IOException e) {
                // Step 3: Handle the IOException
                e.printStackTrace();
            }
            System.out.println("");
            System.out.println("[ -----------------java-shim-file-end------------------------- ]");
            System.out.println("");
        }

        // jbang run <script>_py.java param1 param2 ...
        {
            StringBuilder params = new StringBuilder("run --java-options=-Dpython.console.encoding=UTF-8");

            params.append(" " + javaFilename);
            for (int i = 0; i < args.length; i++) {
                params.append(" " + args[i]);
            }
            if (debug) {
                System.out.println("jbang " + params.toString());
                System.out.println();
            }
            String ext = System.getProperty("os.name").toLowerCase().startsWith("win") ? ".cmd" : "";
            var jargs = params.toString().split("\\s+");
            try (Stream<String> ps = Jash.start("jbang" + ext, jargs).stream()) {
                    ps.forEach(System.out::println);
            }
        }
    }
}
