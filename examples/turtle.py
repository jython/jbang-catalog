#!/usr/bin/env jython-cli

# turtle.py

# ------------------------------------------
#
# aplu5.jar is not published to Maven Central
#
# To make aplu5.jar available to Jython, install it in
# the local Maven repository by following these steps:
#
#  (1 - Download file) $ wget https://www.java-online.ch/download/aplu5.jar
#
#  (2 - Install file ) $ mvn install:install-file -Dfile=./aplu5.jar -DgroupId=ch.aplu.turtle -DartifactId=aplu5 -Dversion=0.1.9 -Dpackaging=jar
#
# ------------------------------------------

# /// jbang
# requires-jython = "2.7.4"
# requires-java = "21"
# dependencies = [
#   "ch.aplu.turtle:aplu5:0.1.9",
# ]
# [java]
#   runtime-options = ""
# [jython-cli]
#   debug = false
# ///


import ch.aplu.turtle.TurtleFrame as TurtleFrame
import ch.aplu.turtle.Turtle as Turtle

def drawSquare(turtle, length):
    for i in range(4):
        turtle.forward(length)
        turtle.right(90)

def main():
    myTurtleframe = TurtleFrame("Turtle Demo", 500, 500)
    tt0 = Turtle(myTurtleframe)
    tt0.setPenColor("red")
    tt0.right(90)
    for i in range(10):
        drawSquare(tt0, 150)
        tt0.left(36)

main()
