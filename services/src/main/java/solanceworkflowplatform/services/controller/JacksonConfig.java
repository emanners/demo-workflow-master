package solanceworkflowplatform.services.controller;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

@Configuration
public class JacksonConfig {
  @Bean
  public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
    return builder -> {
      builder.modules(new JavaTimeModule());
      builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

      // Add custom deserializer for Instant
      SimpleModule instantModule = new SimpleModule();
      instantModule.addDeserializer(Instant.class, new JsonDeserializer<Instant>() {
        @Override
        public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
          String dateString = p.getText().trim();
          if (dateString.isEmpty()) {
            return null;
          }
          try {
            return Instant.parse(dateString);
          } catch (Exception e) {
            throw new IOException("Failed to parse Instant: " + dateString, e);
          }
        }
      });
      builder.modules(instantModule);
    };
  }
}
