
$(document).ready(function() {
  $(".tab").click(function(e) {
    var tabId = $(this).attr("tab_id");
    var urlId = $(this).attr("url_id");
    $(".tab-bar .tab[url_id='" + urlId + "']").each(function(i, obj) {
      $(this).toggleClass("selected", $(this).attr("tab_id") == tabId);
    });
    $(".article-details[url_id='" + urlId + "']").each(function(i, obj) {
      if ($(this).hasClass(tabId)) {
        $(this).show();
      } else {
        $(this).hide();
      }
    });
  });
});
