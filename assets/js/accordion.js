var acc = document.getElementsByClassName("accordion");
var i;

for (i = 0; i < acc.length; i++) {
    acc[i].onclick = function(){
        this.classList.toggle("active");
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

function displaySection(pubType) {
  if (pubType === "journals") {
    console.log('journals');
    document.getElementById("journals").style.display = "block";
    document.getElementById("conferences").style.display = "none";
  } else if (pubType === "conferences") {
    console.log('conferences');
    document.getElementById("conferences").style.display = "block";
    document.getElementById("journals").style.display = "none";
  }
}
