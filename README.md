# Jython JBang Catalog

Catalog for the JBang command line launcher (https://jbang.dev)

## Usage

To use the JBang scripts from this catalog you need to have [JBang](https://www.jbang.dev/) installed.

To start the Jython interpreter or run a Jython script via the interpreter, use JBang as follows and add additional options as required:

* `jbang run jython-cli@jython <jython-script>.py`

## App

The `jython-cli@jython` JBang script can also be installed as a JBang application which is accessible from the command-line.

```
jbang app install jython-cli@jython
[jbang] Command installed: jython-cli
```

Jython scripts can be run using the `jython-cli` application as:

* `jython-cli <jython-script>.py`

On supported platforms a Jython script (with execution permissions enabled) can also be started as

`./<jython-script>.py`

provided that the first line of the script contains text:

`#!/usr/bin/env jython-cli`

## PEP-723

`jython-cli` uses the `PEP-723` inline script metadata format to specify:

* The version of Jython to use: 2.7.1, ...
* The major version of Java to use: 17, 21, ...
* Maven dependencies to make available to the Jython script
* Additional Java runtime options to use

[Jython metadata for JBang]
```
# /// jbang
# requires-jython = "2.7.4"
# requires-java = "21"
# dependencies = [
#   "io.leego:banana:2.1.0"
# ]
# [java]
#   runtime-options = "-server -Xms2g -Xmx2g -XX:+UseZGC -XX:+ZGenerational"
# ///
```

[Jython script banner.py with metadata for JBang]

```python
#!/usr/bin/env jython-cli

# banner.py

# /// jbang
# requires-jython = "2.7.4"
# requires-java = "21"
# dependencies = [
#   "io.leego:banana:2.1.0",
# ]
# [java]
#   runtime-options = ""
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

## Example 1 - run Jython and display its version number

```bash
jbang run jython-cli@jython --version
```

```
2.7.4.0
```

## Example 2 - run Jython scripts passed in as a string

```bash
jbang run jython-cli@jython -c "print(1+2)"
```

```
3
```

## Example 3 - run Jython command-line interpreter

```bash
jbang run jython-cli@jython
```

```
Jython 2.7.4 (tags/v2.7.4:3f256f4a7, Aug 18 2024, 10:30:53)
[OpenJDK 64-Bit Server VM (Eclipse Adoptium)] on java21.0.6
Type "help", "copyright", "credits" or "license" for more information.
>>> print(1+2)
3
>>> 
```

## Example 4 - run Jython script banner.py

Script `banner.py` is located in folder `https://github.com/jython/jbang-catalog/examples`.

```
jbang run jython-cli@jython banner.py

['banner.py']
      _       _   _                   ____   _____ 
     | |_   _| |_| |__   ___  _ __   |___ \ |___  |
  _  | | | | | __| '_ \ / _ \| '_ \    __) |   / / 
 | |_| | |_| | |_| | | | (_) | | | |  / __/ _ / /  
  \___/ \__, |\__|_| |_|\___/|_| |_| |_____(_)_/   
        |___/                                      
```
## Example 5 - run Jython script restclient.py

Script `restclient.py` is located in folder `https://github.com/jython/jbang-catalog/examples`.

```
jbang run jython-cli@jython restclient.py

{
  "userId": 1,
  "id": 1,
  "title": "delectus aut autem",
  "completed": false
}
```

## Example 6 - Turtle Graphics

Script `turtle.py` is located in folder `https://github.com/jython/jbang-catalog/examples`.

```
jbang run jython-cli@jython turtle.py
```

![Alt text](images/turtle.png)

## Development testing

If the `jython_cli.java` program is modified and needs to be tested (before changes
are submitted to the repo), the example scripts can be used as tests and run 
locally:

* jbang run jython-cli examples/banner.py
* jbang run jython-cli examples/restclient.py
* jbang run jython-cli examples/httpserver.py
* jbang run jython-cli examples/turtle.py
* jbang run jython-cli examples/simpletest.py

## Articles about Jython and JBang

The following articles describe the work that led to the creation of the `jython-cli` JBang script.

* [Medium.com - Running Jython scripts with JBang using a Java helper program (Part 1)](https://medium.com/@werner.fouche/running-jython-scripts-with-jbang-using-a-java-helper-program-9ab9f8e35ddc)
* [Medium.com - Running Jython scripts with JBang using PEP 723 (Part 2)](https://medium.com/@werner.fouche/running-jython-scripts-with-jbang-part-2-d13b3699c015)

