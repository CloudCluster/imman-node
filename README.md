# CCIO Image Caching and Manipulation Cluster #

**CCIO ImMan** is a caching cluster for images which are stored on Amazon S3. Here is [more information](https://github.com/CloudCluster/imman-node/wiki) about ImMan.

The images could be resized by providing request parameters:

- iw=xxx resize image to the given width
- ih=yyy resize image to the given height

for example:

```
http://yourserver.com/image.jpg?iw=300
```

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

[ImMan Cluster set up process](https://github.com/CloudCluster/imman-node/wiki/Setup) described on Wiki.

