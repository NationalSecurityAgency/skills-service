package skills.service.auth

import groovy.transform.CompileStatic
import groovy.transform.Immutable
import org.springframework.security.core.GrantedAuthority
import skills.storage.model.auth.UserRole

@Immutable
@CompileStatic
class UserSkillsGrantedAuthority implements GrantedAuthority {

    UserRole role

    @Override
    String getAuthority() {
        return role.roleName
    }
}
