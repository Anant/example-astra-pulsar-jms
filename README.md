# DataStax Apache Pulsar JMS Client - Spring Example

This is a sample project that demonstrates how to use Apache Pulsar as a JMS provider with Spring Boot. It shows how to integrate Pulsar's JMS client with Spring's JMS support to create a robust messaging application.

## Overview

This example demonstrates:
- Integration of Apache Pulsar with Spring Boot using the DataStax Pulsar JMS client
- Configuration of Pulsar JMS connection factory and connection
- Sending and receiving messages using Spring's JMS template
- Message listener implementation using Spring's `@JmsListener` annotation
- Handling of custom message types (Email objects)

## Prerequisites

- Java 8 or higher
- Maven
- Docker (optional, for running Pulsar locally)

## Project Structure

The project consists of several key components:
- `PulsarJMSConfiguration`: Configures the Pulsar JMS connection factory and connection
- `PulsarJMSConfigurationProperties`: Contains configuration properties for Pulsar connection
- `PulsarJMSExampleApplication`: Main application class that demonstrates message sending
- `ExampleListener`: Message listener implementation
- `Email`: Sample message payload class

## Running the Example

### 1. Start Pulsar

You can start Pulsar using Docker:

```bash
docker run -p 8080:8080 -p 6650:6650 apachepulsar/pulsar:latest bin/pulsar standalone
```

Or by starting [Pulsar Standalone](https://pulsar.apache.org/docs/en/standalone/) directly.

### 2. Build and Run

Build the project using Maven:

```bash
mvn clean package -DskipTests
```

Run the application:

```bash
mvn spring-boot:run
```

## How It Works

### Sending Messages

The application uses Spring's `JmsTemplate` to send messages. Messages are sent as serialized Java objects:

```java
JmsTemplate jmsTemplate = context.getBean(JmsTemplate.class);
jmsTemplate.convertAndSend("IN_QUEUE", new Email("info@example.com", "Hello"));
```

### Receiving Messages

Messages are received using Spring's `@JmsListener` annotation:

```java
@Component
public class ExampleListener {
    @JmsListener(destination = "IN_QUEUE", containerFactory = "myFactory")
    public void onMessage(Email email) {
        System.out.println("Received " + email);
    }
}
```

### Configuration

The application is configured to connect to a Pulsar broker running at `http://localhost:8080`. The configuration includes:
- JMS connection factory setup
- Connection pooling
- Message listener container configuration
- Topic/queue mapping

## Dependencies

The project uses:
- Spring Boot 2.4.5
- DataStax Pulsar JMS Client 4.0.1
- Spring Pulsar 1.0.5
- Lombok for reducing boilerplate code

## License

This project is licensed under the terms included in the LICENSE file.