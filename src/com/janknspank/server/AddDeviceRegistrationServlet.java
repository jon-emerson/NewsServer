package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.janknspank.bizness.GuidFactory;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.CoreProto.DeviceRegistration;
import com.janknspank.proto.CoreProto.DeviceRegistration.DeviceType;
import com.janknspank.proto.UserProto.User;

@AuthenticationRequired(requestMethod = "POST")
public class AddDeviceRegistrationServlet extends StandardServlet {
  @Override
  protected JSONObject doPostInternal(HttpServletRequest req, HttpServletResponse resp)
      throws RequestException, DatabaseSchemaException, DatabaseRequestException {
    User user = Database.with(User.class).get(getSession(req).getUserId());
    DeviceRegistration registration = DeviceRegistration.newBuilder()
        .setId(GuidFactory.generate())
        .setUserId(user.getId())
        .setDeviceId(this.getRequiredParameter(req, "ios_device_token"))
        .setDeviceType(DeviceType.IOS)
        .setCreateTime(System.currentTimeMillis())
        .build();
    Database.insert(registration);
    return createSuccessResponse();
  }
}
