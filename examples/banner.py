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
