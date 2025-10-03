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

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.data.repository.query.Param
import org.springframework.lang.Nullable
import skills.skillLoading.model.SkillBadgeSummary
import skills.storage.model.SimpleBadgeRes
import skills.storage.model.SkillDef
import skills.storage.model.SkillDef.ContainerType
import skills.storage.model.SkillDefWithExtra
import skills.storage.model.SkillRelDef

import java.util.stream.Stream

import static skills.storage.model.SkillDef.*

interface SkillDefWithExtraRepo extends JpaRepository<SkillDefWithExtra, Integer>, PagingAndSortingRepository<SkillDefWithExtra, Integer> {

    List<SkillDefWithExtra> findAllByProjectIdAndType(@Nullable String id, ContainerType type)


    List<SkillDefWithExtra> findAllByProjectIdAndTypeAndEnabled(@Nullable String id, ContainerType type, String enabled)

    @Nullable
    SkillDefWithExtra findByProjectIdAndSkillIdIgnoreCaseAndType(@Nullable String id, String skillId, ContainerType type)

    @Nullable
    SkillDefWithExtra findByProjectIdAndSkillIdIgnoreCaseAndTypeIn(@Nullable String id, String skillId, List<ContainerType> type)

    @Nullable
    SkillDefWithExtra findByProjectIdAndSkillId(String id, String skillId)

    @Nullable
    List<SkillDefWithExtra> findAllByProjectIdAndSkillIdIn(String projectId, List<String> skillId)

    static interface SkillDescDBRes {
        String getSkillId()
        @Nullable
        String getDescription()
        @Nullable
        String getHelpUrl()
        @Nullable
        Date getAchievedOn()
        SelfReportingType getSelfReportingType()
        Integer getCopiedFrom()
        @Nullable
        ContainerType getType()
        String getEnabled()
        String getJustificationRequired()
        @Nullable
        String getVideoUrl()
        @Nullable
        String getVideoType()
        Boolean getVideoHasCaptions()
        Boolean getVideoHasTranscript()
        @Nullable
        String getSlidesUrl()
        @Nullable
        String getSlidesType()
        @Nullable
        Double getSlidesWidth()
    }

    @Query(value='''SELECT c.skill_id               as skillId,
                           convert_from(lo_get(CAST(c.description as oid)), 'UTF8') as description,
                           convert_from(lo_get(CAST(c.help_url as oid)), 'UTF8') as helpUrl,
                           ua.achieved_on           as achievedOn,
                           c.self_reporting_type     as selfReportingType,
                           c.type                  as type,
                           c.justification_required as justificationRequired,
                           c.enabled               as enabled,
                           c.copied_from_skill_ref            as copiedFrom,
                           sad.attributes ->> 'videoUrl' as videoUrl,
                           sad.attributes ->> 'videoType'                                                 as videoType,
                           sad.attributes ->> 'url' as slidesUrl,
                           sad.attributes ->> 'type' as slidesType,
                           sad.attributes ->> 'width' as slidesWidth,
                           case when sad.attributes ->> 'captions' is not null then true else false end   as videoHasCaptions,
                           case when sad.attributes ->> 'transcript' is not null then true else false end as videoHasTranscript
                    from skill_definition s,
                         skill_relationship_definition r,
                         skill_definition c
                             left join user_achievement ua on c.skill_id = ua.skill_id and c.project_id = ua.project_id and ua.user_id = ?5
                             left join skill_attributes_definition sad on
                               (case when c.copied_from_skill_ref is not null then c.copied_from_skill_ref else c.id end) = sad.skill_ref_id 
                               and sad.type in ('Video', 'Slides')
                    where s.id = r.parent_ref_id
                      and c.id = r.child_ref_id
                      and s.project_id = ?1
                      and c.project_id = ?1
                      and c.enabled = 'true'
                      and s.skill_id = ?2
                      and r.type = ?3
                      and c.version <= ?4
                    order by c.skill_id asc;
    ''', nativeQuery = true)
    List<SkillDescDBRes> findAllChildSkillsDescriptions(String projectId, String parentSkillId, String relationshipType, int version, String userId)

    @Query(value='''SELECT c.skill_id                                                                     as skillId,
                           convert_from(lo_get(CAST(c.description as oid)), 'UTF8') as description,
                           convert_from(lo_get(CAST(c.help_url as oid)), 'UTF8') as helpUrl,
                           ua.achieved_on                                                                 as achievedOn,
                           c.self_reporting_type                                                          as selfReportingType,
                           c.type                                                                         as type,
                           c.justification_required                                                       as justificationRequired,
                           sad.attributes ->> 'videoUrl'                                                  as videoUrl,
                           sad.attributes ->> 'videoType'                                                 as videoType,
                           case when sad.attributes ->> 'captions' is not null then true else false end   as videoHasCaptions,
                           case when sad.attributes ->> 'transcript' is not null then true else false end as videoHasTranscript,
                           sad.attributes ->> 'url' as slidesUrl,
                           sad.attributes ->> 'type' as slidesType,
                           sad.attributes ->> 'width' as slidesWidth
                    from skill_definition s,
                         skill_relationship_definition r,
                         skill_definition c
                             left join user_achievement ua on c.skill_id = ua.skill_id and c.project_id = ua.project_id and ua.user_id = ?5
                             left join skill_attributes_definition sad on
                                     (case when c.copied_from_skill_ref is not null then c.copied_from_skill_ref else c.id end) =
                                     sad.skill_ref_id
                                 and sad.type in ('Video', 'Slides')
                    where s.id = r.parent_ref_id
                      and c.id = r.child_ref_id
                      and s.project_id = ?1
                      and c.project_id = ?1
                      and s.skill_id in ?2
                      and r.type = ?3
                      and c.version <= ?4
                    order by c.skill_id asc''', nativeQuery = true)
    List<SkillDescDBRes> findAllChildSkillsDescriptionsForSkillsGroups(String projectId, List<String> parentSkillIds, String relationshipType, int version, String userId)


    @Query(value='''SELECT c.skillId as skillId, c.description as description, c.helpUrl as helpUrl, ua.achievedOn as achievedOn, c.copiedFrom as copiedFrom
        from SkillDefWithExtra s, SkillRelDef r, SkillDefWithExtra c 
        left join UserAchievement ua on c.skillId = ua.skillId and c.projectId = ua.projectId and ua.userId=?4
        where 
            s.id = r.parent.id and c.id = r.child.id and 
            s.projectId is null and c.enabled = 'true' and
            s.skillId=?1 and r.type=?2 and c.version<=?3
        order by c.skillId asc''')
    List<SkillDescDBRes> findAllGlobalChildSkillsDescriptions(String parentSkillId, SkillRelDef.RelationshipType relationshipType, int version, String userId)

    static interface SkillIdAndDesc {
        String getSkillId()
        @Nullable
        String getDescription()
    }
    @Query(value='''SELECT sdf.skillId as skillId, sdf.description as description 
        from SkillDefWithExtra sdf
        where sdf.projectId=?1 and sdf.skillId in ?2''')
    List<SkillIdAndDesc> findDescriptionBySkillIdIn(String projectId, List<String> skillIds)

    @Query(value='''
        WITH mp AS (
            SELECT s.project_id AS project_id 
            FROM settings s, users uu
            WHERE s.setting = 'my_project' 
                AND uu.user_id=?1 
                AND uu.id = s.user_ref_id 
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

    @Nullable
    @Query('''select s from SkillDefWithExtra s where s.copiedFrom = ?1''')
    List<SkillDefWithExtra> findSkillsCopiedFrom(int skillRefId)

    @Query(value = '''select count(id) > 0
            from skill_definition
            where
                convert_from(lo_get(CAST(description as oid)), 'UTF8') like CONCAT('%(/api/download/', ?3, ')%')
              and LOWER(skill_id) <> LOWER(?2)
              and LOWER(project_id) = LOWER(?1) ''', nativeQuery = true)
    Boolean otherSkillsExistInProjectWithAttachmentUUID(String projectId, String notThisSkill, String attachmentUUID)


    @Nullable
    @Query('''SELECT sd 
          FROM SkillDefWithExtra sd
          JOIN UserRole ur ON ur.globalBadgeId = sd.skillId
          WHERE ur.userId = :userId 
          AND ur.roleName = :#{T(skills.storage.model.auth.RoleName).ROLE_GLOBAL_BADGE_ADMIN} 
          AND sd.type = :#{T(skills.storage.model.SkillDef.ContainerType).GlobalBadge}''')
    List<SkillDefWithExtra> findGlobalBadgesForAdmin(@Param("userId") String userId)

    @Nullable
    @Query('''SELECT subjectDef
          FROM SkillRelDef srd
          JOIN SkillDefWithExtra subjectDef ON subjectDef.id = srd.parent.id AND srd.type = 'RuleSetDefinition'
          JOIN SkillDefWithExtra groupDef ON groupDef.id = srd.child.id AND groupDef.type = 'SkillsGroup'
          WHERE subjectDef.projectId = :projectId AND subjectDef.type = 'Subject'
          AND groupDef.skillId = :groupId AND groupDef.type = 'SkillsGroup' AND groupDef.projectId = :projectId''')
    SkillDefWithExtra findSubjectForGroup(@Param("projectId") String projectId, @Param("groupId") String groupId)
}
