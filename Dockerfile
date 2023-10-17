FROM maven:3-openjdk-17 as build
LABEL authors="Paulo Henrique Alves <paulo.alves@fabricads.com.br>"
RUN mkdir -p /usr/src
WORKDIR /usr/src
COPY . /usr/src
RUN mvn clean install -DskipTests -Dmaven.javadoc.skip=true

FROM openjdk:18
LABEL authors="Paulo Henrique Alves <paulo.alves@fabricads.com.br>"
EXPOSE 8091 8080
COPY --from=build  /usr/src/target/postalcode-spawn-demo-shaded.jar  /opt/
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/opt/postalcode-spawn-demo-shaded.jar"]

