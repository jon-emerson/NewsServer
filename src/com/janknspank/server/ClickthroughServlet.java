package com.janknspank.server;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.google.api.client.repackaged.com.google.common.base.Throwables;
import com.google.common.util.concurrent.Futures;
import com.janknspank.bizness.GuidFactory;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.NotificationsProto.Notification;
import com.janknspank.proto.UserProto.UserAction;
import com.janknspank.proto.UserProto.UserAction.ActionType;

@ServletMapping(urlPattern = "/clickthrough")
public class ClickthroughServlet extends StandardServlet {

  @Override
  protected JSONObject doGetInternal(HttpServletRequest req, HttpServletResponse resp)
      throws RedirectException, RequestException, DatabaseSchemaException, DatabaseRequestException {
    String url = getRequiredParameter(req, "url");
    String userId = getRequiredParameter(req, "uid");
    String notificationId = getRequiredParameter(req, "nid");

    // Get the respective notification.
    Notification notification = Database.with(Notification.class).get(notificationId);

    // Validate that things match up.
    if (!userId.equals(notification.getUserId())) {
      throw new RequestException("Notification doesn't belong to user");
    }

    // Mark the notification as clicked, if necessary.
    Future<Notification> updatedNotification = notification.hasClickTime()
        ? Futures.immediateFuture(notification)
        : Database.with(Notification.class).setFuture(
            notification, "click_time", System.currentTimeMillis());

    // Save the user action.
    Database.insert(UserAction.newBuilder()
        .setId(GuidFactory.generate())
        .setUserId(userId)
        .setActionType(ActionType.EMAIL_CLICK)
        .setUrl(url)
        .setUrlId(notification.getUrlId())
        .setCreateTime(System.currentTimeMillis())
        .build());

    // Make sure the notification update commits.
    try {
      updatedNotification.get();
    } catch (InterruptedException | ExecutionException e) {
      Throwables.propagateIfInstanceOf(e.getCause(), DatabaseSchemaException.class);
      Throwables.propagateIfInstanceOf(e.getCause(), DatabaseRequestException.class);
      e.printStackTrace();
      throw new DatabaseRequestException("Could not read notification", e);
    }

    // Send the user off.
    throw new RedirectException(url);
  }
}
