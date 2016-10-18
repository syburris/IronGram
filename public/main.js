$.get(
    "/user",
    function(data) {
        if (data.length > 0) {
            $("#loginForm").hide();
            $("#logoutForm").show();
        }
    }
);