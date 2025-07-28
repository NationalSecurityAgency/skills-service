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
package skills.dbupgrade

import org.springframework.http.HttpMethod
import spock.lang.Specification

class UpgradeSafeUrlDeciderSpec extends Specification {

    def "POST/PUT urls annotated with DBUpgradeSafe are allowed"() {
        UpgradeSafeUrlDecider upgradeSafeUrlDecider = new UpgradeSafeUrlDecider()
        upgradeSafeUrlDecider.init()

        expect:

        upgradeSafeUrlDecider.isUrlAllowed(url, method) == allowed

        where:
        url | method | allowed
        "/public/log" | HttpMethod.POST | true
        "/api/validation/description" | HttpMethod.PUT | true
        "/api/validation/name" | HttpMethod.POST | true
        "/api/validation/url" | HttpMethod.PUT | true
        "/app/projectExist" | HttpMethod.POST | true
        "/admin/projects/foo/subjectNameExists" | HttpMethod.POST | true
        "/admin/projects/foo/badgeNameExists" | HttpMethod.POST | true
        "/admin/projects/foo/skillNameExists" | HttpMethod.POST | true
        "/app/badges/name/exists" | HttpMethod.POST | true
        "/admin/projects/bar/skills/catalog/exists/rando" | HttpMethod.POST | true
        "/projects/foo/users/usera/roles/PROJECT_ADMINISTRATOR" | HttpMethod.POST | false
        "/projects/foo/subjects/newSUbject" | HttpMethod.PUT | false
        "/projects/foo/subjects/subj/skills/aSkill" | HttpMethod.POST | false
        "/projects/foo/levels/edit/5" | HttpMethod.POST | false
        "/projects/foo/subjects/subj/levels/edit/3" | HttpMethod.POST | false
        "/projects/foo/catalog/finalize" | HttpMethod.POST | false
        "/root/global/settings/a_setting" | HttpMethod.POST | false
        "/admin/projects/fromProj/skills/aSkill/shared/projects/toProj" | HttpMethod.POST | false
        "/admin/projects/fooo/badge/barrr/skills/bazzz" | HttpMethod.POST | false
        "/admin/projects/foo/approvals/approve" | HttpMethod.POST | false
        "/root/users/aUser/tags/tagvalue" | HttpMethod.POST | false
        "/app/userInfo" | HttpMethod.POST | false
        "/app/users/suggestDashboardUsers" | HttpMethod.POST | true
        "/app/users/projects/foooo/suggestClientUsers" | HttpMethod.POST | true
        "/app/users/suggestClientUsers" | HttpMethod.POST | true
        "/app/users/suggestPkiUsers" | HttpMethod.POST | true
        "/oauth/token" | HttpMethod.POST | true
    }

    def "skill event reporting is identified"() {
        UpgradeSafeUrlDecider upgradeSafeUrlDecider = new UpgradeSafeUrlDecider()
        upgradeSafeUrlDecider.init()

        when:
        QueuedSkillEvent post = upgradeSafeUrlDecider.isSkillEventReport("/api/projects/myProj/skills/mySkill", HttpMethod.POST)
        QueuedSkillEvent put = upgradeSafeUrlDecider.isSkillEventReport("/api/projects/myProj/skills/mySkill", HttpMethod.PUT)

        then:
        post
        put
        post.projectId == "myProj"
        post.skillId == "mySkill"
        post.requestTime
        put.projectId == "myProj"
        put.skillId == "mySkill"
        put.requestTime
    }
}
