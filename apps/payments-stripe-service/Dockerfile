# syntax=docker/dockerfile:1

# ---- Build ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml pom.xml
COPY src src
RUN mvn -q -DskipTests package

# ---- Run ----
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd -ms /bin/bash appuser
COPY --from=build /app/target/*.jar /app/app.jar
RUN chown appuser:appuser /app/app.jar
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:MaxRAMPercentage=75"
EXPOSE 8085
USER appuser
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
