FROM eclipse-temurin:21-jdk-jammy

# Copy jar file
COPY target/finance-dashboard-0.0.1-SNAPSHOT.jar app.jar


# Expose Render port
EXPOSE 8080

# Activate prod profile inside container
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]