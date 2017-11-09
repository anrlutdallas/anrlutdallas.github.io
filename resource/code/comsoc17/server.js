const path = require('path')
const express = require('express');  
const web_app = express();
const https = require('https');  
const fs = require('fs');
const cors = require('cors')
const request = require('request');
const http = require('http');
const mysql = require('mysql');


const net = require('net');


let con = mysql.createConnection({
  host: "localhost",
  user: "anrl",
  password: "",
  database: "traffic"
})

con.connect(function(err) {
  if (err) throw err 
  console.log(`Connected to MySQL server!`)
})

web_app.use(cors())
web_app.options('*', cors())

let sslPath = '/etc/letsencrypt/live/project-ariadne.protobyte.info/'
let options = {
  key: fs.readFileSync(sslPath + 'privkey.pem'),
  cert: fs.readFileSync(sslPath + 'fullchain.pem')
}

http.createServer(function (req, res) {
    res.writeHead(301, { "Location": "https://" + req.headers['host'] + req.url });
    res.end()
}).listen(8080)

web_app.get('/', function(req, res){
  res.sendFile(path.resolve('index.html'))
})

web_app.get('/console', function(req, res, next){
  res.sendFile(path.resolve('/home/ubuntu/website/console.html'))
})

web_app.get('/analytics', function(req, res, next){
  res.sendFile(path.resolve('/home/ubuntu/website/analytics.html'))
})

web_app.get('/proxy', function(req, res){
  let url = 'https://www.google.com/'
  request(url, function(error, response, html){
    if(!error){
      res.setHeader('Content-Type', 'application/json');  
      res.json({proxyhtml: html});
    }
  }) 
})

const web_server = https.createServer(options, web_app);    
web_server.listen(443);  
const io = require('socket.io')(web_server)
console.log(`Serving website with SSL!`)

io.on('connection', function(socket){ 
  console.log('New client connection')
  con.query("SELECT date, latitude, longitude, speed, shock, type, message, address, name, licensePlate FROM events, drivers WHERE events.driverId = drivers.id", function (err, result, fields) {
    if (err) throw err
    let records = []
    records = result
    //console.log(records)
    socket.emit('client-connection', {'records': records})
  })
  socket.on('hoursPast', function(data) {
    con.query("SELECT date, latitude, longitude, speed, shock, type, message, address, name, licensePlate FROM events, drivers WHERE events.driverId =     drivers.id AND date > NOW() - INTERVAL " + data + " HOUR", function (err, result, fields) {
      if (err) throw err
      let records = []
      records = result
      console.log(records) 
      socket.emit('hours-result', {'records': records})
    })
  })
  socket.on('dateInterval', function(data){
    con.query("SELECT date, latitude, longitude, speed, shock, type, message, address, name, licensePlate FROM events, drivers WHERE events.driverId = drivers.id AND  date >= '" + data.startDate + " 00:00:00' AND date <= '" + data.endDate + " 00:00:00'", function (err, result, fields){
      if (err) throw err
      let records = []
      records = result
      socket.emit('dateinterval-result', {'records': records})
    }) 
  })
  socket.on('speedAnalytics', function(data){
    con.query("SELECT speed FROM events WHERE date >= '" + data.startDate + " 00:00:00' AND date <= '" + data.endDate + " 00:00:00'", function (err, result, fields){
      if (err) throw err
      let records = []
      result.forEach(function(record){
        records.push(record.speed)
      })
      socket.emit('speed-results', {'records': records})
    })
  })
  socket.on('comparisonAnalytics', function(data){
    let records = []
    for (var i=0; i<12; i++) {
      let start = data.bounds[i].start
      let end = data.bounds[i].end
      let last = data.bounds[i].last
      //console.log(data.bounds[i].start)
      let group = {'start':start, 'end':end, 'crash':0, 'pothole':0}
      con.query("SELECT * FROM events WHERE date >= '" + start + "' AND date <= '" + end + "'", function (err, result, fields){
	if (err) throw err
        //console.log(result)
	//console.log(result.length)
        result.forEach(function(record){
	  if (record.type == 0) {
	    group.pothole++
	  }
	  if (record.type == 1) {
	    group.crash++
	  }
	}) 
      	//console.log(group)
        records.push(group)
        if (last) {
	  socket.emit('comparison-results', {'records': records})
	}
      })
    } 
  })
})   
    


var events = []
var client = new net.Socket()
client.connect(43006, 'localhost', function() {
    console.log('Connected to Java server!')
    
    client.on('data', function(data) {
      //console.log(data)
      let result = "";
      let tempEvent = null
      for(let i = 0; i < data.length; ++i){
        result+= (String.fromCharCode(data[i]))
      }
      result = result.replace(/^\s+|\s+$/g, '')
      console.log(result)
      tempEvent = result.split(',')
      if (result.includes('Accident')) {
        console.log('accident')
	//events.push('accident')
       // var record = {
	//  'latitude': 32.983716,
	//  'longitude': -96.752076,
	//  'date': 
//	}
        io.emit('new-accident', 'asdf') 
      } else {
	console.log('pothole')
        io.emit('new-pothole', 'asdf')
      }
    }) 

    client.on('close', function() { 
       console.log('Connection closed')
    })
})  

client.on('error', function(ex) {
  console.log("TCP client error with code: " + ex.code)
});


/**
process.on('uncaughtException', function (err) {
  console.log(err);
})
*/
