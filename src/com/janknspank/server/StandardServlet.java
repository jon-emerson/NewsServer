package com.janknspank.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.janknspank.Asserts;
import com.janknspank.data.DataInternalException;
import com.janknspank.data.DataRequestException;
import com.janknspank.data.ValidationException;

@AuthenticationRequired(requestMethod = "POST")
public abstract class StandardServlet extends NewsServlet {
  @Override
  protected final void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    writeSoyTemplate(resp, ".main", null);
  }

  protected abstract JSONObject doPostInternal(HttpServletRequest req, HttpServletResponse resp)
      throws DataInternalException, ValidationException, DataRequestException, NotFoundException;

  @Override
  protected final void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    try {
      JSONObject response = doPostInternal(req, resp);
      Asserts.assertTrue(response.getBoolean("success"), "success in response");
      writeJson(resp, response);
    } catch (NotFoundException e) {
      resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
      writeJson(resp, getErrorJson(e.getMessage()));
    } catch (DataInternalException | ValidationException e) {
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      writeJson(resp, getErrorJson(e.getMessage()));
    } catch (DataRequestException e) {
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      writeJson(resp, getErrorJson(e.getMessage()));
    }
  }

  protected final JSONObject createSuccessResponse() {
    JSONObject response = new JSONObject();
    response.put("success", true);
    return response;
  }
}
