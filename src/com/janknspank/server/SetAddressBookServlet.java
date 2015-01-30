package com.janknspank.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.janknspank.bizness.BiznessException;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.Serializer;
import com.janknspank.proto.UserProto.AddressBook;
import com.janknspank.proto.UserProto.User;

@AuthenticationRequired(requestMethod = "POST")
public class SetAddressBookServlet extends StandardServlet {
  @Override
  protected JSONObject doPostInternal(HttpServletRequest req, HttpServletResponse resp)
      throws BiznessException, DatabaseSchemaException, DatabaseRequestException, RequestException {
    User user = Database.with(User.class).get(getSession(req).getUserId());
    AddressBook addressBook = AddressBook.newBuilder()
        .setData(getRequiredParameter(req, "data"))
        .setCreateTime(System.currentTimeMillis())
        .build();
    user = Database.set(user, "address_book", addressBook);

    // Create response.
    JSONObject response = createSuccessResponse();
    response.put("user", Serializer.toJSON(user));
    return response;
  }
}
