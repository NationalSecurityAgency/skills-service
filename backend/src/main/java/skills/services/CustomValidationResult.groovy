package skills.services

import groovy.transform.Canonical
import groovy.transform.EqualsAndHashCode

@Canonical
class CustomValidationResult {
    boolean valid
    String msg
}
