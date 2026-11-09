FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /workspace/target/job-recruitment-platform-0.0.1-SNAPSHOT.jar app.jar
USER app
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=45s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://127.0.0.1:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:+ExitOnOutOfMemoryError", "-jar", "/app/app.jar"]
