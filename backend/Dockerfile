FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml ./
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B verify

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN apk add --no-cache wget && addgroup -S app && adduser -S app -G app
USER app

COPY --from=build /workspace/target/academic-monitor-backend-0.1.0-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
