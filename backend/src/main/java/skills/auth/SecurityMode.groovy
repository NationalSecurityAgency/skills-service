package skills.auth

import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.core.type.AnnotatedTypeMetadata

class SecurityMode {

    static class PkiAuth implements Condition {
        @Override
        boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            AuthMode authMode = AuthMode.getFromContext(context)
            return authMode == AuthMode.PKI
        }
    }

    static class FormAuth implements Condition {
        @Override
        boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            AuthMode authMode = AuthMode.getFromContext(context)
            return authMode == AuthMode.FORM
        }
    }
}
