package skills

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties("skills.config")
class UIConfigProperties {
    Map<String,String> ui = [:]
}
