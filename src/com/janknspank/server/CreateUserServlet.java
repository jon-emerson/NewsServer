package com.janknspank.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.janknspank.data.DataRequestException;
import com.janknspank.data.Session;
import com.janknspank.data.User;

public class CreateUserServlet extends NewsServlet {
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
    String password = getParameter(req, "password");

    // TODO(jonemerson): Add parameter validation.

    // Business logic.
    User user;
    Session session;
    try {
      user = User.create(email, password);
      session = Session.create(email, password);
    } catch (DataRequestException e) {
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      writeJson(resp, getErrorJson(e.getMessage()));
      return;
    }

    // Write response.
    JSONObject response = new JSONObject();
    response.put("user", user.toJSONObject());
    response.put("session", session.toJSONObject());
    writeJson(resp, response);
  }
}
