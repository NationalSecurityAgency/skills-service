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
package skills.auth.pki

import org.springframework.beans.factory.annotation.Autowired
import skills.auth.UserAuthService
import skills.auth.UserInfo
import skills.intTests.utils.DefaultIntSpec
import skills.storage.model.UserTag
import skills.storage.repos.UserAttrsRepo
import skills.storage.repos.UserTagRepo

import java.util.concurrent.atomic.AtomicInteger

class PkiUserDetailsServiceSpecs extends DefaultIntSpec {

    @Autowired
    UserAuthService userAuthService

    @Autowired
    UserAttrsRepo userAttrsRepo

    @Autowired
    UserTagRepo userTagRepo

    def "handle loading users concurrently"() {

        PkiUserLookup pkiUserLookup = Mock(PkiUserLookup)
        pkiUserLookup.lookupUserDn(_) >> { String dn ->
            new UserInfo(
                    firstName: "First",
                    lastName: "Last",
                    nickname: "",
                    username: dn,
                    usernameForDisplay: dn,
                    userDn: dn,
                    userTags: [Organization : "XYZ"],
            )
        }
        int numThreads = 8
        AtomicInteger exceptioncount = new AtomicInteger()
        List<String> userIdsToSave = (0..100).collect { "User${it}".toString() }
        when:
        List<Thread> threads = (1..numThreads).collect {
            Thread.start {
                PkiUserDetailsService pkiUserDetailsService = new PkiUserDetailsService(userAuthService: userAuthService, pkiUserLookup: pkiUserLookup)
                userIdsToSave.each {
                    try {
                        pkiUserDetailsService.loadUserByUsername(it)
                    } catch (Throwable t){
                        exceptioncount.incrementAndGet()
                    }
                }
            }
        }
        threads.each {
            it.join(5000)
        }

        then:
        List<String> allDbUserIds = userAttrsRepo.findAll().collect({it.userId})
        userIdsToSave.each { String userIdToSearch ->
            assert allDbUserIds.contains(userIdToSearch.toLowerCase()), "[$userIdToSearch] userId does not exist in UserAttrs table"
            List<String> foundsIds = allDbUserIds.findAll({ userIdToSearch.equalsIgnoreCase(it)})
            assert foundsIds.size() == 1, "[$userIdToSearch] userId has more than 1 entry"
            List<UserTag> foundUserTags = userTagRepo.findAllByUserId(userIdToSearch?.toLowerCase())
            assert foundUserTags.size() == 1, "[$userIdToSearch] userId has more than 1 userTag entry"
        }

        exceptioncount.get() == 0
    }
}
