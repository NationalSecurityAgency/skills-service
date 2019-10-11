package skills.storage.model


import javax.persistence.*

@Entity
@Table(name = 'user_attrs')
class UserAttrs {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id

    String userId

    String firstName
    String lastName
    String email
    String dn
    String nickname
    String userIdForDisplay
}
