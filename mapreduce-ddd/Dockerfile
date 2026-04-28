FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

FROM apache/hadoop:3.3.6
COPY --from=build /build/target/mapreduce-ddd-1.0.0.jar /app/mapreduce-ddd-1.0.0.jar
