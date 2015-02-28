package com.janknspank.bizness;

import java.util.Map;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

/**
 * Intent codes on users.
 */
public enum Intent {
  STAY_ON_TOP_OF_INDUSTRY("indst", "Stay on top of your industry"),
  IMPROVE_SKILLS("sklls", "Improve your skills"),
  SWITCH_INDUSTRIES("swtch", "Switch industries"),
  NEW_JOB("nwjob", "Find a new job"),
  START_COMPANY("start", "Start a company");

  private static final Map<String, Intent> INTENT_CODE_MAP = Maps.uniqueIndex(
      ImmutableList.of(
          STAY_ON_TOP_OF_INDUSTRY,
          IMPROVE_SKILLS,
          SWITCH_INDUSTRIES,
          NEW_JOB,
          START_COMPANY),
      new Function<Intent, String>() {
        @Override
        public String apply(Intent intentCode) {
          return intentCode.getCode();
        }
      });

  private final String code;
  private final String description;

  private Intent(String code, String description) {
    this.code = code;
    this.description = description;
  }

  public String getCode() {
    return code;
  }

  public String getDescription() {
    return description;
  }

  public static Intent fromCode(String code) {
    return INTENT_CODE_MAP.get(code);
  }
}
