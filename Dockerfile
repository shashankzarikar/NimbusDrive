# Stage 1: Build the application using Maven and Java 23
FROM maven:3.9.6-eclipse-temurin-23 AS build
COPY . .
RUN mvn clean package -DskipTests

# Stage 2: Run the compiled application JAR
FROM eclipse-temurin:23-jre-alpine
COPY --from=build /target/NimbusDrive-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]