package com.janknspank.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.janknspank.data.DataInternalException;
import com.janknspank.data.DataRequestException;
import com.janknspank.data.Sessions;
import com.janknspank.data.Users;
import com.janknspank.proto.Serializer;
import com.janknspank.proto.Core.Session;
import com.janknspank.proto.Core.User;

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
      user = Users.create(email, password);
      session = Sessions.create(email, password);
    } catch (DataRequestException|DataInternalException e) {
      resp.setStatus(e instanceof DataInternalException ?
          HttpServletResponse.SC_INTERNAL_SERVER_ERROR : HttpServletResponse.SC_BAD_REQUEST);
      writeJson(resp, getErrorJson(e.getMessage()));
      return;
    }

    // Write response.
    JSONObject response = new JSONObject();
    response.put("success", true);
    response.put("user", Serializer.toJSON(user));
    response.put("session", Serializer.toJSON(session));
    writeJson(resp, response);
  }
}
