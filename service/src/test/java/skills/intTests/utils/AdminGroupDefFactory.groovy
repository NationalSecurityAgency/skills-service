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
package skills.intTests.utils

class AdminGroupDefFactory {

    static String DEFAULT_ADMINGROUP_NAME = "Test Admin Group"
    static String DEFAULT_ADMINGROUP_ID_PREPEND = DEFAULT_ADMINGROUP_NAME.replaceAll(" ", "")

    static String getDefaultAdminGroupId(int adminGroupNum = 1) {
        DEFAULT_ADMINGROUP_ID_PREPEND + "${adminGroupNum}"
    }

    static String getDefaultAdminGroupName(int adminGroupNum = 1) {
        DEFAULT_ADMINGROUP_NAME + " #${adminGroupNum}"
    }

    static createAdminGroup(int adminGroupNumber = 1) {
        return [adminGroupId: getDefaultAdminGroupId(adminGroupNumber), name: getDefaultAdminGroupName(adminGroupNumber)]
    }

}
