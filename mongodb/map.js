
// Called by MongoDB.  "this" is an Article object.
function() {
  var log = log || { push: function() {} };
  log.push("URL ID: " + this._id.$oid);
  log.push("userIndustryFeatureCode = " + userIndustryFeatureCode);

  // TODO(tomch): Look at the features and other attributes stored in 'this'
  // Article object and determine a score.  If the score is reasonably high,
  // emit an article + score 'object' of the form {'article': Article,
  // 'score': double} on key 0.
  var score = 0;
  for (var i = 0; i < this.feature.length; i++) {
    log.push("similarity(" + this.feature[i].feature_id + ") = " + this.feature[i].similarity);
    if (this.feature[i].feature_id == userIndustryFeatureCode ||
        this.feature[i].feature_id == 20000 /* Startup vector */) {
      score = Math.max(score, this.feature[i].similarity);
    }
  }
  emit(0, {
    'object': {
      'article': this,
      'url_id': this._id,
      'score': score * (3 / 4)
    }
  });
}
