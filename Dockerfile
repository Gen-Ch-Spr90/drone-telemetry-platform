FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -q
COPY src src
RUN ./mvnw clean package -DskipTests -q

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /workspace/target/drone-telemetry-platform-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]