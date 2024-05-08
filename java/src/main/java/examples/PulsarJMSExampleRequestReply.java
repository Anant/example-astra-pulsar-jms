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
package example;

import com.datastax.oss.pulsar.jms.PulsarConnectionFactory;
import example.exception.InvalidParamException;
import example.exception.ConfRuntimeException;
import example.CsvFileLineScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.io.File;
import java.io.IOException;

public class PulsarJMSExampleRequestReply extends PulsarJMSExampleApplication {
    // Must be set before initializing the "logger" object.
    private final static String APP_NAME = "PulsarJMSExampleRequestReply";
    static { System.setProperty("log_file_base_name", getLogFileName(API_TYPE, APP_NAME)); }
    private final static Logger logger = LoggerFactory.getLogger(PulsarJMSExampleSender.class);

    private static PulsarConnectionFactory connectionFactory;
    private static JMSContext jmsContext;
    private static JMSProducer jmsProducer;
    private static JMSConsumer jmsConsumer;
    private static TemporaryQueue queueDestination;

    private File iotSensorDataCsvFile;
    public PulsarJMSExampleRequestReply(String appName, String[] inputParams) {
        super(appName, inputParams);
        addRequiredCommandLineOption("csv","csvFile", true, "IoT sensor data CSV file.");

        logger.info("Starting application: \"" + appName + "\" ...");
    }

    public static void main(String[] args) {
        PulsarJMSExample workshopApp = new PulsarJMSExampleRequestReply(APP_NAME, args);
        int exitCode = workshopApp.runCmdApp();
        System.exit(exitCode);
    }

    @Override
    public void processExtendedInputParams() throws InvalidParamException {
        super.processExtendedInputParams();

        // (Required) CLI option for IoT sensor source file
        iotSensorDataCsvFile = processFileInputParam("csv");
        if ( iotSensorDataCsvFile == null) {
            throw new InvalidParamException("Must provided a valid IoT sensor source data csv file!");
        }
    }

   

    @Override
    public void execute() throws ConfRuntimeException {
        
        // request and reply 
        try {
            if (connectionFactory == null) {
                connectionFactory = createPulsarJmsConnectionFactory();

                if (jmsContext == null) {
                    jmsContext = createJmsContext(connectionFactory);
                    jmsProducer = jmsContext.createProducer();                   
                }

                // Create a request queue
                Queue requestQueue = createQueueDestination(jmsContext, queueName+"_request");
                logger.info("Request queue created: {}", requestQueue.getQueueName());

                // Create a reply queue
                Queue replyQueue = createQueueDestination(jmsContext, queueName+"_reply");
                logger.info("Reply queue created: {}", replyQueue.getQueueName());

                // Create a topic
                Topic topic = createTopicDestination(jmsContext, topicName);
                logger.info("Topic created: {}", topic.topicName());


                // Create a producer for the request queue
                MessageProducer requestProducer = jmsContext.createProducer(requestQueue);

                // Create a producer for the topic
                MessageProducer topicProducer = jmsContext.createProducer(topic);

                // Create a consumer for the reply queue
                MessageConsumer replyConsumer = jmsContext.createConsumer(replyQueue);

                // Create a consumer for the topic
                MessageConsumer topicConsumer = jmsContext.createConsumer(topic);

                // Create a request message
                TextMessage requestMessage = jmsContext.createTextMessage("Hello, Pulsar!");

                // Set the reply queue as the JMSReplyTo
                requestMessage.setJMSReplyTo(replyQueue);

                // Send the request message to the request queue
                requestProducer.send(requestMessage);

                // Receive the reply message from the reply queue
                Message replyMessage = replyConsumer.receive();

                // Process the reply message
                if (replyMessage instanceof TextMessage) {
                    TextMessage textMessage = (TextMessage) replyMessage;
                    System.out.println("Received reply: " + textMessage.getText());
                } else {
                    System.out.println("Received reply: " + replyMessage);
                }


            }
            
            
            logger.info("Request Reply finished");
        } catch (IOException ioException) {
            throw new ConfRuntimeException("Failed to read from the workload data source file! " + ioException.getMessage());
        } catch (JMSException jmsException) {
            throw new ConfRuntimeException("Unexpected error when sending or receiving JMS messages to/from a queue! " + jmsException.getMessage());
        }

    }

    @Override
    public void termCmdApp() {
        try {
            if (jmsContext != null) {
                jmsContext.close();
            }

            if (connectionFactory != null) {
                connectionFactory.close();
            }
        }
        finally {
            logger.info("Terminating application: \"" + appName + "\" ...");
        }
    }
}
