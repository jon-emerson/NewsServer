package com.janknspank.bizness;

import com.janknspank.proto.UserProto.User;
import com.janknspank.proto.UserProto.User.Experiment;

public class Experiments {
  public static boolean has(User user, Experiment experiment) {
    for (Experiment userExperiment : user.getExperimentList()) {
      if (userExperiment.equals(experiment)) {
        return true;
      }
    }
    return false;
  }
}

