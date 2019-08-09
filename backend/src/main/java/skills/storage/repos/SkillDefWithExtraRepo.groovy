package skills.storage.repos

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.lang.Nullable
import skills.storage.model.SkillDef
import skills.storage.model.SkillDef.ContainerType
import skills.storage.model.SkillDefWithExtra
import skills.storage.model.SkillRelDef

interface SkillDefWithExtraRepo extends PagingAndSortingRepository<SkillDefWithExtra, Integer> {

    List<SkillDefWithExtra> findAllByProjectIdAndType(String id, SkillDef.ContainerType type)

    @Nullable
    SkillDefWithExtra findByProjectIdAndSkillIdIgnoreCaseAndType(String id, String skillId, SkillDef.ContainerType type)

    @Nullable
    SkillDefWithExtra findByProjectIdAndSkillIdAndType(String id, String skillId, SkillDef.ContainerType type)

    static interface SkillDescDBRes {
        String getSkillId()
        String getDescription()
        String getHelpUrl()
    }

    @Query(value='''SELECT c.skillId as skillId, c.description as description, c.helpUrl as helpUrl
        from SkillDefWithExtra s, SkillRelDef r, SkillDefWithExtra c 
        where 
            s.id = r.parent and c.id = r.child and 
            s.projectId=?1 and c.projectId=?1 and
            s.skillId=?2 and r.type=?3 and c.version<=?4''')
    List<SkillDescDBRes> findAllChildSkillsDescriptions(String projectId, String parentSkillId, SkillRelDef.RelationshipType relationshipType, int version)
}
