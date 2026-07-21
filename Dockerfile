FROM maven:3.9.11-eclipse-temurin-17-alpine AS build

WORKDIR /workspace
COPY pom.xml ./
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-alpine

RUN apk add --no-cache curl \
    && addgroup -S app \
    && adduser -S -G app app

WORKDIR /app
COPY --from=build /workspace/target/smarthiring-0.0.1-SNAPSHOT.jar app.jar
RUN chown app:app app.jar

USER app
EXPOSE 8080
ENV JAVA_OPTS="-Xms128m -Xmx768m -XX:+UseG1GC"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
