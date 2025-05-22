package examples;

import com.datastax.oss.pulsar.jms.PulsarConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.util.Properties;

public class PulsarJMSExamplePatternReceiver {
    private static final Logger log = LoggerFactory.getLogger(PulsarJMSExamplePatternReceiver.class);
    private final PulsarConnectionFactory factory;
    private final String tenantNamespace;
    private final String pattern;
    private final String listenerName;
    private Connection connection;
    private Session session;

    public PulsarJMSExamplePatternReceiver(String pattern, String listenerName) throws Exception {
        this.pattern = pattern;
        this.listenerName = listenerName;
        
        // Load configuration
        Properties props = new Properties();
        props.load(getClass().getClassLoader().getResourceAsStream("application.properties"));
        this.tenantNamespace = props.getProperty("tenantNamespace", "test-bed-nomura/default");
        
        // Load search pattern from deploy.properties
        Properties deployProps = new Properties();
        deployProps.load(getClass().getClassLoader().getResourceAsStream("deploy.properties"));
        String searchPattern = deployProps.getProperty("topicSearchPattern");
        if (searchPattern != null && !searchPattern.isEmpty()) {
            this.pattern = searchPattern;
        }
        
        // Initialize connection factory with Pulsar JMS configuration
        this.factory = new PulsarConnectionFactory(props);
    }

    public void start() throws JMSException {
        connection = factory.createConnection();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        connection.start();
        subscribeToPattern();
    }

    private void subscribeToPattern() throws JMSException {
        String fullPattern = String.format("persistent://%s/%s", tenantNamespace, pattern);
        log.info("Subscribing to pattern: {} as listener: {}", fullPattern, listenerName);
        
        Destination destination = session.createTopic(fullPattern);
        MessageConsumer consumer = session.createConsumer(destination);
        
        consumer.setMessageListener(message -> {
            try {
                if (message instanceof TextMessage) {
                    String text = ((TextMessage) message).getText();
                    log.info("{} received message: {}", listenerName, text);
                }
            } catch (JMSException e) {
                log.error("Error processing message", e);
            }
        });
    }

    public void stop() {
        try {
            if (session != null) session.close();
            if (connection != null) connection.close();
        } catch (JMSException e) {
            log.error("Error closing connection", e);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: PulsarJMSExamplePatternReceiver <pattern> <listener-name>");
            System.exit(1);
        }
        
        PulsarJMSExamplePatternReceiver receiver = new PulsarJMSExamplePatternReceiver(args[0], args[1]);
        receiver.start();
        
        // Keep running until interrupted
        Runtime.getRuntime().addShutdownHook(new Thread(receiver::stop));
    }
} 