FROM eclipse-temurin:21 AS build

WORKDIR /app
COPY . .
RUN ./mvnw clean package


FROM eclipse-temurin:21-jre AS release

WORKDIR /app
COPY --from=build /app/target/java-fastpaws-1.0-SNAPSHOT.jar app.jar
COPY logging.properties .

EXPOSE 4445/udp
EXPOSE 4446/udp

ENTRYPOINT ["java", "-Djava.util.logging.config.file=logging.properties", "-jar", "app.jar"]

