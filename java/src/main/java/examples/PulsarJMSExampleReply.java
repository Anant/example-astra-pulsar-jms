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

public class PulsarJMSExampleReply extends PulsarJMSExampleApplication implements MessageListener {
    // Must be set before initializing the "logger" object.
    private final static String APP_NAME = "PulsarJMSExampleReply";
    static { System.setProperty("log_file_base_name", getLogFileName(API_TYPE, APP_NAME)); }
    private final static Logger logger = LoggerFactory.getLogger(PulsarJMSExampleReply.class);

    private static PulsarConnectionFactory connectionFactory;
    private static JMSContext jmsContext;
    private static JMSProducer jmsProducer;
    private static JMSConsumer jmsConsumer;
    private static Queue queueDestination;

    private File iotSensorDataCsvFile;
    public PulsarJMSExampleReply(String appName, String[] inputParams) {
        super(appName, inputParams);
        addRequiredCommandLineOption("csv","csvFile", true, "IoT sensor data CSV file.");

        logger.info("Starting application: \"" + appName + "\" ...");
    }

    public static void main(String[] args) {
        PulsarJMSExample workshopApp = new PulsarJMSExampleReply(APP_NAME, args);
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
        
        try {
            if (connectionFactory == null) {
                connectionFactory = createPulsarJmsConnectionFactory();

                
                if (jmsContext == null) {
                    jmsContext = createJmsContext(connectionFactory);
                    jmsProducer = jmsContext.createProducer();                   
                }
                // Create an admin queue
                queueDestination = createTempQueueDestination(jmsContext, queueName);
                logger.info("Admin messages queue created: {}", queueDestination.getQueueName());

                // Create a producer for admin queue
                jmsProducer = jmsContext.createProducer();
                jmsProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

    
                //Set up a consumer to consume messages off of the admin queue
                jmsConsumer = jmsContext.createConsumer(queueDestination);
                jmsConsumer.setMessageListener(this);
            }
            jmsConsumer.receive();
            logger.info("Waiting for messages: \"" + appName + "\" ...");

        } /*catch (IOException ioException) {
            throw new ConfRuntimeException("Failed to read from the workload data source file! " + ioException.getMessage());
        } catch (InterruptedException intrptException) {
            throw new ConfRuntimeException("Unexpected error with an interruption." + intrptException.getMessage());
        } */ catch (JMSException jmsException) {
            throw new ConfRuntimeException("Unexpected error when sending or receiving JMS messages to/from a queue! " + jmsException.getMessage());
        }

    }


    public String handleProtocolMessage(String messageText) {
        String responseText;
        if ("MyProtocolMessage".equalsIgnoreCase(messageText)) {
            responseText = "I recognize your protocol message";
        } else {
            responseText = "Unknown protocol message: " + messageText;
        }
        
        return responseText;
    }

    public void onMessage(Message message) {
        try {
            TextMessage response = jmsContext.createTextMessage();
            if (message instanceof TextMessage) {
                TextMessage txtMsg = (TextMessage) message;
                String messageText = txtMsg.getText();
                response.setText(handleProtocolMessage(messageText));
            }

            //Set the correlation ID from the received message to be the correlation id of the response message
            //this lets the client identify which message this is a response to if it has more than
            //one outstanding message to the server
            response.setJMSCorrelationID(message.getJMSCorrelationID());

            //Send the response to the Destination specified by the JMSReplyTo field of the received message,
            //this is presumably a temporary queue created by the client
            jmsProducer.send(message.getJMSReplyTo(), response);
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
