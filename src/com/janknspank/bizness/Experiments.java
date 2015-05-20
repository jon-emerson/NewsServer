package com.janknspank.bizness;

import com.janknspank.database.Database;
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

  public static void main(String args[]) throws Exception {
    Database.update(Users.getByEmail("tom.charytoniuk@gmail.com").toBuilder()
        .addExperiment(Experiment.EXPRESSIONS)
        .build());
  }
}

