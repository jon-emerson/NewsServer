package com.janknspank.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.google.common.collect.ImmutableList;
import com.janknspank.data.DataInternalException;
import com.janknspank.data.UserUrlRatings;
import com.janknspank.proto.Core.Session;

@AuthenticationRequired(requestMethod = "POST")
public class DeleteUserUrlRatingServlet extends NewsServlet {
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    writeSoyTemplate(resp, ".main", null);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    Session session = this.getSession(req);

    // Request validation.
    String urlId = getParameter(req, "urlId");
    if (urlId == null) {
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      writeJson(resp, getErrorJson("urlId required"));
      return;
    }

    // Business logic.
    try {
      int numDeleted = UserUrlRatings.deleteIds(session.getUserId(), ImmutableList.of(urlId));
      if (numDeleted == 0) {
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        writeJson(resp, getErrorJson("Rating not found"));
        return;
      }
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
