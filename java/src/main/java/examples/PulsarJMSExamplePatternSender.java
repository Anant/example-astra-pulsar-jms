package examples;

import com.datastax.oss.pulsar.jms.PulsarConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PulsarJMSExamplePatternSender {
    private static final Logger log = LoggerFactory.getLogger(PulsarJMSExamplePatternSender.class);
    private final PulsarConnectionFactory factory;
    private final String tenantNamespace;
    private final ScheduledExecutorService executor;
    private final Random random = new Random();
    private final List<String> topicPatterns;
    private Connection connection;
    private Session session;
    private int messageCount = 0;
    private final int maxMessages;

    public PulsarJMSExamplePatternSender(int maxMessages) throws Exception {
        // Load configuration
        Properties props = new Properties();
        props.load(getClass().getClassLoader().getResourceAsStream("application.properties"));
        this.tenantNamespace = props.getProperty("tenantNamespace", "test-bed-nomura/default");
        
        // Load topic patterns from deploy.properties
        Properties deployProps = new Properties();
        deployProps.load(getClass().getClassLoader().getResourceAsStream("deploy.properties"));
        String patterns = deployProps.getProperty("topicBroadcastPatterns");
        this.topicPatterns = Arrays.asList(patterns.split(","));
        
        // Initialize connection factory with Pulsar JMS configuration
        this.factory = new PulsarConnectionFactory(props);
        this.executor = Executors.newSingleThreadScheduledExecutor();
        this.maxMessages = maxMessages;
    }

    public void start() throws JMSException {
        connection = factory.createConnection();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        connection.start();
        startPublishing();
    }

    private void startPublishing() {
        executor.scheduleAtFixedRate(() -> {
            try {
                if (maxMessages > 0 && messageCount >= maxMessages) {
                    stop();
                    return;
                }

                String pattern = topicPatterns.get(random.nextInt(topicPatterns.size()));
                String topicName = pattern.replace("*", String.format("%03d", random.nextInt(1000)));
                String fullTopicName = String.format("persistent://%s/%s", tenantNamespace, topicName);
                
                Destination destination = session.createTopic(fullTopicName);
                MessageProducer producer = session.createProducer(destination);
                
                String messageText = String.format("Message %d to %s", messageCount++, topicName);
                TextMessage message = session.createTextMessage(messageText);
                producer.send(message);
                log.info("Sent message: {} to topic: {}", messageText, fullTopicName);
                
                producer.close();
            } catch (Exception e) {
                log.error("Error publishing message", e);
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    public void stop() {
        executor.shutdown();
        try {
            if (session != null) session.close();
            if (connection != null) connection.close();
        } catch (JMSException e) {
            log.error("Error closing connection", e);
        }
    }

    public static void main(String[] args) throws Exception {
        int maxMessages = args.length > 0 ? Integer.parseInt(args[0]) : 0;
        PulsarJMSExamplePatternSender sender = new PulsarJMSExamplePatternSender(maxMessages);
        sender.start();
        
        if (maxMessages > 0) {
            // Wait for messages to be sent
            Thread.sleep((maxMessages + 1) * 1000);
            sender.stop();
        } else {
            // Keep running until interrupted
            Runtime.getRuntime().addShutdownHook(new Thread(sender::stop));
        }
    }
} 