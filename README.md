# Quarkus Lamda / Rest -> QR Code Current Time

## Requirements
* Maven 3.8.x
* Java 17 (GraalVM JDK 17, 22.x for native binary)

## Execution
```bash
[]$ mvn quarkus:dev
```

## Building / Containerizing
```bash
[]$ mvn install -Pnative
[]$ docker build -f src/container/Docker.native -t {repo}/qr-native:latest
[]$ docker push {repo}/qr-native:latest
```

## Description
This is probably the most awesome stupid thing I've ever created. It responds with a QR code of the current time. That's it. But...
it supports png, html, and ascii output.

```bash
[]$ curl localhost:8080/time.ascii


    █▀▀▀▀▀█ █▄ █  ▀▀▄▀  ▀██▀▄▀ ██ █▀▀▀▀▀█
    █ ███ █ ▄ ███▀█▀▄▀▄█ ▀ ▄█▄▄█▀ █ ███ █
    █ ▀▀▀ █ █▀▀▄▀ ▀  ▄▄██▄▄ ▄█▄█  █ ▀▀▀ █
    ▀▀▀▀▀▀▀ ▀▄▀▄█ █▄▀ ▀ █ ▀▄█ █ ▀ ▀▀▀▀▀▀▀
     ▄▄█ ▄▀▄ ▄▄█ ██ ▀▀▀  ▀▀ ▀ ▄▄█▄ ▀▀█ ▀█
    █▀██▄▄▀▀  ▄█ ▀ █▄█ ▀▄█ ▄██ ▀▄ █▀██▀▄█
    ▀▀ ▀█▄▀█ ▄█ ███▄▄█▀  ▄▄█ ▀█▀▄▄█▀ ▄▄▀█
    ▀ ▄█▄▀▀▀▄ █▀▄▀ ▄█▀█▀ ▀▄▀▀█ ▄ █▀  ▄ █▀
    ▄▀ ▀▄█▀ █ ▀█ █▄▄ █ ▄ ▀▄█  █ ▀▄█▄▄█▀██
    ██▀█ ▀▄ ▀▄▄▀█ ▄▀█ █▄  ▀▄▄ ▄▄▀█▄▄██▀ █
    ▀▀ ▀▀▄▄▀ ▄▀   █▄█▄█▄▄▀ █▄ ▀   ██ ▀▀██
    ▄ ▀▄▀ ▄▀▄▀▄▀▄▀▄█ ▄█ ▄█ ██ ▀▀▄█████ ▄▄
    ▄▀   ▄▄▀█ ▀▀▀ █ ▄▀ ▀ ▀ ▀█▄ ▀▀  █ ▄ ▄█
    ▄████▀▄▀█▄▄▄██▄ ▄ ▀▄▀█▀▄▀▀▄██▄▄▄▄▄ █▄
    ▄▄▄▄▄▄▄ ▀█▀▄ ▀▀ ▀▄▀▄█  ▀▀▄ ▄█ ▄ █▀▄█▄
    █ ▄▄▄ █  ▀▄ ▄▀█▄▀█▄▄▄ ▄█▀▄ ▀█▄▄▄██▀█▄
    █ ███ █ ▀▀▄▀▀█▀▄▀█▀ ▄██▀  █▄ █ ▄█  ▀█
    █▄▄▄▄▄█   █ ▀▄█▄▄ ▄▀▀ █ ▄▀  ██ ▀▀█ ▄▄


```

It's amazing!

## Notes

If you deploy this and it keeps giving you 500 errors without actually starting the lambda or logging anything in CloudWatch:
```bash
[]$ aws lambda add-permission --function-name ${function arn} --statement-id ${name you want to give to this new permission} --action lambda:InvokeFunction --source-arn "${arn of rest api resource}" --principal "*"
```