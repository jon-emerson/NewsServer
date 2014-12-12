package com.janknspank.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.janknspank.data.DataInternalException;
import com.janknspank.data.LinkedInData;
import com.janknspank.data.Session;

@AuthenticationRequired(requestMethod = "POST")
public class SetLinkedInDataServlet extends NewsServlet {
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    writeSoyTemplate(resp, ".main", null);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    // Read parameters.
    String linkedInJson = getParameter(req, "linkedInJson");
    Session session = this.getSession(req);

    // TODO(jonemerson): Add parameter validation.

    // Business logic.
    try {
      LinkedInData.put(session.getUserId(), linkedInJson);
    } catch (DataInternalException e) {
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      writeJson(resp, getErrorJson(e.getMessage()));
      return;
    }

    // Write response.
    JSONObject response = new JSONObject();
    response.put("success", true);
    writeJson(resp, response);
  }
}
