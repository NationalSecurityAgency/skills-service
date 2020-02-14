package skills.services.events

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component

@Component
@Slf4j
class SkillEventPublisher {

    @Autowired
    final SimpMessagingTemplate messagingTemplate;

    void publishSkillUpdate(SkillEventResult result, String userId) {
        log.debug("Reporting user skill for user [$userId], result [$result]")
        messagingTemplate.convertAndSendToUser(userId, "/queue/${result.projectId}-skill-updates", result)
    }
}
