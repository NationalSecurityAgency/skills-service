package skills.services.settings

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties("skills.store")
class DefaultSettingsToInit {
    Map settings
}
