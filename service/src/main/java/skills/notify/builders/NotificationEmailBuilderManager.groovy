package skills.notify.builders

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import skills.storage.model.Notification
import groovy.util.logging.Slf4j

import javax.annotation.PostConstruct

@Service
@Slf4j
class NotificationEmailBuilderManager {

    @Autowired
    ApplicationContext appContext

    private final Map<String, NotificationEmailBuilder> lookup = [:]

    @PostConstruct
    void init() {
        Collection<NotificationEmailBuilder> loadedBuilders = appContext.getBeansOfType(NotificationEmailBuilder).values()
        loadedBuilders.each { NotificationEmailBuilder builder ->
            assert !lookup.get(builder.getId()), "Found more than 1 builder with the same type [${builder.type}]"
            lookup.put(builder.getId(), builder)
        }
        log.info("Loaded [${lookup.size()}] builders: ${lookup.keySet().toList()}")
    }

    NotificationEmailBuilder.Res build(Notification notification) {
        NotificationEmailBuilder builder = lookup.get(notification.type)
        assert builder
        return builder.build(notification)
    }
}
