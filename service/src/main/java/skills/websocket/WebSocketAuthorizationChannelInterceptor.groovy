/**
 * Copyright 2026 SkillTree
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

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Lazy
import org.springframework.core.annotation.Order
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.SimpMessageType
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.AuthenticationTrustResolverImpl
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import skills.auth.UserInfo
import skills.services.admin.InviteOnlyProjectService
import skills.services.admin.UserCommunityService
import skills.storage.model.auth.RoleName
import skills.storage.repos.UserRoleRepo

@Component
@Qualifier('WebSocketConfig')
@Lazy
@Order(0) // After CONNECT token authentication, before subscription side effects.
class WebSocketAuthorizationChannelInterceptor implements ChannelInterceptor {

    @Autowired
    @Lazy
    UserRoleRepo userRoleRepo

    @Autowired
    @Lazy
    InviteOnlyProjectService inviteOnlyProjectService

    @Autowired
    @Lazy
    UserCommunityService userCommunityService

    @Override
    Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor)
        // Spring also generates DISCONNECT after a transport closes or CONNECT fails.
        // It may have no principal and must reach the broker to release session resources.
        if (accessor?.command == StompCommand.DISCONNECT) {
            return message
        }
        Authentication authentication = accessor?.user instanceof Authentication ? (Authentication) accessor.user : null
        if (!authentication?.authenticated || new AuthenticationTrustResolverImpl().isAnonymous(authentication) ||
                !(authentication.principal instanceof UserInfo)) {
            throw new AccessDeniedException('WebSocket authentication is required')
        }

        if (accessor.command == StompCommand.SUBSCRIBE) {
            // Only logical user destinations are permitted, never physical broker queues or wildcards.
            def match = (accessor.destination ?: '') =~ /^\/user\/queue\/([a-zA-Z0-9_-]+)-skill-updates$/
            if (!match.matches()) {
                throw new AccessDeniedException('WebSocket subscription is not allowed')
            }
            String projectId = match.group(1)
            UserInfo user = (UserInfo) authentication.principal
            if (user.proxied && user.proxyingSystemId != projectId) {
                throw new AccessDeniedException('WebSocket subscription is outside the token project')
            }
            if (userCommunityService.isUserCommunityOnlyProject(projectId) &&
                    !userCommunityService.isUserCommunityMember(user.username)) {
                throw new AccessDeniedException('WebSocket project access is denied')
            }
            if (inviteOnlyProjectService.isInviteOnlyProject(projectId)) {
                // HTTP authority loading intentionally omits roles for proxied users and
                // depends on a request URL. STOMP must check the explicit destination project.
                boolean permitted = userRoleRepo.findAllByUserId(user.username.toLowerCase()).any { role ->
                    role.roleName == RoleName.ROLE_SUPER_DUPER_USER ||
                            (role.projectId == projectId && role.roleName in [
                                    RoleName.ROLE_PRIVATE_PROJECT_USER, RoleName.ROLE_PROJECT_ADMIN, RoleName.ROLE_PROJECT_APPROVER])
                }
                if (!permitted) {
                    throw new AccessDeniedException('WebSocket project access is denied')
                }
            }
            return message
        }
        if (accessor.messageType == SimpMessageType.HEARTBEAT || accessor.command in [
                StompCommand.CONNECT, StompCommand.STOMP, StompCommand.UNSUBSCRIBE]) {
            return message
        }
        // This endpoint only delivers server-generated notifications. Clients cannot SEND anywhere.
        throw new AccessDeniedException('WebSocket client messages are not allowed')
    }
}
