package com.janknspank.bizness;

import java.util.Map;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.janknspank.proto.EnumsProto.IntentCode;

/**
 * Intent codes on users
 */
public class IntentCodes {
  public static final IntentCode INDUSTRY = IntentCode.newBuilder()
      .setCode("indst")
      .setDescription("Stay on top of your industry")
      .build();
  public static final IntentCode SKILLS = IntentCode.newBuilder()
      .setCode("sklls")
      .setDescription("Improve your skills")
      .build();
  public static final IntentCode SWITCH = IntentCode.newBuilder()
      .setCode("swtch")
      .setDescription("Switch companies")
      .build();
  public static final IntentCode START = IntentCode.newBuilder()
      .setCode("start")
      .setDescription("Start a company")
      .build();
  public static final Map<String, IntentCode> INTENT_CODE_MAP = Maps.uniqueIndex(
      ImmutableList.of(
          INDUSTRY,
          SKILLS,
          SWITCH,
          START),
      new Function<IntentCode, String>() {
        @Override
        public String apply(IntentCode intentCode) {
          return intentCode.getCode();
        }
      });
}
