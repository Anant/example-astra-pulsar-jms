# DataStax Apache Pulsar JMS Client Examples

This repository contains example projects demonstrating how to use Apache Pulsar as a JMS provider. It includes two implementations:
1. A pure Java implementation (current, production-ready)
2. A Spring Boot implementation (work in progress)

## Overview

These examples demonstrate:
- Integration of Apache Pulsar with JMS using the DataStax Pulsar JMS client
- Configuration of Pulsar JMS connection factory and connection
- Sending and receiving messages using JMS
- Message listener implementation
- Handling of custom message types
- Support for both Astra Streaming and standalone Pulsar deployments

## Project Structure

The repository is organized into two main directories:

### Java Implementation (`/java`)
This is the current, production-ready implementation that includes:
- Pure Java-based JMS client implementation
- Support for both Astra Streaming and standalone Pulsar
- Comprehensive deployment scripts
- Example patterns for different messaging scenarios
- Configuration management for different environments

### Spring Implementation (`/spring`)
This is a work-in-progress implementation that demonstrates:
- Integration with Spring Boot
- Spring's JMS template usage
- Spring's `@JmsListener` annotation
- Spring-based configuration

## Prerequisites

- Java 8 or higher
- Maven
- Docker (optional, for running Pulsar locally)
- Astra Streaming account (optional, for cloud deployment)

## Running the Examples

### Java Implementation

1. Configure your environment:
   - For Astra Streaming: Set up your `client.conf` with your Astra credentials
   - For standalone Pulsar: Use the default configuration

2. Build the project:
```bash
cd java
mvn clean package
```

3. Run the example:
```bash
./_bash/deploy.sh -cc <path-to-client.conf>
```

### Spring Implementation (Work in Progress)

1. Start Pulsar (if using standalone):
```bash
docker run -p 8080:8080 -p 6650:6650 apachepulsar/pulsar:latest bin/pulsar standalone
```

2. Build and run:
```bash
cd spring
mvn clean package
mvn spring-boot:run
```

## Configuration

### Java Implementation
The Java implementation supports both Astra Streaming and standalone Pulsar through:
- `client.conf`: Connection configuration
- `deploy.properties`: Topic and namespace configuration
- Environment-specific settings

### Spring Implementation
The Spring implementation uses:
- `application.properties` for configuration
- Spring's auto-configuration capabilities
- JMS connection factory setup

## Dependencies

### Java Implementation
- DataStax Pulsar JMS Client 4.0.1
- SLF4J for logging
- JUnit for testing

### Spring Implementation
- Spring Boot 2.4.5
- DataStax Pulsar JMS Client 4.0.1
- Spring Pulsar 1.0.5
- Lombok

## License

This project is licensed under the terms included in the LICENSE file.