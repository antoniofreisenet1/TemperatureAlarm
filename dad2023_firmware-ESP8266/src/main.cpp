#include <ESP8266HTTPClient.h>
#include "ArduinoJson.h"
#include <NTPClient.h>
#include <WiFiUdp.h>
#include <ESP8266WiFi.h>
#include <PubSubClient.h>
#include <DHT.h>
#include <DHT_U.h>

#define DHTTYPE DHT22




// Replace 0 by ID of this current device
const int DEVICE_ID = 124;
const int sensor_id = 72;
const int actuator_id = 3;
String mqttmsg;
int contador = 0;

// VARIABLES

const int sensorPin = 2; //esto es el pin D4
const int actuatorPin = 12; //esto es el pin D6


//DHT Sensor declaration
DHT dht(sensorPin, DHTTYPE);
int test_delay = 4000; // so we don't spam the API
boolean describe_tests = true;

// Replace 0.0.0.0 by your server local IP (ipconfig [windows] or ifconfig [Linux o MacOS] gets IP assigned to your PC)
String serverName = "http://192.168.43.195:8080/";
HTTPClient http;

// Replace WifiName and WifiPassword by your WiFi credentials
/* #define STASSID "Livebox6-4E4E"    //"Your_Wifi_SSID"
#define STAPSK "3bD6JnR6z9DC" //"Your_Wifi_PASSWORD" */


#define STASSID "AquarisV_ajm"    //"Your_Wifi_SSID"
#define STAPSK "ajmTest123" //"Your_Wifi_PASSWORD"


// NTP (Net time protocol) settings
WiFiUDP ntpUDP;
NTPClient timeClient(ntpUDP);

// MQTT configuration
WiFiClient client;
WiFiClient client2;
PubSubClient mqttClient(client2);

// Server IP, where de MQTT broker is deployed
const char *MQTT_BROKER_ADRESS = "192.168.43.195";
const uint16_t MQTT_PORT = 1883;

// Name for this MQTT client
const char *MQTT_CLIENT_NAME = "192.168.43.195";
const char *MQTT_CHANNEL = "mqttChannelDevice5";

// Pinout settings


// callback a ejecutar cuando se recibe un mensaje
// en este ejemplo, muestra por serial el mensaje recibido

void OnMqttReceived(char *topic, byte *payload, unsigned int length)
{
  Serial.print("Received on ");
  Serial.print(topic);
  Serial.print(": ");

  String content = "";
  for (size_t i = 0; i < length; i++)
  {
    content.concat((char)payload[i]);
  }
  Serial.print(content);
  Serial.println();
  mqttmsg=content;
}

// inicia la comunicacion MQTT
// inicia establece el servidor y el callback al recibir un mensaje
void InitMqtt()
{
  mqttClient.setServer(MQTT_BROKER_ADRESS, MQTT_PORT);
  mqttClient.setCallback(OnMqttReceived);
}

// Setup
void setup()
{
  Serial.begin(9600);
  //Serial.println();
  //Serial.print("Connecting to ");
  //Serial.println(STASSID);
  dht.begin();
  mqttmsg = "";
  /* Explicitly set the ESP32 to be a WiFi-client, otherwise, it by default,
     would try to act as both a client and an access-point and could cause
     network-issues with your other WiFi-devices on your WiFi-network. */
  WiFi.mode(WIFI_STA);
  WiFi.begin(STASSID, STAPSK);
  while (WiFi.status() != WL_CONNECTED)
  {
    delay(500);
    Serial.print(".");
  }
  InitMqtt();
  Serial.println("");
  Serial.println("WiFi connected");
  Serial.println("IP address: ");
  Serial.println(WiFi.localIP());
  Serial.println("Setup!");
  // Configure pin modes for actuators (output mode) and sensors (input mode). Pin numbers should be described by GPIO number (https://www.upesy.com/blogs/tutorials/esp32-pinout-reference-gpio-pins-ultimate-guide)
  // For ESP32 WROOM 32D https://uelectronics.com/producto/esp32-38-pines-esp-wroom-32/
  // You must find de pinout for your specific board version
  pinMode(actuatorPin, OUTPUT);
  //pinMode(sensorPin, INPUT);

  // Init and get the time
  timeClient.begin();
}

String response;

String serializeSensorValueBody(int idSensor, long timestamp, float value) //he quitado long timestamp
{
  // StaticJsonObject allocates memory on the stack, it can be
  // replaced by StaticJsonDocument<200> which allocates in the heap.
  //
  StaticJsonDocument<400> doc;

  // Add values in the document
  //
  doc["idSensor"] = idSensor;
  doc["timestamp"] = timestamp;
  doc["value"] = value;
  doc["removed"] = false;

  // Generate the minified JSON and send it to the Serial port.
  //
  String output;
  serializeJson(doc, output);
  Serial.println(output);

  return output;
}

String serializeActuatorStatusBody(float status, bool statusBinary, int idActuator, long timestamp)
{
  StaticJsonDocument<200> doc;

  doc["status"] = status;
  doc["statusBinary"] = statusBinary;
  doc["idActuator"] = idActuator;
  doc["timestamp"] = timestamp;
  doc["removed"] = false;

  String output;
  serializeJson(doc, output);
  return output;
}

String serializeDeviceBody(String deviceSerialId, String name, String mqttChannel, int idGroup)
{
  StaticJsonDocument<200> doc;

  doc["deviceSerialId"] = deviceSerialId;
  doc["name"] = name;
  doc["mqttChannel"] = mqttChannel;
  doc["idGroup"] = idGroup;

  String output;
  serializeJson(doc, output);
  return output;
}

void deserializeActuatorStatusBody(String responseJson)
{
  if (responseJson != "")
  {
    StaticJsonDocument<200> doc;

    // Deserialize the JSON document
    DeserializationError error = deserializeJson(doc, responseJson);

    // Test if parsing succeeds.
    if (error)
    {
      Serial.print(F("deserializeJson() failed: "));
      Serial.println(error.f_str());
      return;
    }

    // Fetch values.
    int idActuatorState = doc["idActuatorState"];
    float status = doc["status"];
    bool statusBinary = doc["statusBinary"];
    int idActuator = doc["idActuator"];
    long timestamp = doc["timestamp"];

    Serial.println(("Actuator status deserialized: [idActuatorState: " + String(idActuatorState) + ", status: " + String(status) + ", statusBinary: " + String(statusBinary) + ", idActuator" + String(idActuator) + ", timestamp: " + String(timestamp) + "]").c_str());
  }
}

void deserializeSensorValue(int httpResponseCode){
  if (httpResponseCode > 0)
  {

    Serial.printf("HTTP Response Code: %d", httpResponseCode);
    String responseJson = http.getString();
    StaticJsonDocument<400> doc;

    // Deserialize the JSON document
    DeserializationError error = deserializeJson(doc, responseJson);

    // Test if parsing succeeds.
    if (error)
    {
      Serial.println("deserializeJson() failed: ");
      Serial.println(error.f_str());
      return;
    }

    // Fetch values.
    int idSensorValue = doc["idSensorValue"];
    float value = doc["value"];
    int idSensor = doc["idSensor"];
    long timestamp = doc["timestamp"];
    boolean removed = doc["removed"];

    Serial.println(("Sensor value deserialized: [idSensorState: "
     + String(idSensorValue) + ", value: " + String(value) + ", idSensor: " + String(idSensor)
       + ", timestamp: " + String(timestamp) + ", removed" + String(removed) + "]").c_str());
  }

}

void deserializeDeviceBody(int httpResponseCode)
{

  if (httpResponseCode > 0)
  {
    Serial.print("HTTP Response code: ");
    Serial.println(httpResponseCode);
    String responseJson = http.getString();
    StaticJsonDocument<200> doc;

    DeserializationError error = deserializeJson(doc, responseJson);

    if (error)
    {
      Serial.print(F("deserializeJson() failed: "));
      Serial.println(error.f_str());
      return;
    }

    int idDevice = doc["idDevice"];
    String deviceSerialId = doc["deviceSerialId"];
    String name = doc["name"];
    String mqttChannel = doc["mqttChannel"];
    int idGroup = doc["idGroup"];

    Serial.println(("Device deserialized: [idDevice: " + String(idDevice) + ", name: "
     + name + ", deviceSerialId: " + deviceSerialId + ", mqttChannel" + mqttChannel + ", idGroup: " + idGroup + "]").c_str());
  }
  else
  {
    Serial.print("Error code: ");
    Serial.println(httpResponseCode);
  }
}

void deserializeSensorsFromDevice(int httpResponseCode)
{

  if (httpResponseCode > 0)
  {
    Serial.print("HTTP Response code: ");
    Serial.println(httpResponseCode);
    String responseJson = http.getString();
    // allocate the memory for the document
    StaticJsonDocument<200> doc;

    // parse a JSON array
    DeserializationError error = deserializeJson(doc, responseJson);

    if (error)
    {
      Serial.print(F("deserializeJson() failed: "));
      Serial.println(error.f_str());
      return;
    }

    // extract the values
    JsonArray array = doc.as<JsonArray>();
    for (JsonObject sensor : array)
    {
      int idSensor = sensor["idSensor"];
      String name = sensor["name"];
      String sensorType = sensor["sensorType"];
      int idDevice = sensor["idDevice"];

      Serial.println(("Sensor deserialized: [idSensor: " + String(idSensor) + ", name: " + name + ", sensorType: " + sensorType + ", idDevice: " + String(idDevice) + "]").c_str());
    }
  }
  else
  {
    Serial.print("Error code: ");
    Serial.println(httpResponseCode);
  }
}

void deserializeActuatorsFromDevice(int httpResponseCode)
{

  if (httpResponseCode > 0)
  {
    Serial.print("HTTP Response code: ");
    Serial.println(httpResponseCode);
    String responseJson = http.getString();
    // allocate the memory for the document
    StaticJsonDocument<200> doc;

    // parse a JSON array
    DeserializationError error = deserializeJson(doc, responseJson);

    if (error)
    {
      Serial.print(F("deserializeJson() failed: "));
      Serial.println(error.f_str());
      return;
    }

    // extract the values
    JsonArray array = doc.as<JsonArray>();
    for (JsonObject sensor : array)
    {
      int idActuator = sensor["idActuator"];
      String name = sensor["name"];
      String actuatorType = sensor["actuatorType"];
      int idDevice = sensor["idDevice"];

      Serial.println(("Actuator deserialized: [idActuator: " + String(idActuator) + ", name: " + name + ", actuatorType: " + actuatorType + ", idDevice: " + String(idDevice) + "]").c_str());
    }
  }
  else
  {
    Serial.print("Error code: ");
    Serial.println(httpResponseCode);
  }
}

void test_response(int httpResponseCode)
{
  //delay(test_delay);
  if (httpResponseCode > 0)
  {
    Serial.print("HTTP Response code: ");
    Serial.println(httpResponseCode);
    String payload = http.getString();
    Serial.println(payload);
  }
  else
  {
    Serial.print("Error code: ");
    Serial.println(httpResponseCode);
  }
}

void describe(char *description)
{
 // if (describe_tests)
    //Serial.println(description);
}

//PRUEBA DE GET: NO ES NECESARIO MANTENER EN LA IMPLEMENTACION FINAL
//Función: devuelve 5 dispositivos con id creciente.


void GET_tests()
{
  describe("Test GET full device info");
  String serverPath = serverName + "api/devices/" + String(DEVICE_ID);
  http.begin(client, serverPath.c_str());
  // test_response(http.GET());
  deserializeDeviceBody(http.GET());

  describe("Test GET sensors from deviceID");
  serverPath = serverName + "api/devices/" + String(DEVICE_ID) + "/sensors";
  http.begin(client, serverPath.c_str());
  deserializeSensorsFromDevice(http.GET());

  describe("Test GET actuators from deviceID");
  serverPath = serverName + "api/devices/" + String(DEVICE_ID) + "/actuators";
  http.begin(client, serverPath.c_str());
  deserializeActuatorsFromDevice(http.GET());

  describe("Test GET sensors from deviceID and Type");
  serverPath = serverName + "api/devices/" + String(DEVICE_ID) + "/sensors/Temperature";
  http.begin(client, serverPath.c_str());
  deserializeSensorsFromDevice(http.GET());

  describe("Test GET actuators from deviceID");
  serverPath = serverName + "api/devices/" + String(DEVICE_ID) + "/actuators/Relay";
  http.begin(client, serverPath.c_str());
  deserializeActuatorsFromDevice(http.GET());
}

//TEST DE UNA FUNCION DE PUT: ACTUALIZA EL VALOR DE UN DISPOSITIVO EN LA BBDD SEGÚN ID
//TODO: PASARLO A ECLIPSE Y QUITARLO DE VSC

void POST_tests()
{
  /* String actuator_states_body = serializeActuatorStatusBody(random(2000, 4000) / 100, true, 1, millis());
  describe("Test POST with actuator state");
  String serverPath = serverName + "api/actuator_states";
  http.begin(client, serverPath.c_str());
  test_response(http.POST(actuator_states_body)); */

  String sensor_value_body = serializeSensorValueBody(72, millis(), random(2000, 4000) / 100);
  describe("Test POST with sensor value");
  String serverPath = serverName + "api/sensor_values";
  http.begin(client, serverPath.c_str());
  test_response(http.POST(sensor_value_body));

  // String device_body = serializeDeviceBody(String(DEVICE_ID), ("Name_" + String(DEVICE_ID)).c_str(), ("mqtt_" + String(DEVICE_ID)).c_str(), 12);
  // describe("Test POST with path and body and response");
  // serverPath = serverName + "api/device";
  // http.begin(serverPath.c_str());
  // test_response(http.POST(actuator_states_body));
}

//POST sensorValues: recibe idSensor y valor para subirlo a la bbdd. 
void POST_null(){
  test_response(http.POST(""));
}
void POST_sv(float valor){
  String sensor_value_body2 = serializeSensorValueBody(sensor_id, millis(), valor);
  describe("POST SENSOR VALUES");
  String serverPath2 = serverName + "api/sensor_values";
  http.begin(client, serverPath2.c_str());
  test_response(http.POST(sensor_value_body2));
  //http.POST("{\'ejemplo\': \'hola\'}");
}

void POST_actuator(bool status){
  String sensor_value_body2 = serializeActuatorStatusBody(random(5,30), status, actuator_id, millis());
  describe("POST ACTUATOR STATUS");
  String serverPath2 = serverName + "api/actuator_states";
  http.begin(client, serverPath2.c_str());
  test_response(http.POST(sensor_value_body2));
  //http.POST("{\'ejemplo\': \'hola\'}");
}
// conecta o reconecta al MQTT
// consigue conectar -> suscribe a topic y publica un mensaje
// no -> espera 5 segundos
void ConnectMqtt()
{
  Serial.print("Starting MQTT connection...");
  if (mqttClient.connect(MQTT_CLIENT_NAME))
  {
    mqttClient.subscribe(MQTT_CHANNEL);
    //mqttClient.publish(MQTT_CHANNEL, "connected");
    Serial.println("Conectado!");
  }
  else
  {
    Serial.print("Failed MQTT connection, rc=");
    Serial.print(mqttClient.state());
    Serial.println(" try again in 5 seconds");

    delay(5000);
  }
}

// gestiona la comunicación MQTT
// comprueba que el cliente está conectado
// no -> intenta reconectar
// si -> llama al MQTT loop
void HandleMqtt()
{
  if (!mqttClient.connected())
  {
    ConnectMqtt(); 
  }
  mqttClient.loop();
}




// Run the tests!
void loop()
{
  // Update current time using NTP protocol
  timeClient.update();
  float valor = dht.readTemperature();
  //POST_tests();
  //GET_tests();
  //POST_null();
  if(contador == 10000){

  POST_sv(valor);
  
  // Reads analog sensor value and print it by serial monitor

  if (mqttmsg == "1")
    {
      digitalWrite(actuatorPin, HIGH);
      Serial.println("Digital sensor value : ON");
      POST_actuator(true);
    }
    else if (mqttmsg == "0")
    {
      digitalWrite(actuatorPin, LOW);
      Serial.println("Digital sensor value : OFF");
      POST_actuator(false);
    } else{
      Serial.println("NADA " + mqttmsg);
    }
    contador=0;
  }
  contador++;
  //UNA VEZ TENEMOS LOS DATOS, ¿LOS SUBIMOS A LA BASE DE DATOS? ¿ESPERAMOS A RECIBIR EL DATO DE LA BBDD, O ACTUAMOS Y LUEGO SUBIMOS?

  
  // Reads digital sensor value and print ON or OFF by serial monitor depending on the sensor status (binary)


    HandleMqtt();
  
}