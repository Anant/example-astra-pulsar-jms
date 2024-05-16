# JMS Examples

Right now have done work on the java app. So everything is inside `java` 

```
cd java
```

Before starting, make sure to change to the java directory and download `client.conf` that you get from the astra streaming dashboard and place it to the `java` directory.

# Steps 

Download client.conf from Astra streaming dashboard 

```
./_bash/deploy.sh -cc ./client.conf 
```

Run the consumer (persistent topics)
```
./_bash/runConsumer.sh -cc ./client.conf -n 2 -t test-bed-nomura/default/test-topic-1
```
 
Run the producer (persistent topics)
```
./_bash/runProducer.sh -cc ./client.conf -n 2 -t test-bed-nomura/default/test-topic-1
```


Run the processor - send/receive (persistent topics but temporary queues)

```
./_bash/runProcessor.sh -cc ./client.conf -n 2 -t test-bed-nomura/default/test-topic-1
```


Run the reply / request (non persistent topics for queues + temporary queues)

Run the replier
```
./_bash/runRequestReply.sh -cc ./client.conf -n 2 -t test-bed-nomura/default/test-topic-1 -q test-bed-nomura/default/test-queue-1 -m rep
```

Run the requester
```
./_bash/runRequestreply.sh -cc ./client.conf -n 2 -t test-bed-nomura/default/test-topic-1 -q test-bed-nomura/default/test-queue-1 -m req
```

