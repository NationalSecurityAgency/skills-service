package skills.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ArtificialDelay {
    // introduces an artificial delay into any methods annotated with this interface, can be enabled to disabled by
    // application configuration properties
    long delay() default 2500;
}
