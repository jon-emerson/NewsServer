package com.janknspank.server;

import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.google.common.collect.Lists;
import com.janknspank.bizness.IntentCodes;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.EnumsProto.IntentCode;
import com.janknspank.proto.UserProto.Intent;
import com.janknspank.proto.UserProto.User;

@AuthenticationRequired(requestMethod = "POST")
public class SetUserIntentServlet extends StandardServlet {
  @Override
  protected JSONObject doPostInternal(HttpServletRequest req, HttpServletResponse resp)
      throws RequestException, DatabaseSchemaException, DatabaseRequestException {
    // Read parameters.
    String intentCodeString = getRequiredParameter(req, "code");
    boolean enabled = Boolean.parseBoolean(getRequiredParameter(req, "enabled"));
    User user = Database.with(User.class).get(getSession(req).getUserId());

    // Parameter validation.
    IntentCode intentCode = IntentCodes.fromCode(intentCodeString);
    if (intentCode == null) {
      throw new RequestException("Intent code is not valid");
    }

    // Business logic.
    List<Intent> userIntents = Lists.newArrayList(user.getIntentList());
    Iterator<Intent> iterator = userIntents.iterator();
    Intent existingIntent = null;
    boolean intentsChanged = false;
    while (iterator.hasNext()) {
      Intent userIntent = iterator.next();
      if (userIntent.getCode().equals(intentCode.getCode())) {
        existingIntent = userIntent;
        if (!enabled) {
          iterator.remove();
          intentsChanged = true;
        }
      }
    }

    if (existingIntent == null && enabled) {
      userIntents.add(Intent.newBuilder()
            .setCode(intentCode.getCode())
            .setCreateTime(System.currentTimeMillis())
            .build());
      intentsChanged = true;
    }

    if (intentsChanged) {
      Database.with(User.class).set(user, "intent", userIntents);
    }

    // Write response.
    return createSuccessResponse();
  }
}