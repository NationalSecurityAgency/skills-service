package skills.storage.model.auth

import groovy.transform.Immutable
import groovy.transform.ToString

import javax.persistence.*

@ToString
@Entity
@Table(name = 'user_properties')
@Immutable
class UserProp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id

    String name

    String value
}
