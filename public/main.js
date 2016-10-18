$.get(
    "/user",
    function(data) {
        if (data.length > 0) {
            $("#loginForm").hide();
            $("#logoutForm").show();
            $("#uploadForm").show();
        }
    }
);

$.get(
    "/images",
    function(data) {
        var images = JSON.parse(data);
        for (var i in images) {
            var elem = $("<img>");
            elem.attr("src", "images/" + images[i].filename);
            $("#images").append(elem);
        }
    }
);