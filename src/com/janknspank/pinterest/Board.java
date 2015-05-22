package com.janknspank.pinterest;

import org.json.JSONObject;

/**
 * Data object for Pinterest board attributes.
 */
public class Board {
  // A Pinterest board has the following attributes, as represented in JSON
  // form:
  //
  // {
  //   "is_collaborative":false,
  //   "layout":"default",
  //   "description":"",
  //   "cover_images":{
  //      "400x300":{
  //         "url":"https://s-media-cache-ak0.pinimg.com/400x300/0b/14/3e/0b143e353ca2e3a40d05429787ec1f3c.jpg",
  //         "width":400,
  //         "height":300
  //      },
  //      "216x146":{
  //         "url":"https://s-media-cache-ak0.pinimg.com/216x146/0b/14/3e/0b143e353ca2e3a40d05429787ec1f3c.jpg",
  //         "width":216,
  //         "height":146
  //      }
  //   },
  //   "privacy":"public",
  //   "url":"/spotternews/internet/",
  //   "pin_count":1,
  //   "pin_thumbnail_urls":[],
  //   "image_thumbnail_url":"https://s-media-cache-ak0.pinimg.com/upload/463941267802589721_board_thumbnail_2015-05-21-01-27-15_65952_60.jpg",
  //   "access":["write", "delete"],
  //   "collaborated_by_me":false,
  //   "owner":{
  //      "username":"spotternews",
  //      "domain_verified":true,
  //      "image_medium_url":"https://s-media-cache-ak0.pinimg.com/avatars/spottern_1432169529_150.jpg",
  //      "explicitly_followed_by_me":false,
  //      "full_name":"Spotter",
  //      "type":"user",
  //      "id":"463941336521827516"
  //   },
  //   "followed_by_me":true,
  //   "type":"board",
  //   "id":"463941267802589721",
  //   "name":"Internet"
  // }

  private final String id;
  private final String name;
  private final int pinCount;

  public Board(JSONObject boardObject) {
    this.id = boardObject.getString("id");
    this.name = boardObject.getString("name");
    this.pinCount = boardObject.getInt("pin_count");
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public int getPinCount() {
    return pinCount;
  }
}
