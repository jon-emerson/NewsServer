package com.janknspank.server;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.api.client.util.Lists;
import com.janknspank.bizness.BiznessException;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.Serializer;
import com.janknspank.proto.CoreProto.Session;
import com.janknspank.proto.UserProto.AddressBookContact;
import com.janknspank.proto.UserProto.User;

@AuthenticationRequired(requestMethod = "POST")
@ServletMapping(urlPattern = "/v1/set_address_book")
public class SetAddressBookServlet extends StandardServlet {
  @Override
  protected JSONObject doPostInternal(HttpServletRequest req, HttpServletResponse resp)
      throws BiznessException, DatabaseSchemaException, DatabaseRequestException, RequestException {
    Session session = getSession(req);
    User user = getUser(req);

    List<AddressBookContact> addressBookContacts = Lists.newArrayList();
    JSONArray addressBookJson = new JSONArray(getRequiredParameter(req, "data"));
    for (int i = 0; i < addressBookJson.length(); i++) {
      String name = null;
      JSONObject personJson = addressBookJson.getJSONObject(i);
      if (personJson.has("firstName")) {
        if (personJson.has("lastName")) {
          name = personJson.getString("firstName") + " " + personJson.getString("lastName");
        } else {
          name = personJson.getString("firstName");
        }
      } else if (personJson.has("lastName")) {
        name = personJson.getString("lastName");
      }
      if (name != null && name.contains(" ")) {
        addressBookContacts.add(AddressBookContact.newBuilder()
            .setName(name)
            .build());
      }
    }

    user = Database.set(user, "address_book_contact", addressBookContacts);

    // Create the response.
    JSONObject response = this.createSuccessResponse();
    response.put("user", new UserHelper(user).getUserJson());
    response.put("session", Serializer.toJSON(session));
    return response;
  }
}
