
FROM 381492256733.dkr.ecr.ap-northeast-1.amazonaws.com/eclipse-temurin21:AAA-support



COPY ./target/*.jar /ga_vendor/app.jar
COPY ./game_aggregator-root-certificate.pem game_aggregator-root-certificate.pem

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "/ga_vendor/app.jar"]
