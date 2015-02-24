package com.janknspank.rank;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;

import ro.isdc.wro.extensions.script.RhinoUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.janknspank.bizness.UserIndustries;
import com.janknspank.bizness.Users;
import com.janknspank.classifier.IndustryCode;
import com.janknspank.common.Logger;
import com.janknspank.database.Database;
import com.janknspank.database.DatabaseSchemaException;
import com.janknspank.database.MongoConnection;
import com.janknspank.database.Mongoizer;
import com.janknspank.database.QueryOption;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.UserProto.User;
import com.janknspank.proto.UserProto.UserIndustry;
import com.janknspank.proto.UserProto.UserIndustry.Relationship;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MapReduceCommand;
import com.mongodb.MapReduceOutput;

public class JavascriptScorer extends Scorer {
  private static final Logger LOG = new Logger(JavascriptScorer.class);
  private static final String JAVASCRIPT_MAPREDUCE_DIRECTORY = "mongodb/";
  private static final String JAVASCRIPT_MAP_FUNCTION_DEBUG = readJsFile("map.js", true);
  private static final String JAVASCRIPT_MAP_FUNCTION = readJsFile("map.js", false);
  private static final String JAVASCRIPT_REDUCE_FUNCTION = readJsFile("reduce.js", false);
  private static JavascriptScorer INSTANCE = null;

  private JavascriptScorer() {}

  public static synchronized JavascriptScorer getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new JavascriptScorer();
    }
    return INSTANCE;
  }

  private static final String readJsFile(String localFileName, boolean debug) {
    FileReader fileReader = null;
    BufferedReader jsFileReader = null;
    StringBuilder buffer = new StringBuilder();
    try {
      fileReader = new FileReader(JAVASCRIPT_MAPREDUCE_DIRECTORY + localFileName);
      jsFileReader = new BufferedReader(fileReader);
      String line = jsFileReader.readLine();
      while (line != null) {
        String trimLine = line.trim();
        if (debug || (!trimLine.startsWith("var log =") && !trimLine.startsWith("log.push("))) {
          int commentStart = trimLine.indexOf("//");
          buffer.append(commentStart == -1 ? trimLine : trimLine.substring(0, commentStart));
        }
        line = jsFileReader.readLine();
      }
      return buffer.toString();
    } catch (IOException e) {
      throw new Error("Could not read Javascript file: "
          + JAVASCRIPT_MAPREDUCE_DIRECTORY + localFileName);
    } finally {
      IOUtils.closeQuietly(fileReader);
      IOUtils.closeQuietly(jsFileReader);
    }
  }

  /**
   * Implements the {@link Scorer#getScore(User, Article)} function by running our
   * Map function against an Apache Rhino runtime.
   * NOTE(jonemerson): THIS IS NOT FOR PRODUCTION USE!!  In production, the
   * Javascript functions should run directly on MongoDB.  This is here so that
   * we can benchmark our algorithms on developer boxes without having to hit
   * Mongo.
   */
  @Override
  public double getScore(User user, Article article) {
    Context cx = Context.enter();
    try {
      // Initialize the standard objects (Object, Function, etc.)
      // This must be done before scripts can be executed.
      Scriptable scope = cx.initStandardObjects();

      // Set version to JavaScript1.2 so that we get object-literal style
      // printing instead of "[object Object]"
      cx.setLanguageVersion(Context.VERSION_1_2);

      // Fill the scope with variables about the user, so they're quick to access.
      BasicDBObject userScope = getScope(user);
      StringBuilder scriptBuilder = new StringBuilder();
      for (Map.Entry<String, Object> entry : userScope.entrySet()) {
        scriptBuilder.append("var " + entry.getKey() + " = " + toJSON(entry.getValue()) + ";\n");
      }

      // Build a scaffold for running MongoDB-style MapReduce functions inside
      // Apache Rhino Javascript runtime.
      scriptBuilder.append("var emitValue = undefined;\n");
      scriptBuilder.append("var log = [];\n");
      scriptBuilder.append("var emit = function(key, value) {\n");
      scriptBuilder.append("  emitValue = value;\n");
      scriptBuilder.append("};\n");
      scriptBuilder.append("var mapFunction = " + JAVASCRIPT_MAP_FUNCTION_DEBUG + ";\n");
      scriptBuilder.append("mapFunction.call(" + Mongoizer.toDBObject(article).toString() + ");\n");
      cx.evaluateString(scope, scriptBuilder.toString(), null, 1, null);

      // Handle the results.
      LOG.info("log = " + RhinoUtils.toJson(scope.get("log", scope), true));
      Object emitValue = scope.get("emitValue", scope);
      if (!(emitValue instanceof NativeObject)) {
        LOG.warning("Score not found for URL: " + article.getUrl());
        return 0;
      }
      NativeObject emitObject =
          (NativeObject) ((NativeObject) emitValue).get("object", (NativeObject) emitValue);
      return (Double) emitObject.get("score", emitObject);

    } catch (DatabaseSchemaException e) {
      LOG.error("Could not score article", e);
      return 0;
    } finally {
      Context.exit();
    }
  }

  /**
   * Old function that tested our MapReduce against MongoDB itself.  This code
   * will move to Articles, probably.
   */
  public static void main(String args[]) throws DatabaseSchemaException {
    // TODO(jonemerson): Tune the hell out of this!  We need to be SMART about
    // what Articles we run through:
    // - Newer articles: Good!
    // - Less new articles but very relevant to the user's industries: Good!
    // - Moderately new articles but relevant to the user's contacts: YES!!
    // - Other ideas?!
    User user = Users.getByEmail("panaceaa@gmail.com");
    BasicDBObject query = new BasicDBObject("$or", ImmutableList.of(
        new BasicDBObject("published_time",
            new BasicDBObject("$gte", System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1))),
        new BasicDBObject("feature.feature_id", getUserIndustryFeatureCode(user))));

    DBCollection articleCollection = MongoConnection.getDatabase().getCollection("Article");
    MapReduceCommand cmd = new MapReduceCommand(
        articleCollection,
        JAVASCRIPT_MAP_FUNCTION,
        JAVASCRIPT_REDUCE_FUNCTION,
        null,
        MapReduceCommand.OutputType.INLINE,
        query);
    cmd.addExtraOption("jsMode", true);
    cmd.addExtraOption("scope", getScope(user));

    MapReduceOutput out = articleCollection.mapReduce(cmd);
    for (DBObject o : out.results()) {
      System.out.println(o.toString());
    }
    System.out.println("Duration: " + out.getDuration());
  }

  /**
   * Helper function for getting the integer value of the user's current
   * industry.
   */
  public static int getUserIndustryCode(User user) {
    for (UserIndustry userIndustry : UserIndustries.getCurrentIndustries(user)) {
      if (userIndustry.getRelationship() == Relationship.CURRENT_INDUSTRY) {
        return userIndustry.getIndustryCodeId();
      }
    }
    return 0;
  }

  /**
   * Helper function for getting the FeatureId enum value for the user's
   * current industry.  (Basically, the 100xx version of it.)
   */
  public static int getUserIndustryFeatureCode(User user) {
    int userIndustryCode = getUserIndustryCode(user);
    return (userIndustryCode > 0)
        ? IndustryCode.fromId(userIndustryCode).getFeatureId().getId() : 0;
  }

  /**
   * Returns a set of Javascript/MongoDB objects that should be in the root
   * scope when running the Map and Reduce functions.  Anything returned here
   * will be accessible directly in the Javascript runtime.
   * NOTE(jonemerson): Please put anything even remotely computationally
   * intensive in here, and NOT in the map.js function.  Anything in map.js
   * will be re-interpreted thousands of times, once per article.  If you can
   * calculate things once, here, please do!
   */
  public static BasicDBObject getScope(User user) throws DatabaseSchemaException {
    BasicDBObject obj = new BasicDBObject();
    obj.put("user", Mongoizer.toDBObject(user));

    int userIndustryCode = getUserIndustryCode(user);
    obj.put("userIndustryCode", userIndustryCode);
    obj.put("userIndustryFeatureCode", IndustryCode.fromId(userIndustryCode).getFeatureId().getId());

    return obj;
  }

  /**
   * Helper function for converting MongoDB BasicDBObject values to JSON, so
   * that it can be consumed by Rhino.
   * NOTE(jonemerson): It's a bit weird we need to do this just for special
   * handling of String types.  Maybe we don't actually... but I'm pretty sure
   * we do.
   */
  public static String toJSON(Object obj) {
    if (obj instanceof String) {
      return "'" + StringEscapeUtils.escapeJson((String) obj) + "'";
    }
    return obj.toString();
  }

  public static void oldMain(String[] args) throws DatabaseSchemaException {
    User user = Users.getByEmail("panaceaa@gmail.com");

    // Now we can evaluate a script. Let's create a new object
    // using the object literal notation.
    Iterable<Article> articles = Database.with(Article.class).get(
        new QueryOption.WhereEqualsNumber("feature.feature_id", getUserIndustryFeatureCode(user)),
        new QueryOption.Limit(10));
    System.out.println("Processing " + Iterables.size(articles) + " articles...");
    JavascriptScorer scorer = new JavascriptScorer();
    for (Article article : articles) {
      System.out.println("Found score " + scorer.getScore(user, article) + " for " + article.getUrl());
    }
  }
}
