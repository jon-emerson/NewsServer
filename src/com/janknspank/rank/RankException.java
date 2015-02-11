package com.janknspank.rank;

public class RankException extends Exception {
  public RankException(String message) {
    super(message);
  }

  public RankException(String message, Exception cause) {
    super(message, cause);
  }
}
