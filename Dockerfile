FROM openjdk:18

COPY ./target/*.jar app.jar
COPY ./game_aggregator-root-certificate.pem game_aggregator-root-certificate.pem

EXPOSE 8082

ENTRYPOINT java -jar /app.jar
