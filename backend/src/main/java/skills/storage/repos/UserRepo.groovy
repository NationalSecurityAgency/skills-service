package skills.storage.repos


import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import skills.storage.model.auth.User

interface UserRepo extends CrudRepository<User, Integer> {

    User findByUserId(String userId)

    @Query(value = "select u from User u JOIN u.userProps props where props.name = 'DN' and props.value = ?1")
    User getUserByDn(String userDn)

    @Query(value = "select DISTINCT u from User u JOIN u.userProps props where lower(u.userId) LIKE %?1% OR lower(props.value) LIKE %?1%")
    List<User> getUserByUserIdOrPropWildcard(String userDn, Pageable pageable)
}
