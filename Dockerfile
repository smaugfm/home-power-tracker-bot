FROM eclipse-temurin:17
COPY ./build/libs/tracker.jar /app.jar
CMD java ${JAVA_OPTS} -jar /app.jar
