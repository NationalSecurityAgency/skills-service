package skills.storage;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * This @EnableJpaAuditing is required to support @CreatedDate and @LastModifiedDate annotations
 */
@Configuration
@EnableJpaAuditing()
public class JpaConfig {

}
