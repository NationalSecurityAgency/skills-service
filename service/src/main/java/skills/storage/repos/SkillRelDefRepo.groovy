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
import org.springframework.data.repository.CrudRepository
import org.springframework.lang.Nullable
import skills.storage.model.SkillDef
import skills.storage.model.SkillRelDef

interface SkillRelDefRepo extends CrudRepository<SkillRelDef, Integer> {
    List<SkillRelDef> findAllByChildAndType(SkillDef child, SkillRelDef.RelationshipType type)

    List<SkillRelDef> findAllByChildAndTypeIn(SkillDef child, List<SkillRelDef.RelationshipType> types)

    @Query('''SELECT srd from SkillRelDef srd where srd.child.id=?1 and srd.type=?2''')
    List<SkillRelDef> findAllByChildIdAndType(Integer childId, SkillRelDef.RelationshipType type)

    @Query('''SELECT srd from SkillRelDef srd where srd.child.id=?1 and srd.type in ?2''')
    List<SkillRelDef> findAllByChildIdAndTypeIn(Integer childId, List<SkillRelDef.RelationshipType> types)

    SkillRelDef findByChildAndParentAndType(SkillDef child, SkillDef parent, SkillRelDef.RelationshipType type)
    List<SkillRelDef> findAllByParentAndType(SkillDef parent, SkillRelDef.RelationshipType type)
    List<SkillRelDef> findAllByParentAndTypeIn(SkillDef parent, List<SkillRelDef.RelationshipType> types)


    @Query(value = '''select count(srd.id) from SkillRelDef srd where srd.child.skillId=?1 and srd.type='BadgeRequirement' and srd.parent.type = 'GlobalBadge' ''')
    Integer getSkillUsedInGlobalBadgeCount(String skillId)

    @Query(value = '''select count(srd.id) from SkillRelDef srd where srd.type='BadgeRequirement' and srd.parent.type = 'GlobalBadge' and srd.parent.skillId=?1''')
    Integer getGlobalBadgeSkillCount(String badgeId)

    @Query(value = '''select count(srd1.id)
       from SkillRelDef srd1, SkillRelDef srd2
       where srd1.parent.skillId=?1
       and srd1.type='RuleSetDefinition'
       and srd1.child = srd2.child
       and srd1.parent.type = 'Subject'
       and srd2.type = 'BadgeRequirement'
       and srd2.parent.type = 'GlobalBadge' ''')
    Integer getSkillsFromSubjectUsedInGlobalBadgeCount(String skillId)

    @Query(value = '''select count(srd.id) from SkillRelDef srd where srd.child.projectId=?1 and srd.type='BadgeRequirement' and srd.parent.type = 'GlobalBadge' ''')
    Integer getProjectUsedInGlobalBadgeCount(String projectId)

    @Query('''SELECT 
        sd2.id as id,
        sd2.name as name, 
        sd2.skillId as skillId, 
        sd2.projectId as projectId, 
        sd2.version as version,
        sd2.pointIncrement as pointIncrement,
        sd2.pointIncrementInterval as pointIncrementInterval,
        sd2.numMaxOccurrencesIncrementInterval as numMaxOccurrencesIncrementInterval,
        sd2.totalPoints as totalPoints,
        sd2.type as skillType,
        sd2.displayOrder as displayOrder,
        sd2.created as created,
        sd2.updated as updated,
        sd2.selfReportingType as selfReportingType,
        sd2.enabled as enabled,
        sd2.numSkillsRequired as numSkillsRequired,
        sd2.copiedFrom as copiedFrom,
        sd2.readOnly as readOnly,
        sd2.copiedFromProjectId as copiedFromProjectId,
        pd.name as copiedFromProjectName
        from SkillDef sd1, SkillDef sd2, SkillRelDef srd
        left join ProjDef pd on sd2.copiedFromProjectId = pd.projectId
        where sd1 = srd.parent and sd2 = srd.child and srd.type=?3 
              and sd1.projectId=?1 and sd1.skillId=?2''')
    List<SkillDefRepo.SkillDefPartial> getChildrenPartial(String projectId, String parentSkillId, SkillRelDef.RelationshipType type)

    @Query('''SELECT 
        sd2.id as id,
        sd2.name as name, 
        sd2.skillId as skillId, 
        sd2.projectId as projectId, 
        sd2.version as version,
        sd2.pointIncrement as pointIncrement,
        sd2.pointIncrementInterval as pointIncrementInterval,
        sd2.numMaxOccurrencesIncrementInterval as numMaxOccurrencesIncrementInterval,
        sd2.totalPoints as totalPoints,
        sd2.type as skillType,
        sd2.displayOrder as displayOrder,
        sd2.created as created,
        sd2.updated as updated,
        sd2.selfReportingType as selfReportingType
        from SkillDef sd1, SkillDef sd2, SkillRelDef srd 
        where sd1 = srd.parent and sd2 = srd.child and srd.type=?2 
              and sd1.projectId is null and sd1.skillId=?1''')
    List<SkillDefRepo.SkillDefPartial> getGlobalChildrenPartial(String parentSkillId, SkillRelDef.RelationshipType type)

    @Query('''SELECT sd2 
        from SkillDef sd1, SkillDef sd2, SkillRelDef srd 
        where sd1 = srd.parent and sd2 = srd.child and srd.type in ?3 
              and sd1.projectId=?1 and sd1.skillId=?2''')
    List<SkillDef> getChildren(@Nullable String projectId, String parentSkillId, List<SkillRelDef.RelationshipType> types)

    @Nullable
    @Query(value = '''select srd.parent from SkillRelDef srd where srd.child.skillId=?1 and srd.type='BadgeRequirement' and srd.parent.type = 'GlobalBadge' ''')
    SkillDef findGlobalBadgeByChildSkillId(String skillId)

    @Nullable
    @Query(value= '''select srd.parent from SkillRelDef srd where srd.child.skillId=?1 and srd.type=?2 and srd.parent.type=?3 ''')
    List<SkillDef> findAllChildrenByChildSkillIdAndRelationshipTypeAndParentType(String skillId, SkillRelDef.RelationshipType relType, SkillDef.ContainerType parentType)


    static interface SkillDefSkinny {
        Integer getId()
        String getProjectId()
        String getName()
        String getSkillId()
        Integer getVersion()
        Integer getDisplayOrder()
        Date getCreated()
    }

    /**
     * mapping directly to entity is slow, we can save over a second in latency by mapping attributes explicitly
     */
    @Query('''select 
        sd1.id as id,
        sd1.name as name, 
        sd1.skillId as skillId, 
        sd1.projectId as projectId, 
        sd1.pointIncrement as pointIncrement,
        sd1.totalPoints as totalPoints,
        sd1.type as skillType,
         
        sd2.id as id,
        sd2.name as name, 
        sd2.skillId as skillId, 
        sd2.projectId as projectId, 
        sd2.pointIncrement as pointIncrement,
        sd2.totalPoints as totalPoints,
        sd2.type as skillType
        from SkillDef sd1, SkillDef sd2, SkillRelDef srd 
        where sd1 = srd.parent and sd2 = srd.child and srd.type=?2 
              and sd1.projectId=?1''')
    List<Object[]> getGraph(String projectId, SkillRelDef.RelationshipType type)
}
