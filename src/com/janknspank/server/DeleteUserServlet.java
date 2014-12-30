package com.janknspank.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.janknspank.data.DataInternalException;
import com.janknspank.data.Database;
import com.janknspank.data.Sessions;
import com.janknspank.data.Users;
import com.janknspank.proto.Core.User;

public class DeleteUserServlet extends NewsServlet {
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

    // TODO(jonemerson): Add parameter validation.

    // Business logic.
    try {
      User user = Users.getByEmail(email);
      if (user == null) {
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        writeJson(resp, getErrorJson("User not found"));
        return;
      }
      Sessions.deleteAllFromUser(user);
      Database.delete(user);
    } catch (DataInternalException e) {
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      writeJson(resp, getErrorJson(e.getMessage()));
      return;
    }

    // Write response.
    JSONObject response = new JSONObject();
    response.put("success", true);
    writeJson(resp, response);
  }
}
