package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.janknspank.data.DataInternalException;
import com.janknspank.data.Database;
import com.janknspank.data.ValidationException;
import com.janknspank.proto.Core.AddressBook;
import com.janknspank.proto.Core.Session;

@AuthenticationRequired(requestMethod = "POST")
public class SetAddressBookServlet extends StandardServlet {
  @Override
  protected JSONObject doPostInternal(HttpServletRequest req, HttpServletResponse resp)
      throws DataInternalException, ValidationException {
    // Read parameters.
    String linkedInJson = getRequiredParameter(req, "data");
    Session session = this.getSession(req);

    // Business logic.
    AddressBook data = AddressBook.newBuilder()
        .setUserId(session.getUserId())
        .setRawData(linkedInJson)
        .setCreateTime(System.currentTimeMillis())
        .build();
    Database.upsert(data);

    // Response.
    return createSuccessResponse();
  }
}
