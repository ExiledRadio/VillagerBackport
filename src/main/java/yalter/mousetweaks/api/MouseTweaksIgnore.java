package yalter.mousetweaks.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mouse Tweaks' own opt-out annotation, declared here so it can be used without a dependency.
 *
 * <p>Mouse Tweaks looks this up by name on a screen's class and leaves the screen alone if it is
 * present, which is why declaring it locally works and why nothing breaks when Mouse Tweaks is not
 * installed - an annotation whose class is missing at runtime is simply never seen.
 *
 * <p>This is the approach Mouse Tweaks documents for exactly this purpose.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MouseTweaksIgnore {
}
