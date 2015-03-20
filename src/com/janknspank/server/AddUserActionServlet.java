package com.janknspank.server;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.google.common.base.Strings;
import com.janknspank.bizness.GuidFactory;
import com.janknspank.bizness.UserInterests;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.CoreProto.Entity;
import com.janknspank.proto.UserProto.Interest;
import com.janknspank.proto.UserProto.Interest.InterestType;
import com.janknspank.proto.UserProto.User;
import com.janknspank.proto.UserProto.UserAction;

@AuthenticationRequired(requestMethod = "POST")
@ServletMapping(urlPattern = "/v1/add_user_action")
public class AddUserActionServlet extends StandardServlet {
  @Override
  protected JSONObject doPostInternal(HttpServletRequest req, HttpServletResponse resp)
      throws RequestException, DatabaseSchemaException, DatabaseRequestException {
    String actionTypeParam = getRequiredParameter(req, "type");
    String urlIdParam = getRequiredParameter(req, "url_id");
    String urlParam = getRequiredParameter(req, "url");
    String readStartTimeParam = getParameter(req, "read_start_time");
    String readEndTimeParam = getParameter(req, "read_end_time");
    User user = getUser(req);

    // If the user action happened on an interest stream (not home).
    String interestTypeParam = getParameter(req, "on_stream_for_interest[type]");
    String interestEntityTypeParam = getParameter(req, "on_stream_for_interest[entity][type]");
    String interestEntityKeywordParam = getParameter(req, "on_stream_for_interest[entity][keyword]");
    String interestEntityIdParam = getParameter(req, "on_stream_for_interest[entity][id]");
    String interestIndustryCodeParam = getParameter(req, "on_stream_for_interest[industry_code]");
    String interestIntentCodeParam = getParameter(req, "on_stream_for_interest[intent_code]");

    // parameter validation
    UserAction.ActionType actionType = UserAction.ActionType.valueOf(actionTypeParam);
    if (actionType == null) {
      throw new RequestException("Parameter 'type' is invalid");
    }

    UserAction.Builder actionBuilder = UserAction.newBuilder()
        .setId(GuidFactory.generate())
        .setUserId(user.getId())
        .setActionType(actionType)
        .setUrl(urlParam)
        .setUrlId(urlIdParam)
        .setCreateTime(System.currentTimeMillis());

    // Save user model components to the user action. If they change later
    // then we won't lose the context for the neural network
    // Note: getInterests returns only non-tombstoned interests
    List<Interest> userInterests = UserInterests.getInterests(user);
    actionBuilder.addAllInterest(userInterests);
    for (Interest userInterest : userInterests) {
      if (userInterest.getType() == InterestType.ADDRESS_BOOK_CONTACTS) {
        actionBuilder.addAllAddressBookContact(user.getAddressBookContactList());
      } else if (userInterest.getType() == InterestType.LINKED_IN_CONTACTS) {
        actionBuilder.addAllLinkedInContact(user.getLinkedInContactList());
      }
    }

    // If the user was viewing a different stream than home, record everything
    // we know about it.  Let's not bother validating anything here: Let's grab
    // what we can, and remember everything we can.  We can do validation later.
    if (interestTypeParam != null) {
      Interest.Builder interestBuilder = Interest.newBuilder()
          .setType(InterestType.valueOf(interestTypeParam));
      if (interestEntityKeywordParam != null
          && interestEntityIdParam != null
          && interestEntityTypeParam != null) {
        interestBuilder.setEntity(Entity.newBuilder()
            .setKeyword(interestEntityKeywordParam)
            .setId(interestEntityIdParam)
            .setType(interestEntityTypeParam));
      }
      if (interestIndustryCodeParam != null) {
        interestBuilder.setIndustryCode(Integer.parseInt(interestIndustryCodeParam));
      }
      if (interestIntentCodeParam != null) {
        interestBuilder.setIntentCode(interestIntentCodeParam);
      }
      actionBuilder.setOnStreamForInterest(interestBuilder);
    }

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
