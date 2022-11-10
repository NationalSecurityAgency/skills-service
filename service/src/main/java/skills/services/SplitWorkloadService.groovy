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
package skills.services

import callStack.profiler.Profile
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import skills.controller.result.model.UserRoleRes
import skills.storage.model.SkillDefMin
import skills.storage.model.UserTag
import skills.storage.repos.SkillApprovalConfRepo
import skills.storage.repos.UserTagRepo

@Service
@Slf4j
class SplitWorkloadService {

    @Autowired
    SkillApprovalConfRepo skillApprovalConfRepo

    @Autowired
    UserTagRepo userTagRepo

    static class ApproverInfo {
        String approverId
        List<SkillApprovalConfRepo.ApproverConfResult> allConfs = []
        Set<String> userIds = new HashSet<>()
        Set<String> skillIds = new HashSet<>()
        Map<String, List<String>> confUserTags = [:]
        boolean isFallbackApprover = false

        ApproverInfo addConf(SkillApprovalConfRepo.ApproverConfResult it) {
            allConfs.add(it)

            if (it.userId) {
                userIds.add(it.userId)
            }
            if (it.skillId) {
                skillIds.add(it.skillId)
            }
            if (it.userTagKey && it.userTagValue) {
                List<String> userTagValues = confUserTags.get(it.userTagKey)
                if (userTagValues) {
                    userTagValues.add(it.userTagValue)
                } else {
                    confUserTags.put(it.userTagKey, [it.userTagValue])
                }
            }
        }
    }

    @Profile
    List<UserRoleRes> findUsersForThisRequest(List<UserRoleRes> userRolesList, SkillDefMin skillDefinition, String userId) {
        List<UserRoleRes> res = userRolesList
        List<ApproverInfo> configuredApprovers = loadApproverInfo(skillDefinition.projectId, userRolesList)
        if (configuredApprovers) {
            Set<String> matchedApproverIdsByConf = matchByConf(configuredApprovers, skillDefinition, userId)
            if (!matchedApproverIdsByConf) {
                // fallback
                matchedApproverIdsByConf = configuredApprovers.findAll( { it.isFallbackApprover }).collect { it.approverId }.toSet()
            }

            res = userRolesList.findAll { UserRoleRes roleToCheck -> matchedApproverIdsByConf.contains(roleToCheck.userId)}
        }
        return res
    }

    @Profile
    private Set<String> matchByConf(List<ApproverInfo> configuredApprovers, SkillDefMin skillDefinition, String userId) {
        Map<String, List<String>> userTags = loadUserTags(configuredApprovers, userId)
        return configuredApprovers.findAll {
            boolean matchesSkillIdConf = it.skillIds.contains(skillDefinition.skillId)
            boolean matchesUserIdConf = it.userIds.contains(userId)
            boolean matchesUserTag = false
            if ((!matchesSkillIdConf && !matchesUserIdConf) && userTags && it.confUserTags) {
                checkingTagConf:
                for (Map.Entry<String, List<String>> configuredTagConf : it.confUserTags.entrySet()) {
                    List<String> userTagValues = userTags.get(configuredTagConf.key)
                    for (String userTagValue : userTagValues) {
                        boolean foundMatch = configuredTagConf.value.find { userTagValue.toLowerCase().startsWith(it.toLowerCase()) }
                        if (foundMatch) {
                            matchesUserTag = true
                            break checkingTagConf;
                        }
                    }
                }
            }

            return matchesSkillIdConf || matchesUserIdConf || matchesUserTag
        }?.collect { it.approverId }.toSet()
    }

    @Profile
    private Map<String, List<String>> loadUserTags(List<ApproverInfo> configuredApprovers, String userId) {
        Map<String, List<String>> userTags
        if (configuredApprovers.find { !it.confUserTags.isEmpty() }) {
            List<UserTag> dbUserTags = userTagRepo.findAllByUserId(userId)
            if (dbUserTags) {
                userTags = [:]
                dbUserTags.groupBy { it.key }.each {
                    userTags.put(it.key, it.value.collect { it.value })
                }
            }
        }
        userTags
    }

    private List<ApproverInfo> loadApproverInfo(String projectId, List<UserRoleRes> allUserRoles) {
        List<ApproverInfo> res
        List<SkillApprovalConfRepo.ApproverConfResult> approverConfResults = skillApprovalConfRepo.findAllByProjectId(projectId)
        if (approverConfResults) {
            res = approverConfResults.groupBy { it.approverUserId }.collect {
                boolean isFallbackApprover = !it.value || (it.value.size() == 1 && isFallbackApproverRes(it.value.first()))
                ApproverInfo approverInfo = new ApproverInfo(
                        approverId: it.key,
                        isFallbackApprover: isFallbackApprover
                )

                if (!isFallbackApprover) {
                    it.value?.each {
                        approverInfo.addConf(it)
                    }
                }

                return approverInfo
            }
            res.addAll(allUserRoles.findAll { UserRoleRes userRole -> !res.find { it.approverId == userRole.userId } }.collect {
                new ApproverInfo(approverId: it.userId, isFallbackApprover: true)
            })
        }

        return res
    }

    private boolean isFallbackApproverRes(SkillApprovalConfRepo.ApproverConfResult result) {
        return result && !result.userId && !result.skillId && !result.userTagKey
    }

}
