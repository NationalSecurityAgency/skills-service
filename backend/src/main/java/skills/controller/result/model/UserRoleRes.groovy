package skills.controller.result.model

import groovy.transform.EqualsAndHashCode
import skills.storage.model.auth.RoleName

@EqualsAndHashCode
class UserRoleRes {

    String userId

    String projectId

    RoleName roleName
}
