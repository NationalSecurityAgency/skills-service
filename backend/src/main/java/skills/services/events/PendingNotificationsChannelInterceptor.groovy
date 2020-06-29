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
package skills.services.events

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Lazy
import org.springframework.core.annotation.Order
import org.springframework.lang.Nullable
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component

import java.security.Principal

@Qualifier("WebSocketConfig")
@Lazy
@Component
@Slf4j
@Order(Integer.MAX_VALUE)
class PendingNotificationsChannelInterceptor implements ChannelInterceptor {

    @Lazy
    @Autowired
    SkillEventsService skillEventsService

    @Override
    void afterSendCompletion(Message<?> message, MessageChannel channel, boolean sent, @Nullable Exception ex) {
        if(ex == null) {
            StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class)
            if (StompCommand.CONNECT == accessor.getCommand()) {
                Principal user = accessor.getUser()
                if (user != null && ((Authentication)user).isAuthenticated()) {
                    log.debug("sending any pending notifications to user [${user.getName()}]")
                    try {
                        skillEventsService.identifyPendingNotifications(user.getName())
                    } catch (Exception e) {
                        log.error("unable to notify user [${user.getName()}] of pending notifications", e)
                    }
                } else {
                    log.warn("unable to notify user of pending notifications as there is no Authentication or the user is not yet authenticated")
                }
            }
        }
    }

}
