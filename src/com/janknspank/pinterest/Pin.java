package com.janknspank.pinterest;

import org.json.JSONObject;

/**
 * Data object for Pinterest pin attributes.
 */
public class Pin {
  // A Pinterest pin has the following attributes, as represented in JSON
  // form:
  //
  // {
  //   "images":{
  //     "237x":{
  //        "width":237,
  //        "url":"http://media-cache-ec0.pinimg.com/237x/85/18/c8/8518c8f4cd62228eacda1b5a0bdfd93b.jpg",
  //        "height":95
  //     }
  //   },
  //   "is_video":false,
  //   "dominant_color":"#798491",
  //   "like_count":0,
  //   "attribution":null,
  //   "link":"http://venturebeat.com/2015/05/21/trouble-for-fitbit-ipo-fitness-bands-losing-ground-to-smartwatches/",
  //   "description":"While Fitbit still dominates the market for fitness bands, consumers are showing a growing preference for smartwatches, according to a new report from Argus Insights.",
  //   "embed":null,
  //   "id":"463941199091128479",
  //   "pinner":{
  //     "full_name":"Spotter",
  //     "profile_url":"http://www.pinterest.com/spotternews/",
  //     "about":"Spotter finds the best content based on your interests.  Discover more with our iPhone app here: http://appstore.com/id966430113",
  //     "pin_count":11,
  //     "location":"",
  //     "id":"463941336521827516",
  //     "follower_count":1,
  //     "image_small_url":"http://media-cache-ak0.pinimg.com/avatars/spottern_1432169529_30.jpg"
  //   },
  //   "repin_count":0,
  //   "board":{
  //     "image_thumbnail_url":"http://media-cache-ec0.pinimg.com/upload/463941267802589970_board_thumbnail_2015-05-22-02-12-57_34163_60.jpg",
  //     "pin_count":1,
  //     "name":"Health and Fitness",
  //     "description":"",
  //     "id":"463941267802589970",
  //     "follower_count":0,
  //     "url":"/spotternews/health-and-fitness/"
  //   }
  // }

  private final String id;
  private final String link;
  private final String description;
  private final int likeCount;

  public Pin(JSONObject pinObject) {
    this.id = pinObject.getString("id");
    this.link = pinObject.getString("link");
    this.description = pinObject.getString("description");
    this.likeCount = pinObject.getInt("like_count");
  }

  public String getId() {
    return id;
  }

  public String getLink() {
    return link;
  }

  public String getDescription() {
    return description;
  }

  public int getLikeCount() {
    return likeCount;
  }

  @Override
  public String toString() {
    return link + " (" + description + ")";
  }
}
