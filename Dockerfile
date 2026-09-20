FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn --batch-mode --no-transfer-progress verify

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/target/caroai.jar /app/caroai.jar
ENV HOST=0.0.0.0 PORT=8088
EXPOSE 8088
USER 10001:10001
CMD ["java", "-XX:MaxRAMPercentage=65", "-Djava.awt.headless=true", "-cp", "/app/caroai.jar", "caroai.web.WebServer"]
