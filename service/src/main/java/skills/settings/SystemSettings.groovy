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
package skills.settings

import groovy.transform.Canonical


@Canonical
class SystemSettings {

    String publicUrl
    //iso 8601 period/duration string, e.g., PT2H30M45S
    String resetTokenExpiration
    //from address used in all outgoing emails from the system
    String fromEmail
    //html and in-line css to display a custom header in the dashboard application
    String customHeader
    //html and in-line css to display a custom footer in the dashboard application
    String customFooter
    //markdown user agreement that will be displayed to Dashboard users on their first access of the Dashboard
    String userAgreement
    //hash of the userAgreement to trigger redisplay to users on change
    String userAgreemmentVersion
}
