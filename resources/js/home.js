
$(document).ready(function() {
  var iOS = ( navigator.userAgent.match(/(iPad|iPhone|iPod)/g) ? true : false );
  if (iOS == true) {
    if (confirm('Would you like to visit the App Store?')) {
      // Redirect to app store
      window.location.href = "http://itunes.apple.com/app/id966430113";
    } else {
      // Do nothing!
    }
  }
});

