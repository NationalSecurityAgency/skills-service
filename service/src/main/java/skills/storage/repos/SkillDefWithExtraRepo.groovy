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

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.lang.Nullable
import skills.storage.model.SkillDef
import skills.storage.model.SkillDef.ContainerType
import skills.storage.model.SkillDefWithExtra
import skills.storage.model.SkillRelDef

import java.util.stream.Stream

interface SkillDefWithExtraRepo extends PagingAndSortingRepository<SkillDefWithExtra, Integer> {

    List<SkillDefWithExtra> findAllByProjectIdAndType(@Nullable String id, SkillDef.ContainerType type)

    @Nullable
    SkillDefWithExtra findByProjectIdAndSkillIdIgnoreCaseAndType(@Nullable String id, String skillId, SkillDef.ContainerType type)

    @Nullable
    SkillDefWithExtra findByProjectIdAndSkillIdIgnoreCaseAndTypeIn(@Nullable String id, String skillId, List<SkillDef.ContainerType> type)

    @Nullable
    SkillDefWithExtra findByProjectIdAndSkillIdAndType(String id, String skillId, SkillDef.ContainerType type)

    static interface SkillDescDBRes {
        String getSkillId()
        String getDescription()
        String getHelpUrl()
        Date getAchievedOn()
        SkillDef.SelfReportingType getSelfReportingType()
        Integer getCopiedFrom()
        SkillDef.ContainerType getType()
        String getEnabled()
    }

    @Query(value='''SELECT c.skillId as skillId, c.description as description, c.helpUrl as helpUrl, ua.achievedOn as achievedOn, c.selfReportingType as selfReportingType, c.type as type, c.enabled as enabled, c.copiedFrom as copiedFrom
        from SkillDefWithExtra s, SkillRelDef r, SkillDefWithExtra c
        left join UserAchievement ua on c.skillId = ua.skillId and c.projectId = ua.projectId and ua.userId=?5
        where 
            s.id = r.parent and c.id = r.child and 
            s.projectId=?1 and c.projectId=?1 and
            s.skillId=?2 and r.type=?3 and c.version<=?4''')
    List<SkillDescDBRes> findAllChildSkillsDescriptions(String projectId, String parentSkillId, SkillRelDef.RelationshipType relationshipType, int version, String userId)

    @Query(value='''SELECT c.skillId as skillId, c.description as description, c.helpUrl as helpUrl, ua.achievedOn as achievedOn, c.selfReportingType as selfReportingType, c.type as type
        from SkillDefWithExtra s, SkillRelDef r, SkillDefWithExtra c
        left join UserAchievement ua on c.skillId = ua.skillId and c.projectId = ua.projectId and ua.userId=?5
        where 
            s.id = r.parent and c.id = r.child and 
            s.projectId=?1 and c.projectId=?1 and
            s.skillId in ?2 and r.type=?3 and c.version<=?4''')
    List<SkillDescDBRes> findAllChildSkillsDescriptionsForSkillsGroups(String projectId, List<String> parentSkillIds, SkillRelDef.RelationshipType relationshipType, int version, String userId)


    @Query(value='''SELECT c.skillId as skillId, c.description as description, c.helpUrl as helpUrl, ua.achievedOn as achievedOn, c.copiedFrom as copiedFrom
        from SkillDefWithExtra s, SkillRelDef r, SkillDefWithExtra c 
        left join UserAchievement ua on c.skillId = ua.skillId and c.projectId = ua.projectId and ua.userId=?4
        where 
            s.id = r.parent and c.id = r.child and 
            s.projectId is null and
            s.skillId=?1 and r.type=?2 and c.version<=?3''')
    List<SkillDescDBRes> findAllGlobalChildSkillsDescriptions(String parentSkillId, SkillRelDef.RelationshipType relationshipType, int version, String userId)

    @Query(value='''
        WITH mp AS (
            SELECT s.project_id AS project_id 
            FROM settings s, users uu, settings s1
            WHERE s.setting = 'my_project' 
                AND uu.user_id=?1 
                AND uu.id = s.user_ref_id 
                AND s.project_id = s1.project_id 
                AND s1.setting = 'production.mode.enabled' 
                AND s1.value = 'true'
        )
        SELECT * FROM skill_definition sd 
                WHERE (
                (sd.type = 'Badge' AND 
                    sd.project_id IN (
                        SELECT project_id FROM mp
                    )
                ) OR 
                (sd.type='GlobalBadge' 
                        AND ( 
                            exists (
                                SELECT true
                                FROM global_badge_level_definition gbld
                                WHERE gbld.skill_ref_id = sd.id AND gbld.project_id in (select project_id from mp)
                            ) 
                            OR ( 
                            sd.id IN (
                                SELECT srd.parent_ref_id FROM skill_relationship_definition srd JOIN skill_definition ssd ON srd.child_ref_id = ssd.id AND ssd.project_id IN (SELECT project_id FROM mp) 
                                ) 
                            )
                    ) 
                )) AND
              sd.enabled  = 'true'
    ''', nativeQuery = true)
    Stream<SkillDefWithExtra> findAllMyBadgesForUser(String userId)
}
