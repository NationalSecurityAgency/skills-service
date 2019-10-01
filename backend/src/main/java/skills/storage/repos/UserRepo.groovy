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

    @Query('''select DISTINCT u 
        from User u, Setting s 
        where lower(u.userId) LIKE %?1% OR  
            (u.id = s.userId AND (s.settingGroup='user_info') AND lower(s.value) LIKE %?1%) order by u.id asc''')
    List<User> getUserByUserIdOrPropWildcard(String userIdInLowerCase, Pageable pageable)

    boolean existsByUserIdIgnoreCase(String userId)
}
