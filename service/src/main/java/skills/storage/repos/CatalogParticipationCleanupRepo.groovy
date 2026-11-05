/**
 * Copyright 2026 SkillTree
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
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.repository.Repository
import org.springframework.data.repository.query.Param
import skills.storage.model.UserPoints

/** Temporary pre-5.1 remediation SQL; retire with the root cleanup/Fix endpoints. */
interface CatalogParticipationCleanupRepo extends Repository<UserPoints, Integer> {
    @Query(value = '''select distinct project_id from skill_definition
        where type = 'Skill' and enabled = 'true'
        and copied_from_project_id is not null
        and copied_from_project_id <> project_id
        order by project_id''', nativeQuery = true)
    List<String> findProjectIdsWithEnabledCatalogImports()

    String NON_MEMBER = '''not exists (select 1 from user_performed_skill local
        where local.project_id = :projectId and local.user_id = credit.user_id)
        and not exists (select 1 from project_participation participant
        where participant.project_id = :projectId and participant.user_id = credit.user_id)'''
    String REMOVABLE = '''(credit.skill_id is null or exists (select 1 from skill_definition skill
        where skill.project_id = :projectId and skill.skill_id = credit.skill_id
        and (skill.type in ('Subject', 'SkillsGroup', 'Badge') or
        (skill.type = 'Skill' and skill.copied_from_skill_ref is not null
         and skill.copied_from_project_id <> :projectId))))'''

    // JDBC update counts reflect actual deletes, including overlapping calls; no IDs are materialized.
    @Modifying
    @Query(value = '''delete from user_points credit where credit.project_id = :projectId
        and ''' + NON_MEMBER + ' and ' + REMOVABLE, nativeQuery = true)
    int deletePoints(@Param('projectId') String projectId)

    @Modifying
    @Query(value = '''delete from user_achievement credit where credit.project_id = :projectId
        and ''' + NON_MEMBER + ' and ' + REMOVABLE, nativeQuery = true)
    int deleteAchievements(@Param('projectId') String projectId)

    @Modifying
    @Query(value = '''update user_points total set points = coalesce((
        select sum(child_credit.points) from skill_relationship_definition rel
        join skill_definition child on child.id = rel.child_ref_id
        join user_points child_credit on child_credit.skill_ref_id = child.id
        where rel.parent_ref_id = total.skill_ref_id and child.type = 'Skill' and child.enabled = 'true'
        and rel.type in ('RuleSetDefinition', 'GroupSkillToSubject', 'SkillsGroupRequirement')
        and child_credit.user_id = :userId and child_credit.project_id = :projectId), 0)
        where total.project_id = :projectId and total.user_id = :userId
        and exists (select 1 from skill_definition parent where parent.id = total.skill_ref_id
                    and parent.type in ('Subject', 'SkillsGroup'))''', nativeQuery = true)
    void repairContainerTotals(@Param('projectId') String projectId, @Param('userId') String userId)

    @Modifying
    @Query(value = '''update user_points total set points = coalesce((
        select sum(subject_credit.points) from user_points subject_credit
        join skill_definition subject on subject.id = subject_credit.skill_ref_id and subject.type = 'Subject'
        where subject_credit.project_id = :projectId and subject_credit.user_id = :userId), 0)
        where total.project_id = :projectId and total.user_id = :userId and total.skill_id is null''', nativeQuery = true)
    void repairProjectTotal(@Param('projectId') String projectId, @Param('userId') String userId)
}
