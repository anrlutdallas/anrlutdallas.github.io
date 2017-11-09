/*
 * Arduino Wiring
 * GND -> GND
 * A5 -> A5
 * A4 -> A4
 */

#include <Wire.h>
#include "Adafruit_FONA.h"
#include <SoftwareSerial.h>

//Pins used for software serial
#define FONA_RX 2
#define FONA_TX 3
#define FONA_RST 4

//SoftwareSerial is used to communicate with FONA
SoftwareSerial fonaSS = SoftwareSerial(FONA_TX, FONA_RX);
SoftwareSerial *fonaSerial = &fonaSS;

Adafruit_FONA fona = Adafruit_FONA(FONA_RST); //Fona object

//Only registers a crash if g-force exceeds this value
const int crashThreshold = 6;
const int potholeThreshold = 3;

unsigned long previousTime = 0; //Used to store time between each coordinate retrieval

const int success = 100;  //Faster blinks for success
const int fail = 500; //Slower blinks for failure

float latitude, longitude, dir, kph, mph, alt;  //GPS info as float
float prevLat, prevLong;  //Stores previous coordinates, used for speed approximation in getSpeed()

//Character Arrays for each necessary float value
volatile char latArr[15];
volatile char longArr[15];
volatile char mphArr[10];

char* driverID = "1\0"; //DriverID

const char* server = "54.202.247.75";  //IP address of our server
const uint16_t port = 43005;  //Port of server

volatile char data[128]; //Character array that will be created
char dataToSend[128];
//char* packet = "32.99 -96.75 44.2 15.2 7.1 2.7 1\r\n\0";  //Change 1
volatile int dataIndex = 0;
volatile bool crashDetected = false;

void setup() {
    Wire.begin(8); // join i2c bus with address #8
    Wire.onReceive(compileData); // register event

    pinMode(13,OUTPUT); //LED used for debugging when Serial Monitor is unavailable

    Serial.begin(115200); //Baud rate of 115200 must be used

    //Initialize FONA Shield
    digitalWrite(13,HIGH);
    Serial.println(F("Initializing FONA..."));
    fonaSerial->begin(4800);
    while (! fona.begin(*fonaSerial)) {
      Serial.println(F("Couldn't find FONA, trying again..."));
      blinkLED(fail);
    }
    Serial.println(F("FONA initialization succesful"));
    blinkLED(success);

    //Enable GPS
    digitalWrite(13,HIGH);
    Serial.println(F("Enabling GPS..."));
    fona.enableGPS(true);
    Serial.println(F("GPS Enabled"));
    blinkLED(success);
    
    //Initializing GPRS
    digitalWrite(13,HIGH);
    Serial.println(F("Enabling GPRS..."));
    while (!fona.enableGPRS(true)){
      delay(1000);
    }
    blinkLED(success);

    digitalWrite(13,HIGH);
    Serial.println("Opening TCP Communication...");
    fona.TCPconnect(server,port);
    while (!fona.TCPconnected()){
      blinkLED(fail);
    }
    blinkLED(success);

    //Change 2
    //fona.TCPsend(packet,lastIndex(packet) + 1);
}

void loop() {
  //detectCrash();
  unsigned long currentTime = millis();

  boolean foundGPS = fona.getGPS(&latitude, &longitude, &kph, &dir, &alt); //Finds gps values

  detectCrash();
  if (foundGPS){  //Prints GPS coordinates if GPS signal is found
    Serial.print("Latitude: ");
    Serial.println(latitude, 6);
    Serial.print("Longitude: ");
    Serial.println(longitude, 6);
    Serial.print("Speed (MPH): ");
    mph = kph * 0.621371192;  //Converts from KPH to MPH
    Serial.println(mph);
  }
  else{   //If no GPS signal, use GSM location instead
    Serial.println("No GPS signal. Obtaining GSM location...");
    if (fona.getNetworkStatus() == 1) {
      detectCrash();
      boolean gsmloc_success = fona.getGSMLoc(&latitude, &longitude);
      detectCrash();
      if (gsmloc_success) {
        mph = getSpeed(prevLat, latitude, prevLong, longitude, currentTime - previousTime);
        previousTime = currentTime;
        prevLat = latitude;
        prevLong = longitude;
        
        Serial.print("GSM Latitude: ");
        Serial.println(latitude, 6);
        Serial.print("GSM Longitude: ");
        Serial.println(longitude, 6);
        Serial.print("Approximate Speed: ");
        Serial.println(mph);
      } 
      else {  //Restart GPRS so that GSM location can be found next time
        Serial.println("GSM location failed...");
        Serial.println(F("Disabling GPRS"));
        fona.enableGPRS(false);
        detectCrash();
        Serial.println(F("Enabling GPRS"));
        if (!fona.enableGPRS(true)) {
          Serial.println(F("Failed to turn GPRS on"));  
        }
      }
    }
  }
}

//Blinks LED on Pin 13
void blinkLED(int milli){
  long startMillis = millis();
  while (millis() - startMillis < 1000){
    digitalWrite(13,HIGH);
    delay(milli);
    digitalWrite(13,LOW);
    delay(milli);
  }
  digitalWrite(13,LOW);
  delay(500);
}

//Obtains speed 
float getSpeed(float lat1, float lat2, float long1, float long2, float timeInterval){

  //Converts coordinates from degrees to radians
  lat1 = lat1 * 3.141592 / 180;
  lat2 = lat2 * 3.141592 / 180;
  long1 = long1 * 3.141592 / 180;
  long2 = long2 * 3.141592 / 180;

  //Uses the Haversine Formula to calculate Great Sphere distance in order to find MPH
  float distance = pow(sin((lat2 - lat1)/2),2) + cos(lat1) * cos(lat2) * pow(sin((long2 - long1) / 2),2);
  distance = 2 * atan2(sqrt(distance),sqrt(1-distance));
  distance = 3956 * distance;

  return (distance / timeInterval * 1000);
}

int lastIndex(char* arr){
  int count = 0;
  while (arr[count] != '\0'){
    count++;
  }
  return count;
}

void compileData(int howMany) {
  dataIndex = 0;
  
  memset(data,0,sizeof(data));
  memset(latArr,0,sizeof(latArr));
  memset(longArr,0,sizeof(longArr));
  memset(mphArr,0,sizeof(mphArr));

  dtostrf(latitude,2,2,latArr);
  dtostrf(longitude,2,2,longArr);
  dtostrf(mph,2,2,mphArr);
  
  data[dataIndex++] = '*';
  copyToData(latArr);
  data[dataIndex] = ' ';
  data[++dataIndex] = ' ';
  copyToData(longArr);
  data[dataIndex] = ' ';
  data[++dataIndex] = ' ';
  copyToData(mphArr);
  data[dataIndex] = ' ';
  data[++dataIndex] = ' ';
  
  while (Wire.available()) { // loop through all but the last
    char x = Wire.read();    // receive byte as an integer
    data[dataIndex] = x;
    dataIndex++;
  }
  
  data[dataIndex] = ' ';
  dataIndex++;
  copyToData(driverID);
  data[dataIndex++] = '*';
  data[dataIndex] = '\r';
  data[++dataIndex] = '\n';
  data[++dataIndex] = '\0';

  crashDetected = true;
  Serial.println("Data has been compiled.");
}

void copyToData(char* arr){
  for (int count = 0; count < lastIndex(arr); count++){
    data[dataIndex] = arr[count];
    dataIndex++;
  }
}

void detectCrash(){
  if (crashDetected){
    memcpy(dataToSend,data,lastIndex(data) + 1);
    fona.TCPsend(dataToSend,lastIndex(data) + 1);
    Serial.println(dataToSend);
  }

  crashDetected = false;
}

