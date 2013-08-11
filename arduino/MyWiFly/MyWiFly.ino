
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
		dbg(recvBuffer[recvBufferLen - 1]);
		if(recvBufferLen >= RECV_BUFFER_LEN)
		{
			recvBufferLen = 0;
		}
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
			char ssid[ssidLen + 1];
			char pwd[pwdLen + 1];

			for(i = prefixLen; i < prefixLen + ssidLen; i++)
			{
				ssid[i - prefixLen] = recvBuffer[i];
			}
			ssid[i - prefixLen] = '\0';

			for(i = prefixLen + ssidLen; i < prefixLen + ssidLen + pwdLen; i++)
			{
				pwd[i - prefixLen - ssidLen] = recvBuffer[i];
			}
			pwd[i - prefixLen - ssidLen] = '\0';

			dbgln("\n--------- got conf ---------");
			dbg("ssid: ");
			dbgln(ssid);
			dbg("pwd: ");
			dbgln(pwd);

			onBoarding(ssid, pwd);

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

// ==================== Logic Methods =====================
void onBoarding(char ssid[], char pwd[])
{
	dbgln("--------- saving conf ---------");
	wifly.write("$$$");
	waitAndPrintUart();

	wifly.write("set wlan ssid ");
	wifly.write(ssid);
	wifly.write('\r');
	waitAndPrintUart();

	wifly.write("set wlan pass ");
	wifly.write(pwd);
	wifly.write('\r');
	waitAndPrintUart();

	wifly.write("set wlan join 1\r");
	waitAndPrintUart();

	wifly.write("set ip dhcp 1\r");
	waitAndPrintUart();

	wifly.write("save\r");
	waitAndPrintUart();

	wifly.write("reboot\r");
	waitAndPrintUart();

	dbgln("--------- saved ---------");
}

// ==================== Helper Methods =====================
void waitAndPrintUart()
{
	waitAndPrintUart(1000);
}

void waitAndPrintUart(short waitMilliSeconds)
{
	delay(waitMilliSeconds);
	while(wifly.available())
	{
		dbg(char(wifly.read()));
	}
	dbgln("");
}
