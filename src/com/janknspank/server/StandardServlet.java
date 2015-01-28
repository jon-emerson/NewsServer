package com.janknspank.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.google.template.soy.data.SoyMapData;
import com.janknspank.common.Asserts;
import com.janknspank.data.DataInternalException;
import com.janknspank.data.DataRequestException;
import com.janknspank.data.ValidationException;

@AuthenticationRequired(requestMethod = "POST")
public abstract class StandardServlet extends NewsServlet {
  protected JSONObject doGetInternal(HttpServletRequest req, HttpServletResponse resp)
      throws DataInternalException, ValidationException, DataRequestException, NotFoundException,
          RedirectException {
    return null;
  }

  /**
   * Returns any Soy data necessary for rendering the .main template for this
   * servlet's Soy page.
   */
  protected SoyMapData getSoyMapData(HttpServletRequest req)
      throws DataInternalException, ValidationException, DataRequestException, NotFoundException,
          RedirectException {
    return null;
  }

  @Override
  protected final void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    try {
      JSONObject response = doGetInternal(req, resp);
      if (response == null) {
        resp.setHeader("Content-Type", "text/html; charset=utf-8");
        writeSoyTemplate(resp, ".main", getSoyMapData(req));
      } else {
        Asserts.assertTrue(response.getBoolean("success"), "success in response");
        writeJson(resp, response);
      }
    } catch (RedirectException e) {
      resp.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
      resp.setHeader("Location", e.getNextUrl());
    } catch (NotFoundException e) {
      resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
      writeJson(resp, getErrorJson(e.getMessage()));
    } catch (DataInternalException e) {
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      writeJson(resp, getErrorJson(e.getMessage()));
    } catch (DataRequestException | ValidationException e) {
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      writeJson(resp, getErrorJson(e.getMessage()));
    }
  }

  protected JSONObject doPostInternal(HttpServletRequest req, HttpServletResponse resp)
      throws DataInternalException, ValidationException, DataRequestException, NotFoundException {
    throw new UnsupportedOperationException();
  }

  @Override
  protected final void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    try {
      JSONObject response = doPostInternal(req, resp);
      Asserts.assertTrue(response.getBoolean("success"), "success in response");
      writeJson(resp, response);
    } catch (UnsupportedOperationException e) {
      resp.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
      writeJson(resp, getErrorJson(e.getMessage()));
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
}
