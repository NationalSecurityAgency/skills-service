/**
 * Copyright 2020 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package skills.notify.builders

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine
import skills.storage.model.Notification
import groovy.util.logging.Slf4j

import javax.annotation.PostConstruct

@Service
@Slf4j
class NotificationEmailBuilderManager {

    @Autowired
    ApplicationContext appContext

    @Autowired
    SpringTemplateEngine thymeleafTemplateEngine;

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

    NotificationEmailBuilder.Res build(Notification notification, Formatting formatting) {
        NotificationEmailBuilder builder = lookup.get(notification.type)
        assert builder
        return builder.build(notification, formatting)
    }

}
