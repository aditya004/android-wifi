
#include <Arduino.h>
#include <SoftwareSerial.h>
#include "Debug.h"

#define rxPin 2
#define txPin 3

#define RECV_TIMEOUT         1000 // milliseconds
#define RECV_BUFFER_LEN      200

char recvBuffer[RECV_BUFFER_LEN];
short recvBufferLen = 0;
unsigned long timer = 0;

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
		recvBuffer[recvBufferLen++] = wifly.read();
	}

	if (recvBufferLen > 0 && timer == 0)
	{
		timer = millis();
	}

	// CONF (set ssid & pwd)
	if (recvBufferLen >= 16)
	{
		short prefixLen = 14;	
		short msgLen = prefixLen + recvBuffer[12] + recvBuffer[13];

		if (recvBuffer[0] == '*' && 
		    recvBuffer[1] == 'O' && 
		    recvBuffer[2] == 'P' && 
		    recvBuffer[3] == 'E' && 
		    recvBuffer[4] == 'N' && 
		    recvBuffer[5] == '*' &&
		    recvBuffer[6] == '*' &&
		    recvBuffer[7] == 'C' && 
		    recvBuffer[8] == 'O' && 
		    recvBuffer[9] == 'N' && 
		    recvBuffer[10] == 'F' && 
		    recvBuffer[11] == '*' &&
		    recvBufferLen >= msgLen)
		{
			short i = 0;
			short ssidLen = recvBuffer[12];
			short pwdLen = recvBuffer[13];

			dbgln("--------- CONF ---------");
			dbg("ssid: ");
			for(i = prefixLen; i < prefixLen + ssidLen; i++)
			{
				dbg(recvBuffer[i]);
			}
			dbgln("");

			dbg("pwd: ");
			for(i = prefixLen + ssidLen; i < prefixLen + ssidLen + pwdLen; i++)
			{
				dbg(recvBuffer[i]);
			}
			dbgln("");

			recvBufferLen = 0;
			timer = 0;
		}
	}

	if (timer != 0 && millis() - timer > RECV_TIMEOUT)
	{
		recvBufferLen = 0;
		timer = 0;
	}

	while (Serial.available()) 
	{
		wifly.write(Serial.read());
	}
}
