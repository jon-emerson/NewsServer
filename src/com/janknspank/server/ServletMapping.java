package com.janknspank.server;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation placed on servlets to specify what URL pattern they service.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ServletMapping {
  String urlPattern() default ""; 
}
