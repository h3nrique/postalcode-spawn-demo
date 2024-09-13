FROM docker.io/library/maven:3-openjdk-17 AS build
LABEL authors="Paulo Henrique Alves <paulohenriqueas13@gmail.com>"
RUN mkdir -p /usr/src
WORKDIR /usr/src
COPY . /usr/src
RUN mvn clean install -DskipTests -Dmaven.javadoc.skip=true

FROM docker.io/library/openjdk:17
LABEL authors="Paulo Henrique Alves <paulohenriqueas13@gmail.com>"
EXPOSE 8091 8080
COPY --from=build  /usr/src/target/postalcode-spawn-demo-shaded.jar  /opt/
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/opt/postalcode-spawn-demo-shaded.jar"]
