#!/usr/bin/env jython-cli

# restclient.py

# /// jbang
# requires-jython = "2.7.4"
# requires-java = "21"
# dependencies = [
#   "io.leego:banana:2.1.0",
#   "org.springframework.boot:spring-boot-starter-web:3.4.6"
# ]
# runtime-options = [
#   "-Dpython.console.encoding=UTF-8",
#   "-server",
#   "-Xmx2g",
#   "-XX:+UseZGC",
#   "-XX:+ZGenerational"
# ]
# ///

import io.leego.banana.BananaUtils as BananaUtils
import io.leego.banana.Font as Font

import java.lang.String as String
import org.springframework.web.client.RestClient as RestClient

restClient = RestClient.create()

def restApiCall(uri, id):
    rsp = restClient.get().uri(uri, id).retrieve().body(String)
    print(rsp)

def main():
    text0 = "RestClient"
    text1 = BananaUtils.bananaify(text0, Font.STANDARD)
    print(text1)
    restApiCall("https://jsonplaceholder.typicode.com/todos/{id}", 1)

main()
