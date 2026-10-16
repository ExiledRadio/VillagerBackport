package mezz.jei.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Stand-in for JEI's own, compiled against and never shipped. See build.gradle. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface JEIPlugin {
}
