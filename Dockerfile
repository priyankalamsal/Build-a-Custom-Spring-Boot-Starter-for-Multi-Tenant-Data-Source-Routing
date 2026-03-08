FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /workspace
ENV MAVEN_CONFIG=/root/.m2

COPY pom.xml .
COPY multitenancy-spring-boot-starter multitenancy-spring-boot-starter
COPY demo-application demo-application

RUN mvn -pl demo-application -am clean package -DskipTests -B

FROM eclipse-temurin:17.0.8_7-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=builder /workspace/demo-application/target/demo-application-1.0.0.jar ./app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
