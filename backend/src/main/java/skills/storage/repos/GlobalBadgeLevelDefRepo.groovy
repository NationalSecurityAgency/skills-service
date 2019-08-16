package skills.storage.repos

import org.springframework.data.repository.CrudRepository
import skills.storage.model.GlobalBadgeLevelDef

interface GlobalBadgeLevelDefRepo extends CrudRepository<GlobalBadgeLevelDef, Integer>{

    List<GlobalBadgeLevelDef> findAllByBadgeId(String badgeId)

    GlobalBadgeLevelDef findByBadgeIdAndProjectIdAndLevel(String badgeId, String projectId, Integer level)

}
