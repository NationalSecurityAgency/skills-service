package skills.service.auth

import org.springframework.context.annotation.ConditionContext

enum AuthMode {

    JWT, PKI

    static final String AUTH_MODE_PROPERTY = 'skills.authorization.authMode'
    static final String DEFAULT_AUTH_MODE = AuthMode.JWT

    static AuthMode getFromContext(ConditionContext context) {
        AuthMode.valueOf(context.environment.getProperty(AUTH_MODE_PROPERTY)?.toUpperCase()?.trim() ?: DEFAULT_AUTH_MODE)
    }
}
