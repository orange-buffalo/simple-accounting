package io.orangebuffalo.simpleaccounting.infra

import com.fasterxml.jackson.annotation.JsonInclude
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JacksonConfig {

    @Bean
    fun includeNullMapValues(): JsonMapperBuilderCustomizer = JsonMapperBuilderCustomizer { builder ->
        builder.changeDefaultPropertyInclusion {
            JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.ALWAYS)
        }
    }
}
