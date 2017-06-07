var acc = document.getElementsByClassName("accordion");
var i;
var sectionType = '';

for (i = 0; i < acc.length; i++) {
    acc[i].onclick = function(){
        //this.classList.toggle("active");
        var panel = this.nextElementSibling;
        if (panel.style.display === "block") {
            panel.style.display = "none";
            //console.log('hide');
        } else {
            panel.style.display = "block";
            //console.log('show');
        }
    }
}

var QueryString = function () {
  // This function is anonymous, is executed immediately and
  // the return value is assigned to QueryString!
  var query_string = {};
  var query = window.location.search.substring(1);
  var vars = query.split("&");
  for (var i=0;i<vars.length;i++) {
    var pair = vars[i].split("=");
        // If first entry with this name
    if (typeof query_string[pair[0]] === "undefined") {
      query_string[pair[0]] = decodeURIComponent(pair[1]);
        // If second entry with this name
    } else if (typeof query_string[pair[0]] === "string") {
      var arr = [ query_string[pair[0]],decodeURIComponent(pair[1]) ];
      query_string[pair[0]] = arr;
        // If third or later entry with this name
    } else {
      query_string[pair[0]].push(decodeURIComponent(pair[1]));
    }
  }
return query_string;
}();

window.onload = function onLoad() {
  getSection(QueryString.id);
  window.history.replaceState({}, '', 'https://anrlutdallas.github.io/resources.html');
}

function getSection(sectionType) {
  console.log(sectionType);
  if (sectionType === "documents") {
    //console.log('journals');
    document.getElementById("documents-panel").style.display = "block";
    document.getElementById("documents-panel").scrollIntoView();
  } 
  if (sectionType === "slides") {
    //console.log('conferences');
    document.getElementById("slides-panel").style.display = "block";
    document.getElementById("slides-panel").scrollIntoView();
  } 
  if (sectionType === "whitepapers") {
    //console.log('conferences');
    document.getElementById("whitepapers-panel").style.display = "block";
    document.getElementById("whitepapers-panel").scrollIntoView();
  }
}