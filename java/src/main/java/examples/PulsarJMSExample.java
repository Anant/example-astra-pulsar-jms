/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package examples;

import com.datastax.oss.pulsar.jms.PulsarConnectionFactory;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PulsarJMSExample {
    private final TopicPatternManager topicManager;
    private final ConnectionFactory connectionFactory;
    private Connection connection;
    private Session session;
    private final ScheduledExecutorService executor;

    public PulsarJMSExample() throws Exception {
        // Load configuration
        Properties props = new Properties();
        props.load(getClass().getClassLoader().getResourceAsStream("application.properties"));
        
        // Initialize topic manager
        this.topicManager = new TopicPatternManager();
        
        // Initialize connection factory
        this.connectionFactory = new PulsarConnectionFactory(props);
        
        // Initialize executor for publishing messages
        this.executor = Executors.newSingleThreadScheduledExecutor();
    }

    public void start() throws JMSException {
        // Create connection and session
        connection = connectionFactory.createConnection();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        connection.start();

        // Start publishing messages
        startPublishing();

        // Subscribe to patterns
        subscribeToPattern("*-message-*", "All messages");
        subscribeToPattern("stage-message-*", "Stage messages");
        subscribeToPattern("prod-message-*", "Production messages");
    }

    private void startPublishing() {
        executor.scheduleAtFixedRate(() -> {
            try {
                String topic = topicManager.getRandomTopic();
                MessageProducer producer = session.createProducer(session.createTopic(topic));
                TextMessage message = session.createTextMessage("Hello from " + topic);
                producer.send(message);
                log.info("Sent message to topic: {}", topic);
                producer.close();
            } catch (JMSException e) {
                log.error("Error sending message", e);
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    private void subscribeToPattern(String pattern, String listenerName) throws JMSException {
        List<String> matchingTopics = topicManager.getTopicsByPattern(pattern);
        log.info("Subscribing to {} topics matching pattern: {}", matchingTopics.size(), pattern);

        for (String topic : matchingTopics) {
            MessageConsumer consumer = session.createConsumer(session.createTopic(topic));
            consumer.setMessageListener(new MessageListener() {
                @Override
                public void onMessage(Message message) {
                    try {
                        if (message instanceof TextMessage) {
                            String text = ((TextMessage) message).getText();
                            log.info("{} received on topic {}: {}", listenerName, topic, text);
                        }
                    } catch (JMSException e) {
                        log.error("Error processing message", e);
                    }
                }
            });
        }
    }

    public void stop() {
        try {
            executor.shutdown();
            if (connection != null) {
                connection.close();
            }
        } catch (JMSException e) {
            log.error("Error closing connection", e);
        }
    }

    public static void main(String[] args) {
        try {
            PulsarJMSExample example = new PulsarJMSExample();
            example.start();

            // Keep the application running
            Thread.currentThread().join();
        } catch (Exception e) {
            log.error("Error running example", e);
        }
    }
}