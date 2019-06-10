#include <Adafruit_Sensor.h>//1.0.3
#include "DHT.h" //1.3.4
//ArduinoJson v5.13.5


bool DEBUG = true;   //show more logs
int responseTime = 100; //communication timeout
String ssid = "ssid of you android smartphone";
String pwd = "password of the network";
String ipClient = "192.168.43.1";


//HC-SR04
// defines pins numbers
#define TRIGPIN  5
#define ECHOPIN 6
#define DHTPIN A0      // DHT-22 Output Pin connection
#define DHTTYPE DHT11   // DHT Type is DHT 11

// defines variables
long duration;
float distance;

float hum;    // Stores humidity value in percent
float temp;   // Stores temperature value in Celcius
float soundsp;  // Stores calculated speed of sound in M/S
float soundcm;  // Stores calculated speed of sound in cm/ms
int iterations = 5;
 
// Initialize DHT sensor for normal 16mhz Arduino
 
DHT dht(DHTPIN, DHTTYPE); 




void setup()
{
  //HC-SR04
  pinMode(TRIGPIN, OUTPUT); // Sets the TRIGPIN as an Output
  pinMode(ECHOPIN, INPUT); // Sets the ECHOPIN as an Input

  pinMode(13, OUTPUT); //set build in led as output
  // Open serial communications and wait for port to open esp8266:
  Serial.begin(115200);
  Serial1.begin(115200);

  boolean isError = false;

  String responseAT = sendToWifi("AT", responseTime, DEBUG); // configure as station
  if (find(responseAT, "OK")){
    String responseATCWMODE = sendToWifi("AT+CWMODE=1", responseTime, DEBUG); // configure as station
     if (find(responseATCWMODE, "OK")){
          String responseATCWJAP = sendToWifi("AT+CWJAP=\"" + ssid + "\",\"" + pwd+"\"", responseTime+5000, DEBUG); // connect to the wifi
          delay(6000);
          if(find(responseATCWJAP, "OK")){
            String responseATCIFSR = sendToWifi("AT+CIFSR", responseTime, DEBUG); // get ip address
            if(find(responseATCIFSR, "OK")){
              sendToDue("Wifi connection is running!", responseTime, DEBUG);
//              String responseATCIPSTART = sendToWifi("AT+CIPSTART=\"TCP\",\"192.168.43.1\",8080", responseTime+500, DEBUG); 
//              if(find(responseATCIPSTART, "OK")){
//                Serial.println("Prova AT+CIPSEND");
//                String responseATCIPSEND = sendToWifi("AT+CIPSEND=4", responseTime, DEBUG);
//                if(find(responseATCIPSEND, ">")){ 
//                  sendToWifi("test", responseTime, DEBUG);
//                  sendToWifi("AT+CIPCLOSE", responseTime, DEBUG);  
//                }
//                
//              }else{
//                isError = true; 
//              }
            }else{
              isError = true;
            }
          }else{
            isError = true;
          }
     }else{
      isError = true;
     }
    
  }else{
    isError = true;
  }

  if(isError){
    sendToDue("Wifi connection is NOT running!", responseTime, DEBUG);
  }else{
      hum = dht.readHumidity();  // Get Humidity value
      while(isnan(hum)){
        hum = dht.readHumidity();
      }
      temp= dht.readTemperature();  // Get Temperature value
      while(isnan(temp)){
        temp = dht.readHumidity();
      }
  }


}


void loop()
{
  if (Serial.available() > 0) {
    
    String message = readSerialMessage();
    Serial.println("message from Serial: "+message);
    if (find(message, "debugEsp8266:")) {
      String result = sendToWifi(message.substring(13, message.length()), responseTime, DEBUG);
      if (find(result, "OK"))
        sendData("\nOK");
      else
        sendData("\nEr");
    }
  }
  if (Serial1.available() > 0) {

    String message = readWifiSerialMessage();
    Serial.println("message: "+message);
    if (find(message, "esp8266:")) {
      String result = sendToWifi(message.substring(8, message.length()), responseTime, DEBUG);
      if (find(result, "OK"))
        sendData("\n" + result);
      else
        sendData("\nErrRead");               //At command ERROR CODE for Failed Executing statement
      sendToDue("Errore, non ricevuto OK, ricevuto messaggio: " + message, responseTime, DEBUG);
    } else {
      sendData("\nErrRead");                 //Command ERROR CODE for UNABLE TO READ
      sendToDue("Errore, message received: " + message, responseTime, DEBUG);
    }
  }
  if(detectMotion()){
    Serial.println("!!!Movement detected!!!");
    sendData("Movement detected!");    
    delay(2000);
  }
  delay(responseTime);
}


/*
  Name: sendData
  Description: Function used to send string to tcp client using cipsend
  Params:
  Returns: void
*/
void sendData(String str) {  
  String len = "";
  len += str.length();
  Serial.println("sendData str: "+str);
  sendToWifi("AT+CIPSTART=\"TCP\",\"192.168.43.1\",8080", responseTime, DEBUG); 
  sendToWifi("AT+CIPSEND=" + len, responseTime, DEBUG);
  sendToWifi(str, responseTime, DEBUG);
  sendToWifi("AT+CIPCLOSE", responseTime, DEBUG);
}


/*
  Name: find
  Description: Function used to match two string
  Params:
  Returns: true if match else false
*/
boolean find(String string, String value) {
  return string.indexOf(value) >= 0;
}


/*
  Name: readSerialMessage
  Description: Function used to read data from Arduino Serial.
  Params:
  Returns: The response from the Arduino (if there is a reponse)
*/
String  readSerialMessage() {
  char value[100];
  int index_count = 0;
  while (Serial.available() > 0) {
    value[index_count] = Serial.read();
    index_count++;
    value[index_count] = '\0'; // Null terminate the string
  }
  String str(value);
  str.trim();
  return str;
}



/*
  Name: readWifiSerialMessage
  Description: Function used to read data from ESP8266 Serial.
  Params:
  Returns: The response from the esp8266 (if there is a reponse)
*/
String  readWifiSerialMessage() {
  char value[100];
  int index_count = 0;
  while (Serial1.available() > 0) {
    value[index_count] = Serial1.read();
    index_count++;
    value[index_count] = '\0'; // Null terminate the string
  }
  String str(value);
  str.trim();
  return str;
}



/*
  Name: sendToWifi
  Description: Function used to send data to ESP8266.
  Params: command - the data/command to send; timeout - the time to wait for a response; debug - print to Serial window?(true = yes, false = no)
  Returns: The response from the esp8266 (if there is a reponse)
*/
String sendToWifi(String command, const int timeout, boolean debug) {
  String response = "";
  Serial1.println(command); // send the read character to the esp8266
  long int time = millis();
  while ( (time + timeout) > millis())
  {
    while (Serial1.available())
    {
      // The esp has data so display its output to the serial window
      char c = Serial1.read(); // read the next character.
      response += c;
    }
  }
  if (debug)
  {
    Serial.println("Response from sendToWifi: "+ response);
  }
  return response;
}

/*
  Name: sendToDue
  Description: Function used to send data to Arduino.
  Params: command - the data/command to send; timeout - the time to wait for a response; debug - print to Serial window?(true = yes, false = no)
  Returns: The response from the esp8266 (if there is a reponse)
*/
String sendToDue(String command, const int timeout, boolean debug) {
  String response = "";
  Serial.println("Command sent to due: "+ command); // send the read character to the esp8266
  long int time = millis();
  while ( (time + timeout) > millis())
  {
    while (Serial.available())
    {
      // The esp has data so display its output to the serial window
      char c = Serial.read(); // read the next character.
      response += c;
    }
  }
  if (debug)
  {
    Serial.println(response);
  }
  return response;
}

boolean detectMotion() {
  float tempHum = dht.readHumidity();  // Get Humidity value
  if(!isnan(tempHum)){
    hum = tempHum;
  }
  float tempTemp= dht.readTemperature();  // Get Temperature value
  if(!isnan(tempTemp)){
    temp = tempTemp;
  }
    
  // Calculate the Speed of Sound in M/S
  soundsp = 331.4 + (0.606 * temp) + (0.0124 * hum);
    
  // Convert to cm/ms  
  soundcm = soundsp / 10000;
    
  //duration = sonar.ping_median(iterations);
  duration = computeDuration(1);
  //Serial.println("Duration: "+duration);
  
  // Calculate the distance
  distance = (duration / 2) * soundcm;

  if (distance < 20) {
    return true;
  } else {
    return false;
  }
}

long computeDuration(int iterations){
  long totalDuration = 0;
  for(int i = 0; i < iterations; i++){
    // Clears the TRIGPIN
    digitalWrite(TRIGPIN, LOW);
    delayMicroseconds(2);
    // Sets the TRIGPIN on HIGH state for 10 micro seconds
    digitalWrite(TRIGPIN, HIGH);
    delayMicroseconds(10);
    digitalWrite(TRIGPIN, LOW);
    // Reads the ECHOPIN, returns the sound wave travel time in microseconds
    totalDuration += pulseIn(ECHOPIN, HIGH);
  }
  long medianDuration = totalDuration/iterations;
  return medianDuration;
}
