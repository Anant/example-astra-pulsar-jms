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

public class PulsarJMSExampleProcessor extends PulsarJMSExampleApplication {
    // Must be set before initializing the "logger" object.
    private final static String APP_NAME = "PulsarJMSExampleProcessor";
    static { System.setProperty("log_file_base_name", getLogFileName(API_TYPE, APP_NAME)); }
    private final static Logger logger = LoggerFactory.getLogger(PulsarJMSExampleSender.class);

    private static PulsarConnectionFactory connectionFactory;
    private static JMSContext jmsContext;
    private static JMSProducer jmsProducer;
    private static JMSConsumer jmsConsumer;
    private static TemporaryQueue queueDestination;

    private File iotSensorDataCsvFile;
    public PulsarJMSExampleProcessor(String appName, String[] inputParams) {
        super(appName, inputParams);
        addRequiredCommandLineOption("csv","csvFile", true, "IoT sensor data CSV file.");

        logger.info("Starting application: \"" + appName + "\" ...");
    }

    public static void main(String[] args) {
        PulsarJMSExample workshopApp = new PulsarJMSExampleProcessor(APP_NAME, args);
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


    private TemporaryQueue sendMessagesToTempTopic(){
        // produce messages 
        try {
            if (connectionFactory == null) {
                connectionFactory = createPulsarJmsConnectionFactory();

                if (jmsContext == null) {
                    jmsContext = createJmsContext(connectionFactory);
                    jmsProducer = jmsContext.createProducer();
                }

                if (queueDestination == null) {
                    //queueDestination = createQueueDestination(jmsContext, topicName);
                    queueDestination = jmsContext.createTemporaryQueue(); //createTempQueueDestination(jmsContext, topicName);
                    logger.info("Temporary queue created: {}", queueDestination.getQueueName());
                }
            }
            

            assert (iotSensorDataCsvFile != null);
            CsvFileLineScanner csvFileLineScanner = new CsvFileLineScanner(iotSensorDataCsvFile);

            boolean isTitleLine = true;
            String titleLine = "";
            int msgSent = 0;
            if (numMsg == -1) {
                numMsg = Integer.MAX_VALUE;
            }

            while (csvFileLineScanner.hasNextLine()) {
                String csvLine = csvFileLineScanner.getNextLine();
                // Skip the first line which is a title line
                if (!isTitleLine) {
                    if (msgSent < numMsg) {
                        jmsProducer.send(queueDestination, csvLine);
                        logger.info("IoT sensor data sent to queue {} [{}] {}",
                                queueDestination.getQueueName(),
                                msgSent,
                                csvLine);
                        msgSent++;
                    } else {
                        return queueDestination;
                    }
                } else {
                    isTitleLine = false;
                    titleLine = csvLine;
                }
            }
            logger.info("Temporary queue being returned: {}", queueDestination.getQueueName());
            return queueDestination;
        } catch (IOException ioException) {
            throw new ConfRuntimeException("Failed to read from the workload data source file! " + ioException.getMessage());
        }
        catch (JMSException jmsException) {
            throw new ConfRuntimeException("Unexpected error when sending JMS messages to a queue! " + jmsException.getMessage());
        }
    }

    private void receiveMessagesFromTempTopic(TemporaryQueue queueDestination){

        try {
            logger.info("Temporary queue being used to consume: {}", queueDestination.getQueueName());
            if (connectionFactory == null) {
                
                connectionFactory = createPulsarJmsConnectionFactory();

                if (jmsContext == null) {
                    jmsContext = createJmsContext(connectionFactory);
                }

                //if (queueDestination == null) {
                    //queueDestination = createQueueDestination(jmsContext, topicName);
                    //queueDestination = createTempQueueDestination(jmsContext, topicName);
                

                if (jmsConsumer == null) {
                    jmsConsumer = jmsContext.createConsumer(queueDestination);
                }
                //}
            }

            int msgRecvd = 0;
            if (numMsg == -1) {
                numMsg = Integer.MAX_VALUE;
            }

            while (msgRecvd < numMsg) {
                Message message = jmsConsumer.receive();
                logger.info("Message received from topic {}: value={}",
                        queueDestination.getQueueName(),
                        message.getBody(String.class));
                msgRecvd++;
            }
        }
        catch (JMSException jmsException) {
            throw new ConfRuntimeException("Unexpected error when receiving JMS messages from a queue! " + jmsException.getMessage());
        }
    }

    @Override
    public void execute() throws ConfRuntimeException {
        //queueDestination = sendMessagesToTempTopic();
        //receiveMessagesFromTempTopic(queueDestination);

        // produce and consume messages 
        try {
            if (connectionFactory == null) {
                connectionFactory = createPulsarJmsConnectionFactory();

                if (jmsContext == null) {
                    jmsContext = createJmsContext(connectionFactory);
                    jmsProducer = jmsContext.createProducer();                   
                }

                if (queueDestination == null) {
                    //queueDestination = createQueueDestination(jmsContext, topicName);
                    queueDestination = jmsContext.createTemporaryQueue(); //createTempQueueDestination(jmsContext, topicName);
                    logger.info("Temporary queue created: {}", queueDestination.getQueueName());
                }
            }
            jmsConsumer = jmsContext.createConsumer(queueDestination);

            assert (iotSensorDataCsvFile != null);
            CsvFileLineScanner csvFileLineScanner = new CsvFileLineScanner(iotSensorDataCsvFile);

            boolean isTitleLine = true;
            String titleLine = "";
            int msgSent = 0;
            int msgRecvd = 0;
            if (numMsg == -1) {
                numMsg = Integer.MAX_VALUE;
            }

            while (csvFileLineScanner.hasNextLine()) {
                String csvLine = csvFileLineScanner.getNextLine();
                // Skip the first line which is a title line
                if (!isTitleLine) {
                    if (msgSent < numMsg) {
                        jmsProducer.send(queueDestination, csvLine);
                        logger.info("IoT sensor data sent to queue {} [{}] {}",
                                queueDestination.getQueueName(),
                                msgSent,
                                csvLine);
                        msgSent++;
                    } else {
                        break;
                    }
                } else {
                    isTitleLine = false;
                    titleLine = csvLine;
                }
            }

            while (msgRecvd < numMsg) {
                Message message = jmsConsumer.receive();
                logger.info("Message received from topic {}: value={}",
                        queueDestination.getQueueName(),
                        message.getBody(String.class));
                msgRecvd++;
            }
            
            logger.info("Temporary queue being used: {}", queueDestination.getQueueName());
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