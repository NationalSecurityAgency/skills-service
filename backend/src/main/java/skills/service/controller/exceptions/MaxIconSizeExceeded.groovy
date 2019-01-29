package skills.service.controller.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

/**
 * Created with IntelliJ IDEA.
 * Date: 12/7/18
 * Time: 4:13 PM
 */
@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Maximum Icon size exceeded")
class MaxIconSizeExceeded extends RuntimeException{
    MaxIconSizeExceeded() {
    }

    MaxIconSizeExceeded(String var1) {
        super(var1)
    }

    MaxIconSizeExceeded(String var1, Throwable var2) {
        super(var1, var2)
    }

    MaxIconSizeExceeded(Throwable var1) {
        super(var1)
    }

    MaxIconSizeExceeded(String var1, Throwable var2, boolean var3, boolean var4) {
        super(var1, var2, var3, var4)
    }
}
