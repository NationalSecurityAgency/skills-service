package skills.storage.repos

import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import skills.storage.model.ProjDef
import skills.storage.model.UserAttrs

import javax.persistence.LockModeType

interface LockTransactionRepo extends CrudRepository<ProjDef, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    ProjDef findByProjectIdIgnoreCase(String projectId)

    @Query('''select attrs 
        from UserAttrs attrs 
        where
            attrs.userId = ?1''')
    UserAttrs findUserAttrsByUserId(String userId)
}
