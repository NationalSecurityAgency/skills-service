package skills.service.auth

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(value = HttpStatus.FORBIDDEN)
class SkillsAuthorizationException extends RuntimeException {
    SkillsAuthorizationException() {
        super()
    }

    SkillsAuthorizationException(String message) {
        super(message)
    }

    SkillsAuthorizationException(String var1, Throwable var2) {
        super(var1, var2)
    }

    SkillsAuthorizationException(Throwable var1) {
        super(var1)
    }

    SkillsAuthorizationException(String var1, Throwable var2, boolean var3, boolean var4) {
        super(var1, var2, var3, var4)
    }
}
