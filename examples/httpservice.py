#!/usr/bin/env jython-cli

# /// jbang
# requires-jython = "2.7.4"
# requires-java = "21"
# dependencies = [
#   "io.undertow:undertow-core:2.3.18.Final"
# ]
# [java]
#   runtime-options = "-server -Xms2g -Xmx2g -XX:+UseZGC -XX:+ZGenerational"
# [jython-cli]
#   debug = false
# ///

import io.undertow.Undertow as Undertow
import io.undertow.server.HttpHandler as HttpHandler
import io.undertow.server.HttpServerExchange as HttpServerExchange

# http://localhost:8080/
class WebHandler(HttpHandler):
    def handleRequest(self, httpServerExchange):
        httpServerExchange.getResponseSender().send("<html><body><h1>Hello World from Jython/Undertow</h1></body></html>")

def main():
    webHandler = WebHandler()
    builder = Undertow.builder()
    undertow = builder.addHttpListener(8080, "localhost").setHandler(webHandler).build()
    undertow.start()

main()
