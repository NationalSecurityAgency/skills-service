package skills.intTests.utils

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class TransactionHelper {

    @Transactional
    Object doInTransaction(Closure closure) {
        return closure.call()
    }
}
