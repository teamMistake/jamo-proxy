FROM --platform=linux/amd64 eclipse-temurin:18-jre
COPY jamo-proxy-0.1.0-SNAPSHOT.jar server.jar
CMD ["java","-jar","server.jar"]