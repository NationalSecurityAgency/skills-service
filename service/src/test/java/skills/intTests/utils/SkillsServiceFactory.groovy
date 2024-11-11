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

import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.stereotype.Component
import skills.storage.model.UserAttrs
import skills.storage.repos.UserAttrsRepo

@Component
class SkillsServiceFactory {

    @Autowired(required=false)
    MockUserInfoService mockUserInfoService

    @Autowired
    WaitForAsyncTasksCompletion waitForAsyncTasksCompletion

    @Autowired(required=false)
    CertificateRegistry certificateRegistry

    @Autowired
    UserAttrsRepo userAttrsRepo

    private UserUtil userUtil

    @PostConstruct
    def init(){
        userUtil = new UserUtil(certificateRegistry: certificateRegistry)
    }

    SkillsService createService(
            String username,
            String password,
            String firstName,
            String lastName,
            String url){

        boolean pkiEnabled = mockUserInfoService != null
        if (pkiEnabled) {
            url = url.replace("http://", "https://")
        }

        SkillsService.UseParams userParams = new SkillsService.UseParams(
                username: username,
                password: password,
                firstName: firstName,
                lastName: lastName
        )
        SkillsService res = new SkillsService(userParams, url, pkiEnabled != null ? certificateRegistry : null)
        res.waitForAsyncTasksCompletion = waitForAsyncTasksCompletion
        return res
    }

    SkillsService createService(
            SkillsService.UseParams userParams,
            String url){

        boolean pkiEnabled = mockUserInfoService != null
        if (pkiEnabled) {
            url = url.replace("http://", "https://")
        }

        SkillsService res = new SkillsService(userParams, url, pkiEnabled != null ? certificateRegistry : null)
        res.waitForAsyncTasksCompletion = waitForAsyncTasksCompletion
        return res
    }


    /*
     * Returns N number of random users. NOTE - if tests are run in pki mode,
     * N must be less than or equal to the number of test p12 certificates available,
     * otherwise an exception will occur.
     *
     * @param numUsers number of random users - must be less than or equal to the number
     * of test p12 certificates available if in pki mode
     * @return
     */
    List<String>  getRandomUsers(int numUsers, boolean createEmail = true, List<String> exclude=[DefaultIntSpec.DEFAULT_ROOT_USER_ID, SkillsService.UseParams.DEFAULT_USER_NAME]) {
        //create email addresses for the users automatically?
        List<String> userIds =  userUtil.getUsers(numUsers+exclude.size())
        exclude.each { userToExclude ->
            String idToRemove = userIds.find { it.equalsIgnoreCase(userToExclude) }
            userIds.remove(idToRemove)
        }

        while (userIds.size() > numUsers) {
            userIds.pop()
        }
        if (createEmail) {
            userIds?.each {
                if (!(userAttrsRepo.findByUserIdIgnoreCase(it))) {
                    try {
                        UserAttrs userAttrs = new UserAttrs()
                        userAttrs.userId = it.toLowerCase()
                        userAttrs.userIdForDisplay = it
                        userAttrs.email = it.contains('@') ? it : EmailUtils.generateEmaillAddressFor(it)
                        userAttrs.firstName = "${it.toUpperCase()}_first"
                        userAttrs.lastName = "${it.toUpperCase()}_last"
                        userAttrs.userTagsLastUpdated = new Date()
                        userAttrsRepo.save(userAttrs)
                    } catch (Exception e) {
                        throw new RuntimeException("error initializing UserAttrs for [${it}]", e)
                    }
                }
            }

        }

        return userIds
    }

}
