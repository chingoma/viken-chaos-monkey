# Viken Chaos Monkey - Enterprise Chaos Engineering
FROM eclipse-temurin:21-jre-alpine

ENV TZ=Africa/Dar_es_Salaam
ENV JAVA_OPTS="-Duser.timezone=Africa/Dar_es_Salaam"

WORKDIR /app

COPY target/viken-chaos-monkey-*.jar app.jar

EXPOSE 20000

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
