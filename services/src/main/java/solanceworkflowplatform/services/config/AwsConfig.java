package solanceworkflowplatform.services.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.EventBridgeClientBuilder;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;

import java.net.URI;

@Configuration
public class AwsConfig {

    @Value("${aws.region:eu-west-1}")
    private String awsRegion;

    @Value("${aws.endpoint:}")
    private String awsEndpoint;

    @Value("${cloud.aws.credentials.access-key:}")
    private String accessKey;

    @Value("${cloud.aws.credentials.secret-key:}")
    private String secretKey;

    private <T extends AwsClientBuilder<?,?>>
    T configureEndpoint(T builder) {
        if (!awsEndpoint.isBlank()) {
            builder.endpointOverride(URI.create(awsEndpoint));
        }
        return builder;
    }

    private AwsCredentialsProvider credentialsProvider() {
        if (!accessKey.isBlank() && !secretKey.isBlank()) {
            // Use static credentials for local development
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey)
            );
        } else {
            // Fall back to default credentials provider chain for production
            return DefaultCredentialsProvider.create();
        }
    }

    @Bean
    public DynamoDbClient dynamoDbClient() {
        DynamoDbClientBuilder b = DynamoDbClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(credentialsProvider());
        return configureEndpoint(b).build();
    }

    @Bean
    public SqsClient sqsClient() {
        SqsClientBuilder b = SqsClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(credentialsProvider());
        return configureEndpoint(b).build();
    }

    @Bean
    public EventBridgeClient eventBridgeClient() {
        EventBridgeClientBuilder b = EventBridgeClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(credentialsProvider());
        return configureEndpoint(b).build();
    }

    @Bean
    public ObjectMapper objectMapper() {
        // registers JSR310 (java.time) module automatically
        return new ObjectMapper()
                .findAndRegisterModules();
    }
}
