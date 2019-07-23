package skills.auth

import org.springframework.context.annotation.ConditionContext

enum AuthMode {

    FORM, PKI

    static final String AUTH_MODE_PROPERTY = 'skills.authorization.authMode'
    static final String DEFAULT_AUTH_MODE = AuthMode.FORM

    static AuthMode getFromContext(ConditionContext context) {
        AuthMode.valueOf(context.environment.getProperty(AUTH_MODE_PROPERTY)?.toUpperCase()?.trim() ?: DEFAULT_AUTH_MODE)
    }
}
