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
package skills.services.settings


import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.auth.SkillsAuthorizationException
import skills.auth.UserInfo
import skills.auth.UserInfoService
import skills.storage.model.ClientPref
import skills.storage.repos.ClientPrefRepo

@Service
@Slf4j
class ClientPrefService {

    @Autowired
    UserInfoService userInfoService

    @Autowired
    ClientPrefRepo clientPrefRepo

    @Transactional
    void saveOrUpdateProjPrefForCurrentUser(ClientPrefKey prefKey, String value, String projectId) {
        UserInfo currentUser = userInfoService.getCurrentUser()
        if (!currentUser) {
            throw new SkillsAuthorizationException('No current user found')
        }
        String userId = currentUser.username.toLowerCase()
        ClientPref clientPref = getOneRemoveExtra(prefKey, userId, projectId)
        if (!clientPref) {
            clientPref = new ClientPref(key: prefKey.toString(), value: value, projectId: projectId, userId: userId)
        } else {
            clientPref.value = value
        }
        clientPrefRepo.save(clientPref)
    }

    @Transactional
    ClientPref findPref(ClientPrefKey prefKey, String userId, String projectId) {
        List<ClientPref> prefs = clientPrefRepo.findAllByKeyAndUserIdAndProjectId(prefKey.toString(), userId, projectId)
        return prefs ? prefs.first() : null
    }

    private ClientPref getOneRemoveExtra(ClientPrefKey prefKey, String userId, String projectId) {
        List<ClientPref> prefs = clientPrefRepo.findAllByKeyAndUserIdAndProjectId(prefKey.toString(), userId, projectId)
        if (prefs && prefs.size() > 1) {
            List<ClientPref> toRemove = prefs.subList(1, prefs.size())
            log.warn("There are more than 1 preference found for key=[${prefKey}], user=[$userId], projectId=[$projectId], will delete=${toRemove}")
            clientPrefRepo.deleteAll(toRemove)
        }

        return prefs ? prefs.first() : null
    }

}
