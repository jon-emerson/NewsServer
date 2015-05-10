package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.janknspank.bizness.GuidFactory;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.notifications.IosPushNotificationHelper;
import com.janknspank.proto.NotificationsProto.DeviceRegistration;
import com.janknspank.proto.NotificationsProto.DeviceType;
import com.janknspank.proto.UserProto.User;

@AuthenticationRequired(requestMethod = "POST")
@ServletMapping(urlPattern = "/v1/add_device_registration")
public class AddDeviceRegistrationServlet extends StandardServlet {
  @Override
  protected JSONObject doPostInternal(HttpServletRequest req, HttpServletResponse resp)
      throws RequestException, DatabaseSchemaException, DatabaseRequestException {
    String deviceId = this.getRequiredParameter(req, "ios_device_token");

    // Make sure the user hasn't already registered this device.  This catches
    // dupes around 98% of the time, since our app sends a device registration
    // every time on startup.
    User user = getUser(req);
    for (DeviceRegistration registration :
        IosPushNotificationHelper.getDeviceRegistrations(user)) {
      if (deviceId.equals(registration.getDeviceId())) {
        // Already registered.
        return createSuccessResponse();
      }
    }

    DeviceRegistration registration = DeviceRegistration.newBuilder()
        .setId(GuidFactory.generate())
        .setUserId(user.getId())
        .setDeviceId(deviceId)
        .setDeviceType(DeviceType.IOS)
        .setCreateTime(System.currentTimeMillis())
        .build();
    Database.insert(registration);
    return createSuccessResponse();
  }
}
