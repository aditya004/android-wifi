package net.johnsonlau.demo.wifi;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import android.net.NetworkInfo;
import android.net.ConnectivityManager;
import android.content.Context;
import android.util.Log;

public class Utils {
	private static final String TAG = "demo";

	public static void PrintLog(String msg) 
	{
		DateFormat dateFormat = new SimpleDateFormat("(HH:mm:ss) ");
		String timeStr = dateFormat.format(Calendar.getInstance().getTime());
		msg = timeStr + msg;

		Log.i(Utils.TAG, msg);
	}
}
