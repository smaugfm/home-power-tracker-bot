FROM eclipse-temurin:17-alpine
RUN apk add iputils
COPY ./build/libs/tracker.jar /app.jar
CMD java ${JAVA_OPTS} -jar /app.jar
