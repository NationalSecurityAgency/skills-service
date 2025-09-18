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
package skills.storage.repos

import groovy.transform.CompileStatic
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.lang.Nullable
import skills.storage.model.SkillApprovalConf

@CompileStatic
interface SkillApprovalConfRepo extends CrudRepository<SkillApprovalConf, Integer> {

    @Nullable
    SkillApprovalConf findByProjectIdAndApproverUserIdAndUserId(String projectId, String approverId, String userId)

    @Nullable
    SkillApprovalConf findByProjectIdAndApproverUserIdAndUserTagKeyIgnoreCaseAndUserTagValueIgnoreCase(String projectId, String approverId, String userTagKey, String userTagValue)

    long countByProjectIdAndApproverUserId(String projectId, String approverId)

    @Nullable
    SkillApprovalConf findByProjectIdAndApproverUserIdAndSkillRefId(String projectId, String approverId, Integer userRefId)

    @Nullable
    @Query('''select sac from SkillApprovalConf sac
                where 
                    sac.projectId = ?1
                    and sac.approverUserId = ?2
                    and sac.userId is null 
                    and sac.skillRefId is null
                    and sac.userTagKey is null
                    and sac.userTagValue is null
    ''')
    SkillApprovalConf findByProjectIdAndApproverUserIdAndRestAttributesAreNull(String projectId, String approverId)


    @Query('''select count(distinct(sac.approverUserId)) from SkillApprovalConf sac where sac.projectId = ?1 and (sac.skillRefId is not null or sac.userId is not null or sac.userTagKey is not null)''')
    Integer countConfForProject(String projectId)

    @Query('''select count(sac.id) > 0 from SkillApprovalConf sac where sac.projectId = ?1''')
    Boolean confExistForProject(String projectId)

    @Query('''select count(sac.id) > 0 from SkillApprovalConf sac where sac.projectId = ?1 and sac.approverUserId = ?2''')
    Boolean confExistForApprover(String projectId, String approverId)

    @Nullable
    @Query('''select sac.approverUserId from SkillApprovalConf sac 
                where sac.projectId = ?1
                and sac.approverUserId is not null
                and sac.skillRefId is null
                and sac.userTagKey is null
                and sac.userTagValue is null
                and sac.userId is null''')
    List<String> getUsersConfiguredForFallback(String projectId)

    static interface ApproverConfResult {
        Integer getId()

        String getApproverUserId()

        @Nullable
        String getUserIdForDisplay()
        @Nullable
        String getUserId()

        @Nullable
        String getUserTagKey()
        @Nullable
        String getUserTagValue()

        @Nullable
        String getSkillName()
        @Nullable
        String getSkillId()

        Date getUpdated()
    }

    @Nullable
    @Query('''select s.id as id, 
            s.approverUserId as approverUserId, 
            s.userId as userId, 
            uAttrs.userIdForDisplay as userIdForDisplay,
            s.userTagKey as userTagKey,
            s.userTagValue as userTagValue,
            skill.name as skillName, 
            skill.skillId as skillId,
            s.updated as updated
        from SkillApprovalConf s
        left join UserAttrs uAttrs on s.userId = uAttrs.userId
        left join SkillDef skill on s.skillRefId = skill.id
        where s.projectId = ?1''')
    List<ApproverConfResult> findAllByProjectId(String projectId)

    @Query('''select s.id as id, 
            s.approverUserId as approverUserId, 
            s.userId as userId, 
            uAttrs.userIdForDisplay as userIdForDisplay,
            s.userTagKey as userTagKey,
            s.userTagValue as userTagValue,
            skill.name as skillName, 
            skill.skillId as skillId,
            s.updated as updated
        from SkillApprovalConf s
        left join UserAttrs uAttrs on s.userId = uAttrs.userId
        left join SkillDef skill on s.skillRefId = skill.id
        where s.id = ?1''')
    ApproverConfResult findConfResultById(Integer id)

    Long deleteByProjectIdAndApproverUserId(String projectId, String approverUserId)
}
