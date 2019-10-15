package skills.storage.repos

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.lang.Nullable
import skills.storage.model.UserAttrs

interface UserAttrsRepo extends CrudRepository<UserAttrs, Integer> {

    @Nullable
    UserAttrs findByUserIdIgnoreCase(String userId)

    @Query('''select attrs 
        from User u, UserAttrs attrs 
        where
            u.userId = attrs.userId and
            (upper(CONCAT(attrs.firstName, ' ', attrs.lastName, ' (',  attrs.userIdForDisplay, ')')) like UPPER(CONCAT('%', ?1, '%')) OR 
             upper(attrs.userIdForDisplay) like UPPER(CONCAT('%', ?1, '%')))
        order by attrs.firstName asc''')
    List<UserAttrs> searchForUser(String userIdQuery, Pageable pageable)
}
