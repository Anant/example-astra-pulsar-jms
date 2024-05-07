

# Steps 

Download client.conf from Astra streaming dashboard 

```
./_bash/deploy.sh -cc ./client.conf 
```

Run the consumer 
```
./_bash/runConsumer.sh -cc ./client.conf -n 2 -t test-bed-nomura/default/test-topic-1
```

Run the producer
```
./_bash/runProducer.sh -cc ./client.conf -n 2 -t test-bed-nomura/default/test-topic-1
```