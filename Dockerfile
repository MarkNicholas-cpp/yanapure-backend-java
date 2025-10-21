# Build stage
FROM eclipse-temurin:17-jdk as build
WORKDIR /workspace
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -q -DskipTests dependency:go-offline
COPY src ./src
RUN ./mvnw -q -DskipTests package

# Run stage
FROM eclipse-temurin:17-jre
WORKDIR /app
# non-root for safety
RUN useradd -ms /bin/bash appuser
COPY --from=build /workspace/target/*.jar /app/app.jar
USER appuser
EXPOSE 8080
# Spring profile 'docker' makes DB host=postgres
ENV SPRING_PROFILES_ACTIVE=docker
ENTRYPOINT ["java","-jar","/app/app.jar"]
