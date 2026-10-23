# ---- Etapa 1: compilación ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

# ---- Etapa 2: ejecución ----
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --no-create-home appuser && mkdir -p /app/almacenamiento && chown appuser /app/almacenamiento
COPY --from=build /app/target/app.jar app.jar
USER appuser
ENV JAVA_TOOL_OPTIONS="-Duser.timezone=America/Lima -Djava.awt.headless=true"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
