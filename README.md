# CCIO Image Caching and Manipulation Cluster #

*CCIO Image* is a cluster for images which stored on Amazon S3.

The images could be resized by using request parameters:

- iw=xxx resize image to the given width
- ih=yyy resize image to the given height

## Starting the cluster ##

```
nohup java -Xmx450m -Djgroups.bind_addr=10.132.49.29 -Dhttp.ip=104.236.17.191 -Dhttp.port=80 -jar ccio-image.jar
```

Properties:

- *http.ip* - IP for HTTP Server
- *http.port* - port number for HTTP Server
- *jgroups.bind_addr* - IP for Cluster Transport
- *jgroups.tcpping.initial_hosts* - seed hosts, comma separated: 10.10.0.1[7800],10.10.0.2[7800]

Default transport port is *7800*

## Configuration files ##

- */opt/ccio-image-cluster.xml* - configures transport (port, seed hosts)
- */opt/ccio-image-logback.xml* - configures logs 
- */opt/ccio-image.properties* - application properties