if [ ! -f "mockserver-netty-5.13.2-shaded.jar" ]; then
    wget -O mockserver-netty-5.13.2-shaded.jar https://search.maven.org/remotecontent?filepath=org/mock-server/mockserver-netty/5.13.2/mockserver-netty-5.13.2-shaded.jar
fi

my_hostname=$(hostname -f)

# Autodetect JAVA_HOME if not defined
if [ -e /usr/bin/bigtop-detect-javahome ] ; then
    . /usr/bin/bigtop-detect-javahome
fi

if [[ -d "$JAVA_HOME" ]]; then
    JAVA_RUN="$JAVA_HOME"/bin/java
else
    JAVA_RUN=java
fi

mkdir -p logs
nohup "$JAVA_RUN" -Dmockserver.initializationJsonPath=mockserver_expectations.json -jar mockserver-netty-5.13.2-shaded.jar -serverPort 1081 -proxyRemotePort 1082 -proxyRemoteHost ${my_hostname} -logLevel DEBUG > logs/mockserver.log 2>&1  &

if [[ ! -f "../pipelines/rest.properties.template" ]]; then
    cp ../pipelines/rest.properties ../pipelines/rest.properties.template
fi
cat ../pipelines/rest.properties.template | sed -e 's/REST_MOCK_HOST/'"${my_hostname}"'/g' > ../pipelines/rest.properties 
