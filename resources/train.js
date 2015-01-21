function updateRangeLabel(newVal){
	var userString;
	if (newVal > 90) {
		userString = newVal + " (worth a push notification)";
	}
	else if (newVal > 70) {
		userString = newVal + " (want it near the top of my stream)";
	}
	else if (newVal >= 50) {
		userString = newVal + " (belongs in my stream)";
	}
	else if (newVal > 25) {
		userString = newVal + " (not interesting)";
	}
	else {
		userString = newVal + " (garbage to me)";
	}
  document.getElementById("qualityScoreLabel").innerHTML=userString;
}

$( document ).ready(function() {
 
	updateRangeLabel(50);
	
    // Your code here.
	$( "#form" ).submit(function( event ) {
	  event.preventDefault();
	  
	  $("#submit").prop("disabled",true);
	  
	  var dataToPost = $('#form').serialize()
	  alert(dataToPost);
	  $.post( 'train', dataToPost, function(data) {
	         if (data.success) {
	        	 location.reload();
	         }
	       },
	       'json' // I expect a JSON response
	    ).fail(function() {
	        alert( "Error submitting. Try again." );
	        $("#submit").prop("disabled",false);
	    });
	});
});