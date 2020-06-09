#FROM anapsix/alpine-java

#VOLUME /tmp

#EXPOSE 50505

#ADD out/artifacts/notgen2_spring_jar/notgen2-spring.jar app.jar
#ADD target/notgen2-0.0.1-SNAPSHOT.jar app.jar

#ADD tokens/StoredCredential tokens/
#ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]

FROM openjdk:8-jdk-alpine
VOLUME /tmp
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar
COPY tokens/StoredCredential /
ENTRYPOINT ["java","-Dspring.profiles.active=dev2","-jar","/app.jar"]
