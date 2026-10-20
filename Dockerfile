FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts gradle.properties ./

RUN --mount=type=secret,id=github_username \
    --mount=type=secret,id=github_token \
    test -s /run/secrets/github_username && \
    test -s /run/secrets/github_token && \
    export USERNAME="$(cat /run/secrets/github_username)" && \
    export TOKEN="$(cat /run/secrets/github_token)" && \
    ./gradlew dependencies --no-daemon

COPY src src

RUN --mount=type=secret,id=github_username \
    --mount=type=secret,id=github_token \
    export USERNAME="$(cat /run/secrets/github_username)" && \
    export TOKEN="$(cat /run/secrets/github_token)" && \
    ./gradlew bootJar --no-daemon


FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
