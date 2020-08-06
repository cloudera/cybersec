ARG VERSION_DEFAULT=1.0-SNAPSHOT

FROM maven:3.6.3-jdk-8-slim as builder
ARG VERSION_DEFAULT
WORKDIR /usr/app

# cache maven dependencies
ADD pom.xml .
ADD parser-chains-core/pom.xml ./parser-chains-core/
ADD parser-chains-parsers/pom.xml ./parser-chains-parsers/
ADD parser-chains-config-service/pom.xml ./parser-chains-config-service/
ADD parser-chains-storm/pom.xml ./parser-chains-storm/
RUN mvn -B clean install -DskipTests -Dcheckstyle.skip -Dmaven.exec.skip=true -Dmaven.install.skip -Dmaven.resources.skip -Dassembly.skipAssembly=true -Dspring-boot.repackage.skip

# build the project
COPY . .
RUN mvn -B install

FROM openjdk:8-jdk-alpine3.9
WORKDIR /usr/app
ARG VERSION_DEFAULT
ENV VERSION=${VERSION_DEFAULT}
ENV PORT=3000
ENV CONFIG_PATH=/tmp/config

# execute the service
COPY --from=builder /usr/app/parser-chains-config-service/target/parser-chains-config-service-${VERSION}.jar .
CMD java -jar /usr/app/parser-chains-config-service-${VERSION}.jar --server.port=${PORT} --config.path=${CONFIG_PATH}
