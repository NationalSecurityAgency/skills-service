package skills.service.auth

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import org.springframework.security.core.GrantedAuthority
import skills.storage.model.auth.UserRole

@Canonical
@CompileStatic
class UserSkillsGrantedAuthority implements GrantedAuthority {

    UserRole role

    @Override
    String getAuthority() {
        return role.roleName
    }
}
