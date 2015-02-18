
function (key, values) {
  var x = {'reduced': true};
  for (var i = 0; i < values.length; i++) {
    if (values[i].reduced == true) {
      for (var attributeName in values[i]) {
        x[attributeName] = values[i][attributeName];
      }
    } else {
      x[values[i].url_id] = values[i].article;
    }
  }
  return x;
}