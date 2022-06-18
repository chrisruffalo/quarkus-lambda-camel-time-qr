# Quarkus Lamda / Rest -> QR Code Current Time

## Requirements
* Maven 3.8.x
* Java 17 (GraalVM JDK 17, 22.x for native binary)

## Execution
[]$ mvn quarkus:dev

## Description
This is probably the most awesome stupid thing I've ever created. It responds with a QR code of the current time. That's it. But...
it supports png, html, and ascii output.

```bash
[]$ curl localhost:8080/time.ascii


    ▄▄▄▄▄▄▄ ▄    ▄  ▄   ▄ ▄▄▄▄▄▄▄
    █ ▄▄▄ █ █▄ ▀█▀ ▄▀▄▄▀▀ █ ▄▄▄ █
    █ ███ █ ▀██ █▄█▀▄  ▀█ █ ███ █
    █▄▄▄▄▄█ █▀█ ▄ █ █▀█▀▄ █▄▄▄▄▄█
    ▄▄  ▄▄▄   █▀▄▄▀▄ █▄█▀  ▄ ▄▄▄▄
    █▄ ▄  ▄  ▄▄   ▄█  ▄ ▀██▄█▀███
     ▄ █▄▄▄█ ▀ ▄▀▄█ ▀█▄▀█▀ █ ▄ ▀
    ██▀█ ▄▄▄▀▀▀▄█▄███ ▄ ▀█▀▄▀▄ ██
    ▀▀██ ▀▄▀▀▀ ▀▄█ ▀▄█▄▄ ▀ ▄▄  ▀▀
    ▄ ▀▀▄ ▀  ▄▄ ▀█▄▄██▀▀▄▄▄██▄▄▄▄
      █▄▀ ▀▀█▀▄ ▄ ▄█ █▄ ▄▄██▄  █▀
    ▀▀▀▀▀ ▀▀▄▄█▀▄▀▀ ▄▀▀▀█▀▀▀█ ▄▄▄
    █▀▀▀▀▀█ ▄▀▄█▀█▀▀▄█▀▀█ ▀ █ ▀▀▀
    █ ███ █ ▀█▄ ▀█▄▀█▀▀█▀▀▀█▀▀ ▀▄
    █ ▀▀▀ █ ▄█▀ ▄ ▄▀█▄▄▄█▄▄▀ ▀ ██
    ▀▀▀▀▀▀▀ ▀ ▀▀ ▀▀▀▀  ▀▀▀  ▀  ▀



```

It's amazing!