package com.janknspank.server;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.janknspank.data.DataInternalException;
import com.janknspank.data.Database;
import com.janknspank.data.UserInterests;
import com.janknspank.data.ValidationException;
import com.janknspank.proto.Core.AddressBook;
import com.janknspank.proto.Core.UserInterest;

@AuthenticationRequired(requestMethod = "POST")
public class SetAddressBookServlet extends StandardServlet {
  @Override
  protected JSONObject doPostInternal(HttpServletRequest req, HttpServletResponse resp)
      throws DataInternalException, ValidationException {
    String userId = this.getSession(req).getUserId();
    AddressBook addressBook = AddressBook.newBuilder()
        .setUserId(userId)
        .setData(getRequiredParameter(req, "data"))
        .setCreateTime(System.currentTimeMillis())
        .build();
    Database.upsert(addressBook);
    List<UserInterest> interests = UserInterests.updateInterests(userId, addressBook);

    // Return the user's interests.
    JSONObject userJson = new JSONObject();
    userJson.put("interests", interests);

    // Create response.
    JSONObject response = createSuccessResponse();
    response.put("user", userJson);
    return response;
  }

  public static void main(String args[]) throws Exception {
    for (String userId : new String[] {
        "vWxNTAAKB-KYAEofUGJL4A",
        "o0Sr9HzgxZMUVcUi09NIhg"}) {
      AddressBook addressBook = Database.get(userId, AddressBook.class);
      UserInterests.updateInterests(userId, addressBook);
    }
  }
}
