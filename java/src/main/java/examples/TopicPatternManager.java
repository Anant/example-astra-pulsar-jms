package examples;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.IOException;

public class TopicPatternManager {
    private final List<String> allTopics = new ArrayList<>();
    private final Random random = new Random();
    private final int topicsPerPattern;
    private final String coreTopics;

    public TopicPatternManager() throws IOException {
        Properties props = new Properties();
        props.load(new FileInputStream("deploy.properties"));
        
        this.coreTopics = props.getProperty("coreTopics", "test-topic-1,test-message-*,stage-message-*,prod-message-*");
        this.topicsPerPattern = Integer.parseInt(props.getProperty("topicsPerPattern", "50"));
        
        initializeTopics();
    }

    private void initializeTopics() {
        String[] patterns = coreTopics.split(",");
        for (String pattern : patterns) {
            if (pattern.contains("*")) {
                // Generate topics for wildcard patterns
                String basePattern = pattern.replace("*", "");
                List<String> generatedTopics = IntStream.range(1, topicsPerPattern + 1)
                    .mapToObj(i -> basePattern + i)
                    .collect(Collectors.toList());
                allTopics.addAll(generatedTopics);
            } else {
                // Add exact topic
                allTopics.add(pattern);
            }
        }
    }

    public String getRandomTopic() {
        return allTopics.get(random.nextInt(allTopics.size()));
    }

    public List<String> getTopicsByPattern(String pattern) {
        return allTopics.stream()
            .filter(topic -> topic.matches(pattern.replace("*", ".*")))
            .collect(Collectors.toList());
    }

    public List<String> getAllTopics() {
        return new ArrayList<>(allTopics);
    }

    public int getTotalTopicCount() {
        return allTopics.size();
    }
} 