package com.janknspank.server;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.janknspank.bizness.EntityType;
import com.janknspank.bizness.GuidFactory;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseRequestException;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.proto.CoreProto.Entity;
import com.janknspank.proto.UserProto.Interest;
import com.janknspank.proto.UserProto.Interest.InterestSource;
import com.janknspank.proto.UserProto.Interest.InterestType;
import com.janknspank.proto.UserProto.User;

@AuthenticationRequired(requestMethod = "POST")
public class SetUserInterestServlet extends StandardServlet {
  @Override
  protected JSONObject doPostInternal(HttpServletRequest req, HttpServletResponse resp)
      throws RequestException, DatabaseSchemaException, DatabaseRequestException {
    // Read parameters.
    String keyword = getRequiredParameter(req, "keyword");
    String type = getRequiredParameter(req, "type");
    String enabled = getRequiredParameter(req, "enabled");
    User user = Database.with(User.class).get(getSession(req).getUserId());

    // Parameter validation.
    if (EntityType.fromValue(type) == null) {
      throw new RequestException("Parameter 'type' is not valid");
    }

    // Business logic.
    // Find all interests that have nothing to do with the specified user
    // interest.
    List<Interest> existingInterests = Lists.newArrayList();
    for (Interest interest : user.getInterestList()) {
      if ((interest.getSource() == InterestSource.USER
              || interest.getSource() == InterestSource.TOMBSTONE)
          && interest.getType() == InterestType.ENTITY
          && interest.getEntity().getKeyword().equals(keyword)) {
        continue;
      }
      existingInterests.add(interest);
    }

    // Write the filtered list plus a new Interest that represents the
    // parameters received from the client.
    Database.with(User.class).set(user, "interest",
        Iterables.concat(
            existingInterests,
            ImmutableList.of(Interest.newBuilder()
                .setId(GuidFactory.generate())
                .setType(InterestType.ENTITY)
                .setSource("true".equals(enabled)
                    ? InterestSource.USER : InterestSource.TOMBSTONE)
                .setEntity(Entity.newBuilder()
                    .setId(GuidFactory.generate())
                    .setKeyword(keyword)
                    .setType(type)
                    .setSource(Entity.Source.USER))
                .setCreateTime(System.currentTimeMillis())
                .build())));

    // Write response.
    return createSuccessResponse();
  }
}
