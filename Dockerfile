# ---- build stage ----
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Download dependencies first so they're cached unless pom.xml changes
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline

COPY src/ src/
# Tests run in CI / locally (./mvnw test); skip them here to keep image builds fast
RUN ./mvnw -q -B package -DskipTests && cp target/*.jar app.jar

# ---- run stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app

RUN useradd --system --uid 1001 spring
USER spring

COPY --from=build /app/app.jar app.jar

# The H2 console allows arbitrary SQL: never expose it from a deployed container
ENV SPRING_H2_CONSOLE_ENABLED=false
# Fit the heap to the container (Render free tier = 512 MB)
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC"

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
