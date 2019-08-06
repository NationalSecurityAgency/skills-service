package skills.auth

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import groovy.transform.TupleConstructor
import org.springframework.security.core.GrantedAuthority
import skills.storage.model.auth.UserRole

//@Canonical
@EqualsAndHashCode
@ToString
@TupleConstructor
@CompileStatic
class UserSkillsGrantedAuthority implements GrantedAuthority {

    UserRole role

    @Override
    String getAuthority() {
        return role.roleName
    }
}
