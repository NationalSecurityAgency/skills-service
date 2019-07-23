package skills.auth.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AdminUsersOnlyWhenUserIdSupplied {
    // user this annotation on methods that should be restricted to user's with ROLE_PROJECT_ADMIN
}
