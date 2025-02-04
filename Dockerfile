
# Use an official OpenJDK runtime as a parent image
FROM eclipse-temurin:17-jdk

# Set the working directory
WORKDIR /app

# Copy the built JAR file into the container
COPY target/url_shortener-1.0-SNAPSHOT.jar app.jar

# Expose the port your Spring Boot app runs on
EXPOSE 8090

# Run the Spring Boot app
CMD ["java", "-jar", "app.jar"]
