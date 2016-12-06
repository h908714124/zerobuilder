package net.zerobuilder;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Determines step position of the getter
 */
@Retention(SOURCE)
@Target(METHOD)
public @interface BeanStep {
  int value();
}
