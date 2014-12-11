package com.janknspank.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.google.common.base.Strings;
import com.janknspank.data.DataRequestException;
import com.janknspank.data.Session;
import com.janknspank.data.User;

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
    int rowsAffected;
    try {
      if (!Strings.isNullOrEmpty(email)) {
        rowsAffected = Session.deleteAllFromUser(User.get(email));
      } else {
        rowsAffected = Session.deleteSessionKey(sessionKey) ? 1 : 0;
      }
    } catch (DataRequestException e) {
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
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
