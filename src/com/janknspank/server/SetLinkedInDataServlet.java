package com.janknspank.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.janknspank.data.DataInternalException;
import com.janknspank.data.Database;
import com.janknspank.data.ValidationException;
import com.janknspank.proto.Core.LinkedInData;
import com.janknspank.proto.Core.Session;

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
      LinkedInData data = LinkedInData.newBuilder()
          .setUserId(session.getUserId())
          .setRawData(linkedInJson)
          .setCreateTime(System.currentTimeMillis())
          .build();
      Database.upsert(data);
    } catch (DataInternalException e) {
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      writeJson(resp, getErrorJson(e.getMessage()));
      return;
    } catch (ValidationException e) {
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
