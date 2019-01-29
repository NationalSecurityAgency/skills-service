package skills.service.controller.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

/**
 * Created with IntelliJ IDEA.
 * Date: 12/7/18
 * Time: 4:08 PM
 */
@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Unsupported content-type")
class InvalidContentTypeException extends RuntimeException{

    InvalidContentTypeException() {
    }

    InvalidContentTypeException(String var1) {
        super(var1)
    }

    InvalidContentTypeException(String var1, Throwable var2) {
        super(var1, var2)
    }

    InvalidContentTypeException(Throwable var1) {
        super(var1)
    }

    InvalidContentTypeException(String var1, Throwable var2, boolean var3, boolean var4) {
        super(var1, var2, var3, var4)
    }
}
