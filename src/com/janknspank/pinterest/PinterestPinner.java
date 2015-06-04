package com.janknspank.pinterest;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.google.api.client.util.Sets;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.janknspank.bizness.ArticleFeatures;
import com.janknspank.bizness.Articles;
import com.janknspank.bizness.BiznessException;
import com.janknspank.bizness.TimeRankingStrategy.IndustryStreamStrategy;
import com.janknspank.bizness.Users;
import com.janknspank.classifier.FeatureId;
import com.janknspank.fetch.FetchException;
import com.janknspank.fetch.FetchResponse;
import com.janknspank.fetch.Fetcher;
import com.janknspank.proto.ArticleProto.Article;
import com.janknspank.proto.UserProto.Interest;
import com.janknspank.proto.UserProto.Interest.InterestType;
import com.janknspank.proto.UserProto.User;
import com.janknspank.rank.DiversificationPass;

/**
 * Runnable task that pins the best article we have for each respective feature
 * board we have in our Spotter Pinterest account.  E.g. Photography gets a
 * photography pin, Arts gets an artistic pin, then Restaurants, Wine &
 * Spirits, etc.
 */
public class PinterestPinner {
  /**
   * Pinterest.com base URL.
   */
  public static final String PINTEREST_URL = "https://www.pinterest.com";

  /**
   * Pinterest.com API URL.
   */
  public static final String PINTEREST_API_URL = "https://api.pinterest.com";

  private Fetcher fetcher = new Fetcher();
  private ConstantsRetriever constantsRetriever = new ConstantsRetriever(fetcher);
  private Map<String, String> cookies = Maps.newHashMap();

  /**
   * Pinterest account login (aka username).
   */
  private String username = null;

  /**
   * Pinterest account password.
   */
  private String password = null;

  /**
   * Board ID where the pin should be added to.
   */
  private String boardId = null;

  /**
   * The link to pin.  This needs to be a URL.
   */
  private String link;

  /**
   * The description for the pin.  This is like a paragraph of text.
   */
  private String description;

  /**
   * Link to an image.
   */
  private String imageUrl;

  /**
   * If true pinterest.com will automatically share new pin on connected facebook account
   */
  private boolean shareFacebook = false;

  public PinterestPinner() {
  }

  /**
   * Set Pinterest account login.
   */
  public PinterestPinner setUsername(String username) {
    this.username = username;
    return this;
  }

  /**
   * Set Pinterest account password.
   */
  public PinterestPinner setPassword(String password) {
    this.password = password;
    return this;
  }

  /**
   * Set Pinterest board ID to add pin to.
   */
  public PinterestPinner setBoardId(String boardId) {
    this.boardId = boardId;
    return this;
  }

  /**
   * Set pin image URL.
   */
  public PinterestPinner setImage(String imageUrl) {
    this.imageUrl = imageUrl;
    return this;
  }

  /**
   * Set pin description.
   */
  public PinterestPinner setDescription(String description) {
    this.description = description;
    return this;
  }

  /**
   * Set pin link.
   */
  public PinterestPinner setLink(String link) {
    this.link = link;
    return this;
  }

  /**
   * Set 'Share on Facebook' option.
   */
  public PinterestPinner setShareFacebook(boolean shareFacebook) {
    this.shareFacebook = shareFacebook;
    return this;
  }

  /**
   * Create a new pin.
   */
  public void pin() throws FetchException, BiznessException {
    this.postLogin();
    String pinId = this.postPin();
    System.out.println("Created pin: " + pinId);
  }

  private Multimap<String, String> getHeaders()
      throws FetchException, BiznessException {
    ImmutableMultimap.Builder<String, String> builder = ImmutableMultimap.builder();
    builder.put("X-NEW-APP", "1");
    builder.put("X-APP-VERSION", constantsRetriever.getAppVersion());
    builder.put("X-Requested-With", "XMLHttpRequest");
    builder.put("X-Pinterest-AppState", "active");
    builder.put("Accept", "application/json, text/javascript, */*; q=0.01");
    builder.put("X-CSRFToken", cookies.containsKey("csrftoken")
        ? cookies.get("csrftoken") : constantsRetriever.getCsrfToken());
    builder.put("Referer", PINTEREST_URL);

    // Cookies need to look like this:
    // Cookie:__utmt=1; __utma=229774877.1818859653.1432240761.1432240761.143224
    // 0761.1; __utmb=229774877.1.10.1432240761; __utmc=229774877; __utmz=229774
    // 877.1432240761.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none); cm_sub=
    // denied; _b="ARCdsyo9metKEYskTqT8tmAeqf2f20wCvZ8RDYD3BDqoM1Gz9HPf2EEiYlJKa
    // tG19YQ="; csrftoken=x4GqbaLtCbgBWyIJxIyIePjlEaga0LEA; _pinterest_sess="TW
    // c9PSZ6Y2d4VUs2S1MzdExxUGRHVXV2eERZczdlL0U4TkorL3U5OG4wYTJwaGNjTGFKOE1Mck9
    // IMnZNNzgxYUJXMnRoZUcyQjFOQzl4dzloVlJvSjl2Q0YvVUhjdkFEaXVUb25zR3pNRGxUdXJ4
    // cWVmelRCNzhxdFRDNzQ1b3Z0emlhU1pBN2xnR3lCeUdkMnVnQ2ZmaUpaUW4xNDZxKzFNUmMxd
    // FJLeUtmVzh1T1RXNllaek5DZG93a29OZ3VwWHJ6L0w2OS82aXptU1A0cjlqdEk5NHBLR2ZaMk
    // ZwOEtQejU5WjAvRUlQbW9GZTJDQUZqZEVNd3Q3dmoySGF3QUZMYXBVZ1dOenhyUFpOSVZOQ3l
    // vZkdkSEZZN1E3ZjNaL3FRa0JnUGZoTW5PRmZ5STBtajl6VERsZGppUGlUc3hGVE1ackx6d0Fk
    // dzI3L20vY2E2SXQ5bGg5SC9mbmV4Z1V4NE43aitoRHowdUNQOXhXQ0NsRlRxSURGY0NyeUdEZ
    // 3czTTNlQmpaeC9aYU0vTEs4cThQMVpOaFVmMGFpT1lreUVvTE5xKy9jdExmL280RXc5akxzMn
    // hONlJnNUZvamk5K2hNMnBzVHlJY2o3SGNGRXVtc1B3VjZ6alUxaEd4dm5VWjhCY0Y4ZWo5Sk1
    // 4QT0mK29UU1JSSER4YTNvczFFK3ZhOWZ2ZmVsMHV3PQ=="; _pinterest_pfob=disabled;
    // c_dpr=2
    // Notice specifically:
    // _pinterest_sess (which includes quotes!)
    // _b (also with quote!)
    // csrftoken (but no quotes on this!)
    builder.put("Cookie", Joiner.on("; ").withKeyValueSeparator("=").join(cookies));

    return builder.build();
  }

  /**
   * Try to log in to Pinterest.  Returns the login cookies.
   */
  private void postLogin() throws FetchException, BiznessException {
    JSONObject optionsObject = new JSONObject();
    optionsObject.put("username_or_email", this.username);
    optionsObject.put("password", this.password);

    JSONObject dataObject = new JSONObject();
    dataObject.put("options", optionsObject);
    dataObject.put("context", new JSONObject());

    List<NameValuePair> postParameters = Lists.newArrayList();
    postParameters.add(new BasicNameValuePair("data", dataObject.toString()));
    postParameters.add(new BasicNameValuePair("source_url", "/login/"));
    postParameters.add(new BasicNameValuePair("module_path",
        "App()>LoginPage()>Login()>Button("
        + "class_name=primary, text=Log In, type=submit, size=large)"));
    FetchResponse response = fetcher.post(
        PINTEREST_URL + "/resource/UserSessionResource/create/", postParameters,
        getHeaders());
    cookies.put("_pinterest_sess", response.getSetCookieValue("_pinterest_sess"));
    cookies.put("_b", response.getSetCookieValue("_b"));
    cookies.put("csrftoken", response.getSetCookieValue("csrftoken"));
  }

  /**
   * Try to create a new pin.
   * @return the ID of the newly created pin
   */
  private String postPin() throws FetchException, BiznessException {
    JSONObject optionsObject = new JSONObject();
    optionsObject.put("board_id", this.boardId);
    optionsObject.put("description", this.description);
    optionsObject.put("link", this.link);
    optionsObject.put("share_facebook", this.shareFacebook);
    optionsObject.put("image_url", this.imageUrl);
    optionsObject.put("method", "scraped");

    JSONObject dataObject = new JSONObject();
    dataObject.put("options", optionsObject);
    dataObject.put("context", new JSONObject());

    List<NameValuePair> postParameters = Lists.newArrayList();
    postParameters.add(new BasicNameValuePair("data", dataObject.toString()));
    postParameters.add(new BasicNameValuePair("source_url", "/"));
    postParameters.add(new BasicNameValuePair("module_path",
        "App()>ImagesFeedPage(resource=FindPinImagesResource(url=" + this.link + "))"
            + ">Grid()>GridItems()>Pinnable(url=" + this.imageUrl + ", type=pinnable, link="
            + this.link + ")#Modal(module=PinCreate())"));
    String responseBody = fetcher.postResponseBody(
        PINTEREST_URL + "/resource/PinResource/create/", postParameters, getHeaders());

    JSONObject responseObject = new JSONObject(responseBody);
    JSONObject resourceResponseObject = responseObject.getJSONObject("resource_response");
    JSONObject responseDataObject = resourceResponseObject.getJSONObject("data");
    return responseDataObject.getString("id");
  }

  /**
   * Get user's pins.
   */
  public List<Pin> getPins() throws FetchException, BiznessException {
    try {
      String responseBody = fetcher.getResponseBody(
          PINTEREST_API_URL + "/v3/pidgets/users/" + this.username + "/pins/",
          this.getHeaders());
      JSONObject responseObject = new JSONObject(responseBody);
      JSONObject dataObject = responseObject.getJSONObject("data");
      JSONArray pinsArray = dataObject.getJSONArray("pins");
      List<Pin> pins = Lists.newArrayList();
      for (int i = 0; i < pinsArray.length(); i++) {
        pins.add(new Pin(pinsArray.getJSONObject(i)));
      }
      return pins;
    } catch (JSONException e) {
      throw new BiznessException("Could not read pins: " + e.getMessage(), e);
    }
  }

  /**
   * Get logged in user data.
   */
  public JSONObject getUserData() throws FetchException, BiznessException {
    String response = fetcher.getResponseBody(PINTEREST_URL + "/me/", this.getHeaders());
    JSONObject userDataObject = new JSONObject(response);
    JSONArray resourceDataCache = userDataObject.getJSONArray("resource_data_cache");
    JSONObject dataObject = resourceDataCache.getJSONObject(0);
    return dataObject;
  }

  public List<Board> getBoards() throws FetchException, BiznessException {
    JSONObject optionsObject = new JSONObject();
    optionsObject.put("field_set_key", "grid_item");
    optionsObject.put("username", this.username);

    JSONObject dataObject = new JSONObject();
    dataObject.put("options", optionsObject);
    dataObject.put("context", new JSONObject());

    URIBuilder b;
    try {
      b = new URIBuilder("https://www.pinterest.com/resource/ProfileBoardsResource/get/");
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
    b.addParameter("source_url", "/" + this.username + "/");
    b.addParameter("data", dataObject.toString());
    b.addParameter("_", Long.toString(System.currentTimeMillis()));
    String response = fetcher.getResponseBody(b.toString(), getHeaders());

    JSONObject responseObject = new JSONObject(response);
    JSONArray resourceDataCache = responseObject.getJSONArray("resource_data_cache");
    JSONArray responseDataObject = resourceDataCache.getJSONObject(0).getJSONArray("data");
    List<Board> boards = Lists.newArrayList();
    for (int i = 0; i < responseDataObject.length(); i++) {
      JSONObject itemObject = responseDataObject.getJSONObject(i);
      if ("board".equals(itemObject.getString("type"))) {
        boards.add(new Board(itemObject));
      }
    }
    return boards;
  }

  public static void main(String args[]) throws Exception {
    PinterestPinner pinner = new PinterestPinner();
    pinner.setUsername("spotternews");
    pinner.setPassword("HotSpot1");
    pinner.postLogin();

    // If we find anything we've posted already, skip that category.  (Don't
    // post lower ranking articles just to post.)
    Set<String> existingPinLinks = Sets.newHashSet();
    for (Pin pin : pinner.getPins()) {
      existingPinLinks.add(pin.getLink());
    }

    // Use a dummy user for this.  We're going to set all his interests manually
    // anyway, to match the Pinterest board being posted to.
    User user = Users.getByEmail("new@relic.net");
    for (Board board : pinner.getBoards()) {
      FeatureId featureId = FeatureId.fromTitle(board.getName());
      if (featureId != null) {
        user = user.toBuilder()
            .clearInterest()
            .addInterest(Interest.newBuilder()
                .setType(InterestType.INDUSTRY)
                .setIndustryCode(featureId.getId())
                .build())
            .build();
        Iterable<Article> articles = Articles.getStream(
            user,
            new IndustryStreamStrategy(),
            new DiversificationPass.IndustryStreamPass(),
            ImmutableSet.<String>of());
        for (Article article : articles) {
          if (existingPinLinks.contains(article.getUrl())) {
            // If we've already posted the best article for this feature, OK
            // great, let's skip posting here.  We'll post again when we have
            // something even better.
            break;
          }

          boolean isArtistic =
              ArticleFeatures.getFeatureSimilarity(article, FeatureId.ARTS) >= 0.9
              || ArticleFeatures.getFeatureSimilarity(article, FeatureId.PHOTOGRAPHY) >= 0.8
              || ArticleFeatures.getFeatureSimilarity(article,
                  FeatureId.APPAREL_AND_FASHION) >= 0.95
              || ArticleFeatures.getFeatureSimilarity(article,
                  FeatureId.ARCHITECTURE_AND_PLANNING) >= 0.9;
          boolean isAboutMoney =
              ArticleFeatures.getFeatureSimilarity(article, FeatureId.INTERNET) >= 0.1
              || ArticleFeatures.getFeatureSimilarity(article, FeatureId.EQUITY_INVESTING) >= 0.1
              || ArticleFeatures.getFeatureSimilarity(article, FeatureId.VENTURE_CAPITAL) >= 0.1
              || ArticleFeatures.getFeatureSimilarity(article, FeatureId.STARTUPS) >= 0.42;
          boolean isNews =
              ArticleFeatures.getFeatureSimilarity(article,
                  FeatureId.TOPIC_POLITICS) >= 0.3
              || ArticleFeatures.getFeatureSimilarity(article,
                  FeatureId.TOPIC_MURDER_CRIME_WAR) >= 0.25;
          if (article.hasImageUrl()
              && isArtistic
              && !isAboutMoney
              && !isNews) {
            pinner.setBoardId(board.getId());
            pinner.setLink(article.getUrl());
            pinner.setImage(article.getImageUrl());
            pinner.setDescription(article.getTitle());
            pinner.postPin();
            System.out.println("Posting " + article.getTitle() + " to " + board.getName());
            break;
          }
        }
      }
    }
    System.exit(0);
  }
}
