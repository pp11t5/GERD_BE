FROM gradle:9.4.1-jdk21-jammy AS builder

WORKDIR /app

ENV TZ=Asia/Seoul

RUN apt-get update && apt-get install -y tzdata && \
    ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && \
    echo $TZ > /etc/timezone && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
RUN ./gradlew dependencies --no-daemon

COPY src ./src
RUN GRADLE_OPTS="-Xmx512m -Xms256m" ./gradlew clean bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

ENV TZ=Asia/Seoul

RUN apt-get update && apt-get install -y tzdata && \
    ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && \
    echo $TZ > /etc/timezone && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul", "-jar", "app.jar"]
