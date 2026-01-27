package skills.auth.openai

import groovy.util.logging.Slf4j
import org.springframework.ai.document.MetadataMode
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.openai.OpenAiEmbeddingModel
import org.springframework.ai.openai.OpenAiEmbeddingOptions
import org.springframework.ai.openai.api.OpenAiApi
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.retry.support.RetryTemplate

@Configuration
@Slf4j
class OpenAIEmbeddingModelConfig {

    @Value('#{"${skills.openai.embeddingModel:null}"}')
    String model

    @Autowired
    @Qualifier("embeddingsOpenAiApi")
    OpenAiApi openAiApi

    @Bean
    EmbeddingModel embeddingModel() {
        OpenAiEmbeddingModel openAiEmbeddingModel = new OpenAiEmbeddingModel(
                openAiApi,
                MetadataMode.EMBED,
                OpenAiEmbeddingOptions.builder()
                        .model(model)
                        .build(),
                RetryTemplate.builder()
                        .maxAttempts(1)
                        .build());

        return openAiEmbeddingModel
    }
}
