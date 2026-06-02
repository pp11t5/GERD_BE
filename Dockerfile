# syntax=docker/dockerfile:1

FROM gradle:9.4.1-jdk21-jammy AS build

WORKDIR /app

COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
RUN ./gradlew dependencies --no-daemon || true

COPY src ./src
RUN GRADLE_OPTS="-Xmx512m -Xms256m" ./gradlew clean bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-jammy AS extract

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

ENV TZ=Asia/Seoul

RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring:spring

COPY --from=extract /app/dependencies/ ./
COPY --from=extract /app/spring-boot-loader/ ./
COPY --from=extract /app/snapshot-dependencies/ ./
COPY --from=extract /app/application/ ./

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -Duser.timezone=Asia/Seoul $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
