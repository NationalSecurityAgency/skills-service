package skills.storage.repos

import org.springframework.data.repository.CrudRepository
import skills.storage.model.LevelDef

interface LevelDefRepo extends CrudRepository<LevelDef, Integer>{

    List<LevelDef> findAllByProjectRefId(Integer projectId)

}
