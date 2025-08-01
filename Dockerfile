FROM openjdk:18

COPY ./target/*.jar /ga_vendor/app.jar
COPY ./game_aggregator-root-certificate.pem game_aggregator-root-certificate.pem

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "/ga_vendor/app.jar"]
