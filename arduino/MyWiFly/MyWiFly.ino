
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
byte led = 13;

SoftwareSerial wifly = SoftwareSerial(rxPin, txPin);

void setup() 
{
	Serial.begin(9600);

	pinMode(rxPin, INPUT);
	pinMode(txPin, OUTPUT);
	wifly.begin(9600);

	pinMode(led, OUTPUT);

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

	consumeData();

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

void consumeData()
{
	consumeOnBoarding();
	consumeCmd();
}

void consumeOnBoarding()
{
	// CONF (set ssid & pwd)
	if (recvBufferLen >= 16)
	{
		byte prefixLen = 14;	
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
			byte ssidLen = recvBuffer[12];
			byte pwdLen = recvBuffer[13];
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
}

void consumeCmd()
{
	// CMD (control commands)
	if (recvBufferLen >= 13)
	{
		byte prefixLen = 12;	
		short msgLen = prefixLen + recvBuffer[11];

		if (recvBuffer[0] == '*' && 
		    recvBuffer[1] == 'O' && 
		    recvBuffer[2] == 'P' && 
		    recvBuffer[3] == 'E' && 
		    recvBuffer[4] == 'N' && 
		    recvBuffer[5] == '*' &&
		    recvBuffer[6] == '*' &&
		    recvBuffer[7] == 'C' && 
		    recvBuffer[8] == 'M' && 
		    recvBuffer[9] == 'D' && 
		    recvBuffer[10] == '*' &&
		    recvBufferLen >= msgLen)
		{
			short i = 0;
			byte cmdLen = recvBuffer[11];
			char cmd[cmdLen + 1];

			for(i = prefixLen; i < prefixLen + cmdLen; i++)
			{
				cmd[i - prefixLen] = recvBuffer[i];
			}
			cmd[i - prefixLen] = '\0';

			dbgln("\n--------- got cmd ---------");
			dbg("cmd: ");
			dbgln(cmd);

			runCmd(cmd, cmdLen);

			recvBufferLen = 0;
			timer = 0;
		}
	}
}

void runCmd(char cmd[], byte cmdLen)
{
	if(cmdLen == 2 &&
		cmd[0] == 'o' &&
		cmd[1] == 'n')
	{
		digitalWrite(led, HIGH);
	}
	else if(cmdLen == 3 &&
		cmd[0] == 'o' &&
		cmd[1] == 'f' &&
		cmd[2] == 'f')
	{
		digitalWrite(led, LOW);
	}
}

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
