# Parser Chains Config Service

### Running project from command line

```bash
# Normal
java -Dloader.path="contrib" -jar parser-chains-config-service-1.0-SNAPSHOT.jar --server.port=3000 --config.path=parser-chain-configs
# Debug mode
java -Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=8000,suspend=n -jar target/parser-chains-config-service-1.0-SNAPSHOT.jar
```

#### Accessing the REST service

*Swagger*
Default http://localhost:8080/swagger-ui.html

*HTTP endpoints*
The api root is `api/v1/parserconfig`
curl -X GET "http://localhost:3000/api/v1/parserconfig/chains" -H "accept: */*"

#### Options Detail

loader.path - This sets the 3rd party lib directory for adding additional classes to the REST application classpath. This is a Spring Boot property that's passed in using Java system properties, i.e. "-D"
server.port - Set the REST service's port
config.path - This is where the rest application will store the parser chain configurations. An ID generator file also resides here. Don't modify it unless you know what you're doing!
server.servlet.context-path - you can change the application context root path here. e.g. setting the value to "parser-config" would change the access endpoint to look like http://localhost:3000/parser-config/swagger-ui.html
```bash
java -jar parser-chains-config-service-1.0-SNAPSHOT.jar --server.port=3000 --config.path=parser-chain-configs --server.servlet.context-path="/parser-config"
```

### Production Deployment

Spring Boot Actuator has been added for production-ready features such as: HTTP endpoints or JMX to monitor the application. Auditing, health, and metrics gathering can also be automatically applied to the application.

By default, we expose only the `health` and `info` endpoints, which is also the Spring Boot default. More details on how to manage the endpoints and expose additional functionality is available below.

View the exposed Actuator endpoints by going to [http://&lt;hostname&gt;:&lt;port&gt;/actuator/](http://<hostname>:<port>/actuator/)

#### Enabling Endpoints

Instructions for enabling Actuator endpoints can be found here - [Enabling Endpoints](https://docs.spring.io/spring-boot/docs/2.2.2.RELEASE/reference/htmlsingle/#production-ready-endpoints-enabling-endpoints)

A list of available endpoints can be found here - [Spring Actuator Endpoints](https://docs.spring.io/spring-boot/docs/2.2.2.RELEASE/reference/htmlsingle/#production-ready-endpoints)

### Spring Notes

Project structure
* controller - sets up the REST endpoints.
* service - core business logic occurs here.
* Application - main application entry point.

`@SpringBootApplication` is a meta-annotation that pulls in component scanning, auto-configuration, and property support.

#### Logging

Logging is done with Log4j2 - https://logging.apache.org/log4j/2.x/

Modifying log level
By default, logging is set to a nice and quiet WARN level in the application.yml file packaged with the application. This can be changed via passing a property on the CLI, e.g.
```commandline
java -Dlogging.level.root=INFO -jar target/service-core-1.0-SNAPSHOT.jar
```

Spring Boot's logging has been excluded in favor of log4j2 directly. In order to accommodate the Spring dependencies that use SLF4J, the log4j-slf4j-impl dependency has been added. The SLF4J dep is needed to eliminate the following error in the logs:
```commandline
SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
SLF4J: Defaulting to no-operation (NOP) logger implementation
SLF4J: See http://www.slf4j.org/codes.html#StaticLoggerBinder for further details.
```
See https://stackoverflow.com/a/41500347 for detail.

*Other*
* https://www.baeldung.com/spring-boot-logging

#### Integration Tests

Relevant to the spring mock mvc tests - https://github.com/json-path/JsonPath

#### Reference

**Spring**

* https://spring.io/guides/gs/spring-boot/
* https://docs.spring.io/spring-boot/docs/2.2.2.RELEASE/reference/htmlsingle
* https://spring.io/guides/tutorials/rest/
* [Spring Hateos](https://spring.io/projects/spring-hateoa) - Used for creating hypermedia-based REST.
* https://spring.io/blog/2019/03/05/spring-hateoas-1-0-m1-released - Many of the HATEOAS examples are still outdated. Refer to this link for hints.
* https://docs.spring.io/spring-hateoas/docs/1.0.0.M1/reference/html/#migrate-to-1.0 - more migration details

**REST Principles**
* https://restfulapi.net/http-methods/
* https://en.wikipedia.org/wiki/Hypertext_Application_Language
* https://en.wikipedia.org/wiki/HATEOAS
