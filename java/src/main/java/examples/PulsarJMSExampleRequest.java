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
import java.util.Random;

public class PulsarJMSExampleRequest extends PulsarJMSExampleApplication implements MessageListener {
    // Must be set before initializing the "logger" object.
    private final static String APP_NAME = "PulsarJMSExampleRequest";
    static { System.setProperty("log_file_base_name", getLogFileName(API_TYPE, APP_NAME)); }
    private final static Logger logger = LoggerFactory.getLogger(PulsarJMSExampleRequest.class);

    private static PulsarConnectionFactory connectionFactory;
    private static JMSContext jmsContext;
    private static JMSProducer jmsProducer;
    private static JMSConsumer jmsConsumer;
    private static Queue queueDestination;

    private File iotSensorDataCsvFile;
    public PulsarJMSExampleRequest(String appName, String[] inputParams) {
        super(appName, inputParams);
        addRequiredCommandLineOption("csv","csvFile", true, "IoT sensor data CSV file.");

        logger.info("Starting application: \"" + appName + "\" ...");
    }

    public static void main(String[] args) {
        PulsarJMSExample workshopApp = new PulsarJMSExampleRequest(APP_NAME, args);
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

                // Create an admin queue
                queueDestination = createTempQueueDestination(jmsContext, queueName);
                logger.info("Admin messages queue created: {}", queueDestination.getQueueName());

                // Create a producer for admin queue
                jmsProducer = jmsContext.createProducer();
                jmsProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

                // Create a temporary queue tat the client will listen for responses on and
                // then create a consumer that consumes message from this temp queue
                // This is where you likely need to create a temp queue per client 
                TemporaryQueue tempQueueDest = jmsContext.createTemporaryQueue();
                logger.info("Temp queue created: {}", tempQueueDest.getQueueName());
                jmsConsumer = jmsContext.createConsumer(tempQueueDest);

                //Now create the actual message you want to send
                TextMessage requestMessage = jmsContext.createTextMessage("Hello, Pulsar! : ");


                //This class will handle the messages to the temp queue as well
                jmsConsumer.setMessageListener(this);

                //Set the reply to field to the temp queue you created above, this is the queue the server
                //will respond to
                requestMessage.setJMSReplyTo(tempQueueDest);

                //Set a correlation ID so when you get a response you know which sent message the response is for
                //If there is never more than one outstanding message to the server then the
                //same correlation ID can be used for all the messages...if there is more than one outstanding
                //message to the server you would presumably want to associate the correlation ID with this
                //message somehow...a Map works good
                String correlationId = this.createRandomString();
                requestMessage.setJMSCorrelationID(correlationId);
                jmsProducer.send(queueDestination, requestMessage);
            }
            
            logger.info("Request Reply finished");
        } /*catch (IOException ioException) {
            throw new ConfRuntimeException("Failed to read from the workload data source file! " + ioException.getMessage());
        } catch (InterruptedException intrptException) {
            throw new ConfRuntimeException("Unexpected error with an interruption." + intrptException.getMessage());
        }*/ catch (JMSException jmsException) {
            throw new ConfRuntimeException("Unexpected error when sending or receiving JMS messages to/from a queue! " + jmsException.getMessage());
        }

    }

    private String createRandomString() {
        Random random = new Random(System.currentTimeMillis());
        long randomLong = random.nextLong();
        return Long.toHexString(randomLong);
    }

    public void onMessage(Message message) {
        String messageText = null;
        try {
            if (message instanceof TextMessage) {
                TextMessage textMessage = (TextMessage) message;
                messageText = textMessage.getText();                
                logger.info("Message Text: {}", messageText);                    

            }
        } catch (JMSException jmsException) {
            //Handle the exception appropriately
            throw new ConfRuntimeException("Unexpected error when processing from queue! " + jmsException.getMessage());
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
