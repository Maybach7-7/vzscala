FROM hseeberger/scala-sbt:17.0.2_1.6.2_3.1.1

WORKDIR /app/rps
RUN apt-get update && apt-get install -y wget curl openjdk-17-jre-headless && apt-get clean
COPY . .
RUN sbt update
RUN sbt assembly

EXPOSE 8080
CMD ["java", "-jar", "target/scala-3.2.2/rock-paper-scissors-server.jar"]