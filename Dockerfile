FROM --platform=linux/amd64 eclipse-temurin:18-jre
COPY jamo-proxy-0.7.0-SNAPSHOT.jar server.jar
CMD ["java","-Dfile.encoding=iso-8859-1","-jar","server.jar"]