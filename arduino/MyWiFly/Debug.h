
#ifndef __DEBUG_H__
#define __DEBUG_H__

#define DEBUG

#ifdef DEBUG
#define dbg(message)    Serial.print(message)
#define dbgln(message)    Serial.println(message)
#else
#define dbg(message)
#define dbgln(message)
#endif // DEBUG

#endif // __DEBUG_H__
