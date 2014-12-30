package com.janknspank.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.google.common.base.Strings;
import com.janknspank.data.DataInternalException;
import com.janknspank.data.Database;
import com.janknspank.data.Sessions;
import com.janknspank.data.Users;
import com.janknspank.proto.Core.Session;

public class LogoutServlet extends NewsServlet {
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    writeSoyTemplate(resp, ".main", null);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    // Read parameters.
    String email = getParameter(req, "email");
    String sessionKey = getParameter(req, "sessionKey");

    // Parameter validation.
    if (!(Strings.isNullOrEmpty(email) ^ Strings.isNullOrEmpty(sessionKey))) {
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      writeJson(resp, getErrorJson("You must only specify email or sessionKey, not both"));
      return;
    }

    // Business logic.
    int rowsAffected = 0;
    try {
      if (!Strings.isNullOrEmpty(email)) {
        rowsAffected = Sessions.deleteAllFromUser(Users.getByEmail(email));
      } else {
        Database.deletePrimaryKey(sessionKey, Session.class);
        rowsAffected++;
      }
    } catch (DataInternalException e) {
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      writeJson(resp, getErrorJson(e.getMessage()));
      return;
    }

    // Write response.
    JSONObject response = new JSONObject();
    response.put("success", true);
    response.put("rowsAffected", rowsAffected);
    writeJson(resp, response);
  }
}
