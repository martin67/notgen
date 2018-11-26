FROM anapsix/alpine-java

VOLUME /tmp

EXPOSE 50505

ADD target/notgen2-0.0.1-SNAPSHOT.jar app.jar
ADD tokens/StoredCredential tokens/
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]
