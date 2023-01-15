FROM mcr.microsoft.com/playwright/java:latest
ADD https://github.com/ufoscout/docker-compose-wait/releases/download/2.9.0/wait /wait
RUN chmod +x /wait
RUN apt-get -y install fping
COPY ./build/libs/tracker.jar /app.jar
CMD /wait && java ${JAVA_OPTS} -jar /app.jar
