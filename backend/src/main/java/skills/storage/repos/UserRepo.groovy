package skills.storage.repos

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.parser.Part
import org.springframework.lang.Nullable
import skills.storage.model.UserAttrs
import skills.storage.model.auth.User

interface UserRepo extends CrudRepository<User, Integer> {

    @Nullable
    User findByUserIdIgnoreCase(String userId)

    boolean existsByUserIdIgnoreCase(String userId)
}
