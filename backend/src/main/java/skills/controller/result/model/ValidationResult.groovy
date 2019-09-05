package skills.controller.result.model

import groovy.transform.Canonical
import groovy.transform.EqualsAndHashCode

@Canonical
class ValidationResult {
    boolean valid
    String msg
}
