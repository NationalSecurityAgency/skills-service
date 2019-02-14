package skills.storage.model.auth

import groovy.transform.Canonical
import groovy.transform.ToString

import javax.persistence.*

@ToString
@Entity
@Table(name = 'user_roles')
@Canonical
class UserRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id

    String userId

    String projectId

    @Enumerated(EnumType.STRING)
    RoleName roleName
}
