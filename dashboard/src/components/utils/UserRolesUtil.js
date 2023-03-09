/*
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
export default class UserRolesUtil {
  static isReadOnlyProjRole(role) {
    return this.isApproverRole(role);
  }

  static isApproverRole(role) {
    return role && role === 'ROLE_PROJECT_APPROVER';
  }

  static isQuizAdminRole(role) {
    return role && role === 'ROLE_QUIZ_ADMIN';
  }

  static isQuizReadOnlyRole(role) {
    return role && role === 'ROLE_QUIZ_READ_ONLY';
  }
}
