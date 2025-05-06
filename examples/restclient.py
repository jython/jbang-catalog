#!/usr/bin/env jython-cli

# restclient.py

# /// jbang
# requires-jython = "2.7.4"
# requires-java = "21"
# dependencies = [
#   "org.springframework.boot:spring-boot-starter-web:3.4.5"
# ]
# [java]
#   runtime-options = "-server -Xms2g -Xmx2g -XX:+UseZGC -XX:+ZGenerational"
# [jython-cli]
#   debug = false
# ///

import java.lang.String as String
import org.springframework.web.client.RestClient as RestClient

restClient = RestClient.create()

def restApiCall(uri, id):
    rsp = restClient.get().uri(uri, id).retrieve().body(String)
    print(rsp)

def main():
    restApiCall("https://jsonplaceholder.typicode.com/todos/{id}", 1)

main()
