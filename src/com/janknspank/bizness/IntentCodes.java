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
  public static final IntentCode STAY_ON_TOP_OF_INDUSTRY = IntentCode.newBuilder()
      .setCode("indst")
      .setDescription("Stay on top of your industry")
      .build();
  public static final IntentCode IMPROVE_SKILLS = IntentCode.newBuilder()
      .setCode("sklls")
      .setDescription("Improve your skills")
      .build();
  public static final IntentCode SWITCH_INDUSTRIES = IntentCode.newBuilder()
      .setCode("swtch")
      .setDescription("Switch industries")
      .build();
  public static final IntentCode NEW_JOB = IntentCode.newBuilder()
      .setCode("nwjob")
      .setDescription("Find a new job")
      .build();
  public static final IntentCode START_COMPANY = IntentCode.newBuilder()
      .setCode("start")
      .setDescription("Start a company")
      .build();
  public static final Map<String, IntentCode> INTENT_CODE_MAP = Maps.uniqueIndex(
      ImmutableList.of(
          STAY_ON_TOP_OF_INDUSTRY,
          IMPROVE_SKILLS,
          SWITCH_INDUSTRIES,
          NEW_JOB,
          START_COMPANY),
      new Function<IntentCode, String>() {
        @Override
        public String apply(IntentCode intentCode) {
          return intentCode.getCode();
        }
      });
}
