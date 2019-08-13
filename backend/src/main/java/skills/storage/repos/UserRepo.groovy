package skills.storage.repos

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.parser.Part
import org.springframework.lang.Nullable
import skills.storage.model.auth.User

interface UserRepo extends CrudRepository<User, Integer> {

    @Nullable
    User findByUserIdIgnoreCase(String userId)

    @Query(value = "select DISTINCT u from User u, Setting s where u.id = s.userId and lower(u.userId) LIKE %?1% OR lower(s.value) LIKE %?1%")
    List<User> getUserByUserIdOrPropWildcard(String userId, Pageable pageable)

    boolean existsByUserIdIgnoreCase(String userId)
}
