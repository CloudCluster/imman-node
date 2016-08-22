# CCIO Image Caching and Manipulation Cluster #

**CCIO ImMan** is a caching cluster for images which are stored on Amazon S3.

The images could be resized by using request parameters:

- iw=xxx resize image to the given width
- ih=yyy resize image to the given height

## Starting the cluster ##

```
nohup java -Xmx450m -Dtransport.ip=10.132.49.29 -Dhttp.ip=104.236.17.191 -Dhttp.port=80 -jar imman-node.jar
```

Properties:

- **http.ip** - IP for HTTP Server
- **http.port** - port number for HTTP Server
- **transport.ip** - IP for the Cluster Transport, usually on the private network
- **transport.port** - port number used by the Cluster Transport, 9900 is default port
- **transport.seeds** - seed hosts, comma separated: 10.10.0.1:9900,10.10.0.2:9900

## Configuration files ##

- **/opt/ccio-image-logback.xml** - configures logs 
- **/opt/ccio-image.properties** - application properties

**ccio-image-logback.xml** example: 

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<included>

	<appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
		<encoder>
			<pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
		</encoder>
	</appender>

	<appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
		<file>/opt/logs/ccio-image.log</file>
		<rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
			<fileNamePattern>/opt/logs/ccio-imman.%d{yyyy-MM-dd}.log.zip</fileNamePattern>
			<maxHistory>30</maxHistory>
		</rollingPolicy>
		<encoder>
			<pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
		</encoder>
	</appender>

	<appender name="EMAIL" class="ch.qos.logback.classic.net.SMTPAppender">
		<smtpHost>localhost</smtpHost>
		<!-- Change the emails -->
		<from>no-reply@email.com</from>
		<to>your@email.com</to>
		<subject>[CCIO ImMan] %logger{20} - %m</subject>
		<layout class="ch.qos.logback.classic.html.HTMLLayout" />
		<filter class="ch.qos.logback.classic.filter.ThresholdFilter">
			<level>ERROR</level>
		</filter>
	</appender>

	<logger name="ccio.imman" level="DEBUG" additivity="false">
		<appender-ref ref="STDOUT" />
	</logger>

	<root level="INFO">
		<appender-ref ref="STDOUT" />
		<appender-ref ref="FILE" />
		<appender-ref ref="EMAIL" />
	</root>

</included>
```

**ccio-image.properties** example:

```properties
http.ip=127.0.0.1
http.port=8080

transport.ip=127.0.0.1
transport.port=9900
transport.seeds=127.0.0.1:9900,127.0.0.2:9900

files.space.reserved=1000000000
files.locations=/opt/ccio/store1,/mnt/fldr/store2

secret=SomeRandomSecretTokenForStatAccess
name=ccio-imman

image.width=100,300
image.height=350

aws.s3.access=S3_ACCESS_KEY
aws.s3.secret=S3_SECRET_TOKEN
aws.s3.bucket=S3_BUCKET_NAME
```