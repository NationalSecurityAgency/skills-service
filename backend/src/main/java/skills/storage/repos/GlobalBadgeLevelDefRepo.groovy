package skills.storage.repos

import org.springframework.data.repository.CrudRepository
import skills.storage.model.GlobalBadgeLevelDef

interface GlobalBadgeLevelDefRepo extends CrudRepository<GlobalBadgeLevelDef, Integer>{

    List<GlobalBadgeLevelDef> findAllByBadgeId(Integer projectId)

}
