package com.visionselect.backend.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson ObjectMapper customization via {@link Jackson2ObjectMapperBuilderCustomizer}.
 *
 * <p>Using the builder customizer (rather than registering an {@code @Primary
 * ObjectMapper} bean) ensures that Spring Boot's auto-configuration machinery
 * applies these settings to the shared {@code ObjectMapper} used by
 * {@code MappingJackson2HttpMessageConverter}, Spring MVC's argument
 * resolvers, and any other component that relies on the auto-configured mapper.
 *
 * <p>Key setting: {@code FAIL_ON_UNKNOWN_PROPERTIES = true}.
 * DTOs annotated with {@code @JsonIgnoreProperties(ignoreUnknown = false)}
 * rely on this global setting to actually reject unknown fields — the
 * annotation alone is a statement of intent; this property is what enforces it.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> builder
                // Reject unknown JSON properties globally.
                // DTOs with @JsonIgnoreProperties(ignoreUnknown = false)
                // rely on this to actually fail on unknown fields.
                .featuresToEnable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                // Serialize Instant/LocalDate etc. as ISO-8601 strings, not epoch arrays.
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
