FROM eclipse-temurin:17
RUN mkdir /opt/app
COPY build/libs/notgen.jar /opt/app
CMD ["java", "-jar", "/opt/app/notgen.jar"]
