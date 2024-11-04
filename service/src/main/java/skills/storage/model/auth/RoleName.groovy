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
package skills.storage.model.auth

enum RoleName {
    ROLE_APP_USER('Application'),
    ROLE_PRIVATE_PROJECT_USER('Private'),
    ROLE_PROJECT_ADMIN('Admin'),
    ROLE_SUPERVISOR('Supervisor'),
    ROLE_SUPER_DUPER_USER('Root'),
    ROLE_PROJECT_APPROVER('Approver'),
    ROLE_DASHBOARD_ADMIN_ACCESS('Admin Access'),
    ROLE_QUIZ_ADMIN('Admin'),
    ROLE_QUIZ_READ_ONLY('Read Only'),
    ROLE_ADMIN_GROUP_OWNER('Admin Group Owner'),
    ROLE_ADMIN_GROUP_MEMBER('Admin Group Member');

    String displayName;

    RoleName(String displayName) {
        this.displayName = displayName;
    }
}
