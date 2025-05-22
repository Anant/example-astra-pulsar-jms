# JMS Examples

This directory contains various examples demonstrating different JMS patterns with Apache Pulsar.

## Setup

1. Change to the java directory:
```bash
cd java
```

2. Download `client.conf` from the Astra Streaming dashboard and place it in the `java` directory.

3. Deploy the configuration:
```bash
./_bash/deploy.sh -cc ./client.conf 
```

## Examples

### 1. Basic Producer/Consumer
Demonstrates basic message publishing and consumption using persistent topics.

Run the consumer:
```bash
./_bash/runConsumer.sh -cc ./client.conf -n 2 -t test-bed-nomura/default/test-topic-1
```

Run the producer:
```bash
./_bash/runProducer.sh -cc ./client.conf -n 2 -t test-bed-nomura/default/test-topic-1
```

### 2. Message Processor
Demonstrates a processor that both sends and receives messages using persistent topics and temporary queues.

```bash
./_bash/runProcessor.sh -cc ./client.conf -n 2 -t test-bed-nomura/default/test-topic-1
```

### 3. Request/Reply Pattern
Demonstrates the request/reply pattern using non-persistent topics for queues and temporary queues.

Run the replier:
```bash
./_bash/runRequestReply.sh -cc ./client.conf -n 2 -t test-bed-nomura/default/test-topic-1 -q test-bed-nomura/default/test-queue-1 -m rep
```

Run the requester:
```bash
./_bash/runRequestReply.sh -cc ./client.conf -n 2 -t test-bed-nomura/default/test-topic-1 -q test-bed-nomura/default/test-queue-1 -m req
```

### 4. Wildcard Topic Pattern (New!)
Demonstrates publishing to multiple topics and subscribing using wildcard patterns.

First, start one or more receivers with different patterns:
```bash
# Start a receiver for all messages
java -cp target/pulsar-jms-example-1.0-SNAPSHOT.jar examples.PulsarJMSExamplePatternReceiver "*-message-*" "All messages"

# Start a receiver for stage messages
java -cp target/pulsar-jms-example-1.0-SNAPSHOT.jar examples.PulsarJMSExamplePatternReceiver "stage-message-*" "Stage messages"

# Start a receiver for production messages
java -cp target/pulsar-jms-example-1.0-SNAPSHOT.jar examples.PulsarJMSExamplePatternReceiver "prod-message-*" "Production messages"
```

Then start the sender to publish messages:
```bash
# Send 100 messages
java -cp target/pulsar-jms-example-1.0-SNAPSHOT.jar examples.PulsarJMSExamplePatternSender 100

# Or send messages indefinitely
java -cp target/pulsar-jms-example-1.0-SNAPSHOT.jar examples.PulsarJMSExamplePatternSender
```

The wildcard pattern example demonstrates:
- Publishing to multiple topics based on patterns
- Subscribing to topics using wildcard patterns
- Dynamic topic generation and management
- Pattern-based message routing

### 5. Spring Integration (Work in Progress)
A Spring Boot example demonstrating JMS integration with Pulsar is available in the `spring` directory. This example is currently under development and will demonstrate:
- Spring JMS configuration with Pulsar
- Message conversion and serialization
- Transaction support
- Integration with Spring's messaging abstractions

## Configuration

All examples use the following configuration files:
- `client.conf`: Connection configuration from Astra Streaming
- `deploy.properties`: Application-specific configuration
- `application.properties`: Spring-specific configuration (for Spring example)

## Building

To build all examples:
```bash
mvn clean package
```

The build will create a single JAR file containing all examples.
