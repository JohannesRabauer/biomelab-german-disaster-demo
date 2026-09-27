# CrisisScope

A German disaster/crisis operations dashboard, built live on stream with [BiomeLab](https://biomelab.dev/) as the coding companion. It pulls real warning feeds (DWD weather warnings, BBK civil protection alerts) into a Spring Boot + Thymeleaf app and renders them on a live map with severity-based KPIs.

[![Watch the stream](https://img.youtube.com/vi/9-WxSKFFEcE/maxresdefault.jpg)](https://www.youtube.com/watch?v=9-WxSKFFEcE)

## Links

- 📝 Article: [rabauer.dev/en/blog/biomelab](https://rabauer.dev/en/blog/biomelab/)
- 🎥 Video: [Watch on YouTube](https://www.youtube.com/watch?v=9-WxSKFFEcE)
- 🧬 BiomeLab: [biomelab.dev](https://biomelab.dev/)

## Running locally

```bash
./mvnw spring-boot:run
```

The dashboard is served at `http://localhost:8080`.

## Stack

- Java 21, Spring Boot 3 (Web, Thymeleaf, Validation)
- Live data providers for DWD weather warnings and BBK civil protection alerts, with a sample provider as fallback
