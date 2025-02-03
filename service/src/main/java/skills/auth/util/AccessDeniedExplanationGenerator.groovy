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
package skills.auth.util

import org.springframework.security.access.AccessDeniedException
import skills.auth.inviteOnly.InviteOnlyAccessDeniedException

class AccessDeniedExplanationGenerator {
    public static final String PRIVATE_PROJECT_CODE = "private_project"
    public static final String ACCESS_DENIED_PROJECT_NOT_FOUND = "project_permission"
    public static final String ACCESS_DENIED_QUIZ_NOT_FOUND = "quiz_permission"
    public static final String ACCESS_DENIED_ADMIN_GROUP_NOT_FOUND = "admin_group_permission"
    public static final String GLOBAL_BADGE_NOT_FOUND = "global_badge_permission"

    AccessDeniedExplanation generateExplanation(String requestPath, AccessDeniedException accessDeniedException) {

        if (accessDeniedException instanceof InviteOnlyAccessDeniedException) {
            return new AccessDeniedExplanation(projectId: accessDeniedException.projectId, errorCode: PRIVATE_PROJECT_CODE, explanation: "This Project has been configured with Invite Only access requirements")
        }

        if (requestPath?.startsWith("/admin/projects")) {
            return new AccessDeniedExplanation(errorCode: ACCESS_DENIED_PROJECT_NOT_FOUND, explanation: "You do not have permission to view/manage this Project OR this Project does not exist")
        } else if (requestPath?.startsWith("/supervisor/badges")) {
            return new AccessDeniedExplanation(errorCode: GLOBAL_BADGE_NOT_FOUND, explanation: "You do not have permission to view/manage this Global Badge OR this Global Badge does not exist")
        } else if (requestPath?.startsWith("/admin/quiz-definitions")) {
            return new AccessDeniedExplanation(errorCode: ACCESS_DENIED_QUIZ_NOT_FOUND, explanation: "You do not have permission to view/manage this Quiz OR this Quiz does not exist")
        } else if (requestPath?.startsWith("/admin/admin-group-definitions")) {
            return new AccessDeniedExplanation(errorCode: ACCESS_DENIED_ADMIN_GROUP_NOT_FOUND, explanation: "You do not have permission to view/manage this Admin Group OR this Admin Group does not exist")
        } else {
            return null;
        }
    }
}
