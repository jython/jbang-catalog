# jython-cli

While Jython scripts with Maven dependencies can be executed using only JBang, 
this method is often cumbersome for users. The `jython-cli` script simplifies this 
process by handling these actions in the background, shielding users from the
underlying complexity.

```python
# banner.py
import sys

import io.leego.banana.BananaUtils as BananaUtils
import io.leego.banana.Font as Font

def main():
    print(sys.argv)

    text0 = "Jython 2.7"
    text1 = BananaUtils.bananaify(text0, Font.STANDARD)

    print(text1)

main()
```

```bash
$ jbang run --java 21 \
    --deps io.leego:banana:2.1.0 \
    --main org.python.util.jython \
    org.python:jython-standalone:2.7.4 \
    banner.py
```

```
['banner.py']
      _       _   _                   ____   _____
     | |_   _| |_| |__   ___  _ __   |___ \ |___  |
  _  | | | | | __| '_ \ / _ \| '_ \    __) |   / /
 | |_| | |_| | |_| | | | (_) | | | |  / __/ _ / /
  \___/ \__, |\__|_| |_|\___/|_| |_| |_____(_)_/
        |___/
```

Optional metadata in the Jython script provide information to `jython-cli` to enable corresponding options in JBang.

## Inline Script Metadata

We took inspiration from [PEP-723](https://peps.python.org/pep-0723/) and adopted its metadata format (TOML) to specify:

* Maven Central dependencies
* Java runtime options
* Version of Jython to use
* Version of Java (JDK) to use

The metadata information starts with `# /// jbang` and ends with `# ///`. Each line in between these two markers are stripped of the first two characters, and the rest of the line is assumed to be valid TOML data.

**banner2.py**

```python
#!/usr/bin/env jython-cli

# banner.py

# /// jbang
# requires-jython = "2.7.4"
# requires-java = "21"
# dependencies = [
#   "io.leego:banana:2.1.0"
# ]
# ///

import sys

import io.leego.banana.BananaUtils as BananaUtils
import io.leego.banana.Font as Font

def main():
    print(sys.argv)

    text0 = "Jython 2.7"
    text1 = BananaUtils.bananaify(text0, Font.STANDARD)

    print(text1)

main()
```

**TOML data extracted from banner2.py**

```toml
requires-jython = "2.7.4"
requires-java = "21"
dependencies = [
  "io.leego:banana:2.1.0"
]
```


## Jython script execution

`$ jbang run jython-cli@jython banner.py`

### Step 1 - Metadata

Extract JBang metadata (if present) from the Jython script. 
The metadata block is optional, therefore `jython-cli` can execute unmodified Jython scripts.

```
# /// jbang
# requires-jython = "2.7.4"
# requires-java = "21"
# dependencies = [
#   "io.leego:banana:2.1.0"
# ]
# ///
```

### Step 2 - Build JBang command

Construct the JBang command to be executed specifying:

* The version of Jython to use
* The version of Java to use
* Java runtime options
* Maven dependencies
* Additional command-line arguments

### Step 3 - Execute JBang command

```java
String cmd = "jbang run --java 21 --deps io.leego:banana:2.1.0 --main org.python.util.jython org.python:jython-standalone:2.7.4 banner.py"

// Execute the JBang command
ProcessBuilder pb = new ProcessBuilder(cmd.split("\\s+"));
pb.inheritIO();
pb.start().waitFor();
```