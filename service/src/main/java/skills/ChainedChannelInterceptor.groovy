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
package skills

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Lazy
import org.springframework.lang.Nullable
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.stereotype.Component

@Slf4j
@Component
class ChainedChannelInterceptor implements ChannelInterceptor{

    @Qualifier("WebSocketConfig")
    @Lazy
    @Autowired
    List<ChannelInterceptor> chainedInterceptors


    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        Message<?> messageToUse = message;
        for (ChannelInterceptor interceptor : chainedInterceptors) {
            Message<?> resolvedMessage = interceptor.preSend(messageToUse, channel);
            if (resolvedMessage == null) {
                String name = interceptor.getClass().getSimpleName();
                if (log.isDebugEnabled()) {
                    log.debug(name + " returned null from preSend, i.e. precluding the send.");
                }
                return null;
            }
            messageToUse = resolvedMessage;
        }
        return messageToUse;
    }

    @Override
    public void afterSendCompletion(Message<?> message, MessageChannel channel, boolean sent, @Nullable Exception ex) {
        for (ChannelInterceptor interceptor : chainedInterceptors) {
            interceptor.afterSendCompletion(message, channel, sent, ex)
        }
    }

    @Override
    public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
        for (ChannelInterceptor interceptor : chainedInterceptors) {
            interceptor.postSend(message, channel, sent);
        }
    }

    @Override
    public boolean preReceive(MessageChannel channel) {
        for (ChannelInterceptor interceptor : chainedInterceptors) {
            if (!interceptor.preReceive(channel)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Message<?> postReceive(Message<?> message, MessageChannel channel) {
        Message<?> messageToUse = message;
        for (ChannelInterceptor interceptor : chainedInterceptors) {
            messageToUse = interceptor.postReceive(messageToUse, channel);
            if (messageToUse == null) {
                return null;
            }
        }
        return messageToUse;
    }

}
