FROM leexiaoming/openjdk-curl:8u171-jre-alpine
CMD java ${JAVA_OPTS} -jar cell-apigateway-test-1.0-SNAPSHOT.jar
COPY target/cell-apigateway-test-1.0-SNAPSHOT.jar .