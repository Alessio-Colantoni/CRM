# syntax=docker/dockerfile:1
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn -B -Dmaven.test.skip=true package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/client_java-2.0.0.jar app.jar
RUN useradd --system --uid 10001 --no-create-home crmapp
USER 10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
