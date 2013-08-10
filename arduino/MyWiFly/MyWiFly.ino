
#include <Arduino.h>
#include <SoftwareSerial.h>
#include "Debug.h"

#define rxPin 2
#define txPin 3

SoftwareSerial wifly = SoftwareSerial(rxPin, txPin);

void setup() 
{
	Serial.begin(9600);

	pinMode(rxPin, INPUT);
	pinMode(txPin, OUTPUT);
	wifly.begin(9600);

	delay(3000); // wait for WiFly init
	dbgln("--------- WiFly Started --------");
}

void loop() 
{
	while (wifly.available()) 
	{
		Serial.write(wifly.read());
	}

	while (Serial.available()) 
	{
		wifly.write(Serial.read());
	}
}
