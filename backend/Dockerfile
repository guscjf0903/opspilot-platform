FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace

COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
COPY src ./src

RUN chmod +x ./gradlew && ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN useradd --uid 10001 --create-home opspilot

COPY --from=build /workspace/build/libs/*.jar /app/opspilot-api.jar

USER opspilot

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/opspilot-api.jar"]
