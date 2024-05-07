

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


Run the producer (persistent topics but temporary queues)

```
./_bash/runProcessor.sh -cc ./client.conf -n 2 -t test-bed-nomura/default/test-topic-1
```