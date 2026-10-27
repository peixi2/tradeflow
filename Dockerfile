FROM node:22-alpine AS frontend-builder
WORKDIR /build/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM maven:3.9.9-eclipse-temurin-21-alpine AS backend-builder
WORKDIR /build
COPY pom.xml ./
COPY src/ ./src/
COPY --from=frontend-builder /build/frontend/dist/ ./src/main/resources/static/
RUN mvn -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S tradeflow && adduser -S tradeflow -G tradeflow
COPY --from=backend-builder /build/target/tradeflow-1.0.0.jar /app/app.jar
USER tradeflow
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=72 -XX:+UseG1GC"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]

