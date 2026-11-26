# ---------- Build ----------
FROM maven:3-eclipse-temurin-25 AS build
WORKDIR /app
RUN java -version 2>&1 | grep -F '25.0.5.1+2'
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------- Runtime ----------
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN java -version 2>&1 | grep -F '25.0.5.1+2'

# Configuracion via variables de entorno (Spring Boot)
COPY src/main/resources/application.properties.example /app/config/application.properties

COPY --from=build /app/target/api-gestion-0.0.1-SNAPSHOT.jar app.jar

# Usuario no-root (buena practica)
RUN groupadd -r app && useradd -r -g app app && chown -R app:app /app
USER app

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.config.additional-location=optional:file:/app/config/"]
