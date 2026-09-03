FROM maven:3.8-openjdk-8 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package

FROM eclipse-temurin:8-jre
WORKDIR /app
COPY --from=build /build/target/catan-1.0-jar-with-dependencies.jar app.jar
# Templates and static assets are read from these on-disk paths at runtime
# (see Main.STATIC_FILE_PATH and the FreeMarker template directory).
COPY --from=build /build/src/main/resources/static src/main/resources/static
COPY --from=build /build/src/main/resources/spark src/main/resources/spark
EXPOSE 4567/tcp
CMD ["java", "-Xmx384m", "-jar", "app.jar"]
