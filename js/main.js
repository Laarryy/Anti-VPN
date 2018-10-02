
$(document).ready(function() {
  $('#spigot').click(function() {
    $('#spigot').text("Downloading..");
    $('#spigot').contents().unwrap();
    download(58291);
    return false;
  });
  $('#bungee').click(function() {
    $('#bungee').text("Downloading..");
    $('#bungee').contents().unwrap();
    download(58716);
    return false;
  });
});

function download(id) {
  $.getJSON('https://api.spiget.org/v2/resources/' + id, function(data) {
    top.location.href = "https://www.spigotmc.org/" + data.file.url;
  });
}
