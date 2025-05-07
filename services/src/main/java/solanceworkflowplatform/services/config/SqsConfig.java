package solanceworkflowplatform.services.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;

import jakarta.annotation.PostConstruct;

@Configuration
public class SqsConfig {
    private static final Logger logger = LoggerFactory.getLogger(SqsConfig.class);

    private final SqsClient sqsClient;
    private final String queueName;

    public SqsConfig(
            SqsClient sqsClient,
            @Value("${sqs.queue.workflow.name}") String queueName) {
        this.sqsClient = sqsClient;
        this.queueName = queueName;
    }

    @PostConstruct
    public void init() {
        logger.info("Initializing SQS configuration with queue name: {}", queueName);
        ensureQueueExists();
        logger.info("SQS configuration initialized successfully");
    }

    /**
     * Ensures that the SQS queue exists, creating it if necessary.
     * This is particularly important for local development with LocalStack.
     */
    private void ensureQueueExists() {
        try {
            // Try to get the queue URL to check if it exists
            String queueUrl = sqsClient.getQueueUrl(GetQueueUrlRequest.builder()
                    .queueName(queueName)
                    .build())
                    .queueUrl();
            logger.info("SQS queue '{}' already exists with URL: {}", queueName, queueUrl);
        } catch (QueueDoesNotExistException e) {
            // Queue doesn't exist, create it
            logger.info("SQS queue '{}' does not exist. Creating it now...", queueName);
            try {
                String queueUrl = sqsClient.createQueue(CreateQueueRequest.builder()
                        .queueName(queueName)
                        .build())
                        .queueUrl();
                logger.info("SQS queue '{}' created successfully with URL: {}", queueName, queueUrl);
            } catch (Exception ex) {
                logger.error("Failed to create SQS queue '{}': {}", queueName, ex.getMessage(), ex);
                throw ex;
            }
        } catch (Exception e) {
            logger.error("Error checking/creating SQS queue '{}': {}", queueName, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Periodically checks the status of the SQS queue and logs relevant information.
     * This is useful for debugging SQS issues.
     */
    @Scheduled(fixedRate = 60000, initialDelay = 10000) // Check every minute, with initial delay of 10 seconds
    public void checkQueueStatus() {
        logger.info("Checking SQS queue status for queue: {}", queueName);
        try {
            // Get the queue URL
            String queueUrl = sqsClient.getQueueUrl(GetQueueUrlRequest.builder()
                    .queueName(queueName)
                    .build())
                    .queueUrl();

            // Get queue attributes
            var attributes = sqsClient.getQueueAttributes(GetQueueAttributesRequest.builder()
                    .queueUrl(queueUrl)
                    .attributeNames(
                            QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES,
                            QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE,
                            QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_DELAYED)
                    .build())
                    .attributes();

            logger.info("SQS queue '{}' status - URL: {}, Messages available: {}, Messages in flight: {}, Messages delayed: {}",
                    queueName,
                    queueUrl,
                    attributes.getOrDefault(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES.toString(), "0"),
                    attributes.getOrDefault(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE.toString(), "0"),
                    attributes.getOrDefault(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_DELAYED.toString(), "0"));
        } catch (Exception e) {
            logger.warn("Failed to check status of SQS queue '{}': {}", queueName, e.getMessage());
        }
    }
}
