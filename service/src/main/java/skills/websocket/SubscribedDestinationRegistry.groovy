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
package skills.websocket

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.context.ApplicationListener
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import org.springframework.web.socket.messaging.AbstractSubProtocolEvent
import org.springframework.web.socket.messaging.SessionDisconnectEvent
import org.springframework.web.socket.messaging.SessionSubscribeEvent

import java.security.Principal
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.ConcurrentSkipListSet
import java.util.function.BiFunction
import java.util.function.Function

@Slf4j
@Component
@CompileStatic
class SubscribedDestinationRegistry implements ApplicationListener<AbstractSubProtocolEvent> {

    private static final Function<String, ConcurrentSkipListSet<String>> skipListCreator = new Function<String, ConcurrentSkipListSet<String>>() {
        @Override
        ConcurrentSkipListSet<String> apply(String s) {
            return new ConcurrentSkipListSet<String>()
        }
    }

    private ConcurrentMap<String, ConcurrentSkipListSet<String>> sessionToDestinationRegistry = new ConcurrentHashMap<>()
    private ConcurrentMap<String, ConcurrentSkipListSet<String>> userToSessionRegistry = new ConcurrentHashMap<>()

    void add(String user, String sessionId, String destination) {
        log.debug("user [{}] subscribed to [{}}]", user, destination)
        ConcurrentSkipListSet<String> destinations = sessionToDestinationRegistry.computeIfAbsent(sessionId, skipListCreator)
        destinations.add(destination)
        ConcurrentSkipListSet<String> sessions = userToSessionRegistry.computeIfAbsent(user, skipListCreator)
        sessions.add(sessionId)
    }

    void remove(String user, String sessionId) {
        log.debug("user [{}] session [{}}] removed", user, sessionId)
        userToSessionRegistry.computeIfPresent(user, new BiFunction<String, ConcurrentSkipListSet<String>, ConcurrentSkipListSet<String>>() {
            @Override
            ConcurrentSkipListSet<String> apply(String s, ConcurrentSkipListSet<String> sessions) {
                sessions.remove(sessionId)
                return sessions.isEmpty() ? null : sessions
            }
        })

        sessionToDestinationRegistry.remove(sessionId)
    }

    List<String> getAllDestinationsForUser(String user) {
        ConcurrentSkipListSet<String> sessions = userToSessionRegistry.get(user)
        List<String> destinations = []
        sessions?.each {
            ConcurrentSkipListSet<String> d = sessionToDestinationRegistry.get(it)
            destinations.addAll(d)
        }

        return destinations
    }

    @Override
    void onApplicationEvent(AbstractSubProtocolEvent event) {
        if (event instanceof SessionDisconnectEvent) {
            StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.message)
            Principal user = accessor.getUser()
            if(user && ((Authentication)user).isAuthenticated()){
                remove(user.getName(), accessor.getSessionId())
            }
        } else if (event instanceof SessionSubscribeEvent) {
            StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.message)
            Principal user = accessor.getUser()
            if(user && ((Authentication)user).isAuthenticated()){
                add(user.getName(), accessor.getSessionId(), accessor.getDestination())
            }
        }
    }
}
