package com.janknspank.server;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation placed on servlets that require authentication.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthenticationRequired {
  String requestMethod() default "ALL"; 
}
