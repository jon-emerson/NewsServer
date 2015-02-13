package com.janknspank.common;

import java.util.logging.Level;

public class Logger extends java.util.logging.Logger {
  public Logger(Class<?> clazz) {
    super(clazz.getName(), null);
  }

  public void error(String message) {
    this.log(Level.SEVERE, message);
  }

  public void error(String message, Throwable e) {
    this.log(Level.SEVERE, message, e);
  }

  public void warning(String message) {
    this.log(Level.WARNING, message);
  }

  public void warning(String message, Throwable e) {
    this.log(Level.WARNING, message, e);
  }

  public void info(String message) {
    this.log(Level.INFO, message);
  }

  public void info(String message, Throwable e) {
    this.log(Level.INFO, message, e);
  }

  public void debug(String message) {
    this.log(Level.FINE, message);
  }

  public void debug(String message, Throwable e) {
    this.log(Level.FINE, message, e);
  }
}
