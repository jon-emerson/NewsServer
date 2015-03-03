package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.google.common.base.Strings;
import com.janknspank.bizness.GuidFactory;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.UserProto.User;
import com.janknspank.proto.UserProto.UserAction;

@AuthenticationRequired(requestMethod = "POST")
public class AddUserActionServlet extends StandardServlet {
  @Override
  protected JSONObject doPostInternal(HttpServletRequest req, HttpServletResponse resp)
      throws RequestException, DatabaseSchemaException, DatabaseRequestException {
    String actionTypeParam = getRequiredParameter(req, "type");
    String urlIdParam = getRequiredParameter(req, "url_id");
    String urlParam = getRequiredParameter(req, "url");
    String readStartTimeParam = getParameter(req, "read_start_time");
    String readEndTimeParam = getParameter(req, "read_end_time");

    // parameter validation
    UserAction.ActionType actionType = UserAction.ActionType.valueOf(actionTypeParam);
    if (actionType == null) {
      throw new RequestException("Parameter 'type' is invalid");
    }

    User user = Database.with(User.class).get(getSession(req).getUserId());
    UserAction.Builder actionBuilder = UserAction.newBuilder()
        .setId(GuidFactory.generate())
        .setUserId(user.getId())
        .setActionType(actionType)
        .setUrl(urlParam)
        .setUrlId(urlIdParam)
        .setCreateTime(System.currentTimeMillis());

    if (actionType == UserAction.ActionType.READ_ARTICLE) {
      if (Strings.isNullOrEmpty(readStartTimeParam)) {
        throw new RequestException("Parameter 'read_start_time' is missing");
      } else if (Strings.isNullOrEmpty(readEndTimeParam)) {
        throw new RequestException("Parameter 'read_end_time' is missing");
      }

      long readStartTime;
      long readEndTime;
      try {
        readStartTime = Long.parseLong(readStartTimeParam);
      } catch (NumberFormatException e) {
        throw new RequestException("Parameter 'read_start_time' is not a valid time");
      }

      try {
        readEndTime = Long.parseLong(readEndTimeParam);
      } catch (NumberFormatException e) {
        throw new RequestException("Parameter 'read_end_time' is not a valid time");
      }

      actionBuilder.setReadStartTime(readStartTime).setReadEndTime(readEndTime);
    }

    UserAction userAction = actionBuilder.build();
    Database.insert(userAction);

    return createSuccessResponse();
  }
}
