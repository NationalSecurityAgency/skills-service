package skills.storage.repos

import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.lang.Nullable
import skills.storage.model.ProjDef
import skills.storage.model.SkillsDBLock
import skills.storage.model.UserAttrs
import skills.storage.model.UserPoints

import javax.persistence.LockModeType

interface SkillsDBLockRepo extends CrudRepository<SkillsDBLock, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    SkillsDBLock findByLock(String lock)

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query('''select p 
        from ProjDef p 
        where
            lower(p.projectId) = lower(?1)''')
    ProjDef findByProjectIdIgnoreCase(String projectId)

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query('''select attrs 
        from UserAttrs attrs 
        where
            attrs.userId = ?1''')
    UserAttrs findUserAttrsByUserId(String userId)


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Nullable
    @Query('''select up 
        from UserPoints up 
        where
            up.projectId = ?1 and
            up.userId = ?1''')
    UserPoints findUserPointsByProjectIdAndUserId(String projectId, String userId)
}
