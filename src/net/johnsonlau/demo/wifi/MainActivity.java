package net.johnsonlau.demo.wifi;

import java.lang.Object;
import java.lang.Thread;
import java.lang.InterruptedException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.Comparator;
import java.net.Socket;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import android.app.ListActivity;
import android.widget.Button;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView;
import android.widget.SimpleAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.view.View.OnClickListener;
import android.view.View;
import android.content.Context;  
import android.net.wifi.ScanResult;  
import android.net.wifi.WifiConfiguration;	
import android.net.wifi.WifiInfo;  
import android.net.wifi.WifiManager;  
import android.net.wifi.WifiManager.WifiLock;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

public class MainActivity extends ListActivity
{
	private Button mResetView;
	private Button mScanWifi;
	private Button mScanDevice;
	private Button mListConfiguredWifi;
	private Button mStartBoardingDevice;
	private Button mSaveOnBoardingInfo;
	private Button mDeviceOn;
	private Button mDeviceOff;
	private TextView mInfo;
	private TextView mDeviceName;
	private TextView mOnBoardingSsid;
	private EditText mOnBoardingPwd;
	private LinearLayout mOnBoardingLayout;
	private LinearLayout mDeviceLayout;
	private Spinner mTargetDeviceSpinner;

	private WifiManager mWifiManager;
	private String mPreviousSsid = "";

	private static final short MSG_SCAN_WIFI = 1;
	private static final short MSG_SHOW_WIFI_SCAN_RESULT = 2;
	private static final short MSG_START_UP = 3;
	private static final short MSG_START_UP_DONE = 4;
	private static final short MSG_ON_BOARD_DEVICE = 5;
	private static final short MSG_ON_BOARDING_SUCCESS = 6;
	private static final short MSG_ON_BOARDING_FAIL = 7;
	private static final short MSG_START_DISCOVERY_DEVICE = 8;
	private static final short MSG_DEVICE_DISCOVERYED = 9;
	private static final short MSG_NO_DEVICE_DISCOVERYED = 10;
	private static final short MSG_DEVICE_ON = 11;
	private static final short MSG_DEVICE_OFF = 12;
	private static final short MSG_SEND_CMD_TO_DEVICE = 13;

	private static final String[] DEVICE_AP_PREFIXS = {"WiFly", "roving"};
	private static final String DEVICE_AP_IP = "1.2.3.4";
	private static final int DEVICE_AP_TCP_PORT = 2000;
	private static final int DEVICE_TCP_PORT = 2000;
	private static final int DISCOVERY_UDP_PORT = 55555;
	private static final int DISCOVERY_TIMEOUT = 15000; // milliseconds
	private static final int WIFI_SCAN_WAITING_TIME = 3000; // milliseconds
	private static final int SAVE_ON_BOARDING_INTO_TO_DEVICE_RETRY_INTERVAL = 1500; // milliseconds
	private static final int SAVE_ON_BOARDING_INTO_TO_DEVICE_RETRY_TIMES = 15;

	private int mSaveOnBoardingInfoToDeviceTryTimes = 0; 

	private Handler mBackendHandler;
	private Handler mCommHandler;
	private Handler mMainHandler;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		init();
	}

	@Override
	public void onResume()
	{
		super.onResume();

		if(!mWifiManager.isWifiEnabled())
		{
			mWifiManager.setWifiEnabled(true);
		}

		mScanWifi.setEnabled(false);
		mListConfiguredWifi.setEnabled(false);
		mSaveOnBoardingInfo.setEnabled(false);
		mStartBoardingDevice.setEnabled(false);

		Message msg = mBackendHandler.obtainMessage(MSG_START_UP);
		mBackendHandler.sendMessage(msg);
	}

	private void init()
	{
		ListView lv = getListView();  
		lv.setOnItemClickListener(new AdapterView.OnItemClickListener() 
		{  
			@Override  
			public void onItemClick(AdapterView<?> parent, View view, int pos, long id) 
			{  
				Map<String, Object> item = (Map<String, Object>)parent.getItemAtPosition(pos);	
				String ssid = (String)item.get("text");

				//Toast.makeText(getApplicationContext(), ssid, Toast.LENGTH_SHORT).show();  
				mOnBoardingSsid.setText(ssid);
			}			  
		});  

		mWifiManager=(WifiManager)getSystemService(Context.WIFI_SERVICE);

		mResetView = (Button) findViewById(R.id.reset_view);
		mScanWifi = (Button) findViewById(R.id.scan_wifi);
		mScanDevice = (Button) findViewById(R.id.scan_device);
		mListConfiguredWifi = (Button) findViewById(R.id.list_configured_wifi);
		mSaveOnBoardingInfo = (Button) findViewById(R.id.save_on_boarding_info);
		mStartBoardingDevice = (Button) findViewById(R.id.start_boarding_device);
		mDeviceOn = (Button) findViewById(R.id.device_on);
		mDeviceOff = (Button) findViewById(R.id.device_off);
		mInfo = (TextView) findViewById(R.id.info);
		mDeviceName = (TextView) findViewById(R.id.device_name);
		mOnBoardingSsid = (TextView) findViewById(R.id.on_boarding_ssid);
		mOnBoardingPwd = (EditText) findViewById(R.id.on_boarding_pwd);
		mOnBoardingLayout = (LinearLayout) findViewById(R.id.on_boarding_layout);
		mDeviceLayout = (LinearLayout) findViewById(R.id.device_layout);
		mTargetDeviceSpinner = (Spinner) findViewById(R.id.target_device_spinner);

		mResetView.setOnClickListener(new MyListener());
		mScanWifi.setOnClickListener(new MyListener());
		mScanDevice.setOnClickListener(new MyListener());
		mListConfiguredWifi.setOnClickListener(new MyListener());
		mSaveOnBoardingInfo.setOnClickListener(new MyListener());
		mStartBoardingDevice.setOnClickListener(new MyListener());
		mDeviceOn.setOnClickListener(new MyListener());
		mDeviceOff.setOnClickListener(new MyListener());

		mOnBoardingLayout.setVisibility(View.GONE);

		mMainHandler = new MainHandler();

		HandlerThread backendThread = new HandlerThread("BackendHandler");
		backendThread.start();
		mBackendHandler = new BackendHandler(backendThread.getLooper());

		HandlerThread discoveryThread = new HandlerThread("CommHandler");
		discoveryThread.start();
		mCommHandler = new CommHandler(discoveryThread.getLooper());
	}

	private void sleep(int milliseconds)
	{
		try
		{
			Thread.sleep(milliseconds);
		}
		catch (InterruptedException ex)
		{
		}
	}

	private class WifiSignalLevelComparator implements Comparator
	{
		@Override
		public int compare(Object arg1, Object arg2) 
		{
		   ScanResult item1=(ScanResult)arg1;
		   ScanResult item2=(ScanResult)arg2;

		   return WifiManager.compareSignalLevel(item2.level, item1.level);
		}
	}

	private int getConfiguredApNetworkId(String ssid)
	{
		int targetNetworkId = -1;
		List<WifiConfiguration> configuredWifis = mWifiManager.getConfiguredNetworks();
		for(WifiConfiguration item : configuredWifis)
		{
			if(item.SSID.indexOf(ssid) != -1)
			{
				targetNetworkId = item.networkId;
				break;
			}
		}
		return targetNetworkId;
	}

	private void joinDeviceAp(String ssid)
	{
		int networkId = getConfiguredApNetworkId(ssid);
		if(networkId != -1)
		{
			joinDeviceAp(networkId);
		}
	}

	private void joinDeviceAp(int networkId)
	{
		mWifiManager.disconnect();
		mWifiManager.enableNetwork(networkId, true);
		mWifiManager.reconnect();
	}

	private void addDeviceAp(String ssid)
	{
		WifiConfiguration conf = new WifiConfiguration();
		conf.SSID = "\"" + ssid + "\""; 

		// for open AP
		conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);

		// for WPA/WPA2 AP
		//String networkPass = "pass";
		//conf.preSharedKey = "\""+ networkPass +"\"";

		mWifiManager.addNetwork(conf);
	}

	private void startBoardingDevice()
	{
		initOnBoardingPanel();
		startScanWifi("onboarding");
	}

	private void initOnBoardingPanel()
	{
		// emptyp the target device spinner
		List<String> devices = new ArrayList<String>();
		ArrayAdapter adapter = new ArrayAdapter(MainActivity.this, R.layout.target_device, devices);
		mTargetDeviceSpinner.setAdapter(adapter);

		mOnBoardingSsid.setText("");
		mStartBoardingDevice.setEnabled(false);
		mSaveOnBoardingInfo.setEnabled(false);
		mInfo.setVisibility(View.GONE);
	}

	private void saveOnBoardingInfo()
	{
		String targetDeviceSsid = mTargetDeviceSpinner.getSelectedItem() != null ? mTargetDeviceSpinner.getSelectedItem().toString() : "";
		String ssid = mOnBoardingSsid.getText().toString();
		String pwd = mOnBoardingPwd.getText().toString();

		if(targetDeviceSsid.length() == 0 || ssid.length() == 0)
		{
			String text = "no device or AP was detected.";
			Toast.makeText(MainActivity.this, text, Toast.LENGTH_LONG).show();
		}
		else
		{
			mSaveOnBoardingInfo.setText("saving...");
			mSaveOnBoardingInfo.setEnabled(false);

			WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
			if(wifiInfo != null)
			{
				mPreviousSsid = wifiInfo.getSSID();
			}

			addDeviceAp(targetDeviceSsid);
			joinDeviceAp(targetDeviceSsid);

			// save the ssid and pwd to device
			mSaveOnBoardingInfoToDeviceTryTimes = 0;
			String obj = ssid + pwd;
			Message msg = mBackendHandler.obtainMessage(MSG_ON_BOARD_DEVICE, obj);
			msg.arg1 = ssid.length();
			msg.arg2 = pwd.length();
			mBackendHandler.sendMessage(msg);
		}
	}
	
	private void scanDevice()
	{
		mScanDevice.setText("scanning device...");
		mScanDevice.setEnabled(false);
		mDeviceLayout.setVisibility(View.GONE);

		Message msg = mCommHandler.obtainMessage(MSG_START_DISCOVERY_DEVICE);
		mCommHandler.sendMessage(msg);
	}
	
	private void resetView()
	{
		mInfo.setVisibility(View.GONE);
		getListView().setVisibility(View.GONE);  
		mOnBoardingLayout.setVisibility(View.GONE);
	}
	
	private void listConfiguredWifis()
	{
		List<WifiConfiguration> configuredWifis = mWifiManager.getConfiguredNetworks();
		StringBuffer sb=new StringBuffer();  
		for(WifiConfiguration item : configuredWifis)
		{
			sb=sb.append(new Integer(item.networkId).toString()).append(" ").append(item.SSID + "\n");
		}
		mInfo.setText(sb.toString());  
		mInfo.setVisibility(View.VISIBLE);
	}

	private void startScanWifi(String action)
	{
		Message msg = mBackendHandler.obtainMessage(MSG_SCAN_WIFI, action);
		mBackendHandler.sendMessage(msg);

		getListView().setVisibility(View.GONE);
		if(action == "scan")
		{
			mOnBoardingLayout.setVisibility(View.GONE);
		}
		else
		{
			mOnBoardingLayout.setVisibility(View.VISIBLE);
		}

		mScanWifi.setEnabled(false);
		String infoText = "scanning wifi...";
		mScanWifi.setText(infoText);
		//Toast.makeText(MainActivity.this, infoText, Toast.LENGTH_LONG).show();
	}

	private void saveOnBoardingInfoToDevice(Message msg)
	{
		sleep(SAVE_ON_BOARDING_INTO_TO_DEVICE_RETRY_INTERVAL); // waiting for the wifi ready
		Utils.PrintLog("writing to " + DEVICE_AP_IP + ":" + DEVICE_AP_TCP_PORT);
		Socket socket = null;
		try
		{
			String ssid = ((String)msg.obj).substring(0, msg.arg1);
			String pwd = ((String)msg.obj).substring(msg.arg1);

			socket = new Socket(DEVICE_AP_IP, DEVICE_AP_TCP_PORT);  
			PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);  
			out.print("*CONF*");  
			out.write(msg.arg1);  
			out.write(msg.arg2);  
			out.print(ssid);  
			out.print(pwd);  
			out.flush();  

			Message onBoardingSuccessMsg = mMainHandler.obtainMessage(MSG_ON_BOARDING_SUCCESS);
			mMainHandler.sendMessage(onBoardingSuccessMsg);
		}
		catch (Exception ex) 
		{   
			Utils.PrintLog("saveOnBoardingInfoToDevice Exception: " + ex.toString());
			retrySavingOnBoardingInfoToDevice(msg);
		} 
		finally 
		{ 
			if(socket != null)
			{
				try
				{
					socket.close();  
				}
				catch (Exception ex2)
				{
				}
			}
		}  
	}

	private void retrySavingOnBoardingInfoToDevice(Message msg)
	{
		mSaveOnBoardingInfoToDeviceTryTimes++;
		if(mSaveOnBoardingInfoToDeviceTryTimes < SAVE_ON_BOARDING_INTO_TO_DEVICE_RETRY_TIMES)
		{	   
			Message retryMsg = mBackendHandler.obtainMessage(msg.what, msg.obj);
			retryMsg.arg1 = msg.arg1;
			retryMsg.arg2 = msg.arg2;
			mBackendHandler.sendMessage(retryMsg);
		}
		else
		{
			Message onBoardingSuccessMsg = mMainHandler.obtainMessage(MSG_ON_BOARDING_FAIL);
			mMainHandler.sendMessage(onBoardingSuccessMsg);
		}
	}

	private boolean isDevicpAp(String ssid)
	{
		boolean result = false;
		
		for(int i = 0; i < DEVICE_AP_PREFIXS.length; i++)
		{
			if(ssid.indexOf(DEVICE_AP_PREFIXS[i]) != -1)
			{
				result = true;
				break;
			}
		}

		return result;
	}

	private void turnOnDevice()
	{
		Message msg = mCommHandler.obtainMessage(MSG_SEND_CMD_TO_DEVICE);
		msg.obj = "on";
		mCommHandler.sendMessage(msg);
	}

	private void turnOffDevice()
	{
		Message msg = mCommHandler.obtainMessage(MSG_SEND_CMD_TO_DEVICE);
		msg.obj = "off";
		mCommHandler.sendMessage(msg);
	}

	private void sendCmdToDevice(String cmd)
	{
		Socket socket = null;
		try
		{
			int cmdLen = cmd.length();
			String DeviceIp = mDeviceName.getText().toString();

			socket = new Socket(DeviceIp, DEVICE_TCP_PORT);  
			PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);  
			out.print("*CMD*");  
			out.write(cmdLen);  
			out.print(cmd);
			out.flush();  
		}
		catch (Exception ex) 
		{   
			Utils.PrintLog("Send Cmd to Device Exception: " + ex.toString());
		} 
		finally 
		{ 
			if(socket != null)
			{
				try
				{
					socket.close();  
				}
				catch (Exception ex2)
				{
				}
			}
		} 
	}

	// == handlers & listener ==============================================================
	
	private class MyListener implements OnClickListener 
	{
		@Override
		public void onClick(View v)
		{
			switch(v.getId())
			{
				case R.id.scan_wifi:
					startScanWifi("scan");
					break;

				case R.id.list_configured_wifi:
					listConfiguredWifis();	
					break;

				case R.id.start_boarding_device:
					startBoardingDevice();
					break;

				case R.id.save_on_boarding_info:
					saveOnBoardingInfo();
					break;

				case R.id.reset_view:
					resetView();
					break;

				case R.id.scan_device:
					scanDevice();
					break;

				case R.id.device_on:
					turnOnDevice();
					break;

				case R.id.device_off:
					turnOffDevice();
					break;

				default:
					break;
			}
		}
	}
	private class BackendHandler extends Handler
	{
		public BackendHandler(Looper looper)
		{
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) 
		{
			switch (msg.what) 
			{
				case MSG_START_UP:
					while(!mWifiManager.isWifiEnabled())
					{
						sleep(200);
					}
					Message enableButtonsMsg = mMainHandler.obtainMessage(MSG_START_UP_DONE);
					mMainHandler.sendMessage(enableButtonsMsg);
					break;

				case MSG_SCAN_WIFI:
					mWifiManager.startScan();
					sleep(WIFI_SCAN_WAITING_TIME);
					Message showWifiScanResultMsg = mMainHandler.obtainMessage(MSG_SHOW_WIFI_SCAN_RESULT, msg.obj);
					mMainHandler.sendMessage(showWifiScanResultMsg);
					break;

				case MSG_ON_BOARD_DEVICE:
					saveOnBoardingInfoToDevice(msg);
					break;

				default:
					break;
			}
		}
	}

	private class CommHandler extends Handler
	{
		public CommHandler(Looper looper)
		{
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) 
		{
			switch (msg.what) 
			{
				case MSG_START_DISCOVERY_DEVICE:
					byte[] buffer = new byte[512];
					DatagramSocket datagramSocket = null;
					try
					{
						datagramSocket = new DatagramSocket(DISCOVERY_UDP_PORT);
						DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);
						datagramSocket.setSoTimeout(DISCOVERY_TIMEOUT);
						datagramSocket.receive(datagramPacket);
						
						// "WiFly-EZX"
						if(datagramPacket.getLength() >= 110 &&
							buffer[60] == 0x57 &&
							buffer[61] == 0x69 &&
							buffer[62] == 0x46 &&
							buffer[63] == 0x6C &&
							buffer[64] == 0x79 &&
							buffer[65] == 0x2D &&
							buffer[66] == 0x45 &&
							buffer[67] == 0x5A &&
							buffer[68] == 0x58)
						{
							String deviceIp = datagramPacket.getAddress().getHostAddress().toString();
							Message discoveryedMsg = mMainHandler.obtainMessage(MSG_DEVICE_DISCOVERYED);
							discoveryedMsg.obj = deviceIp;
							mMainHandler.sendMessage(discoveryedMsg);
						}
					}
					catch(Exception ex)
					{
						Message noDeviceDiscoveryedMsg = mMainHandler.obtainMessage(MSG_NO_DEVICE_DISCOVERYED);
						mMainHandler.sendMessage(noDeviceDiscoveryedMsg);

						Utils.PrintLog("Discovery Device Exception: " + ex.toString());
					}
					finally
					{
						if(datagramSocket != null)
						{
							datagramSocket.close();
						}
					}
					break;

				case MSG_SEND_CMD_TO_DEVICE:
					String cmd = (String)msg.obj;
					sendCmdToDevice(cmd);
					break;

				default:
					break;
			}
		}
	}

	private class MainHandler extends Handler
	{ 
		@Override
		public void handleMessage(Message msg) 
		{
			switch (msg.what) 
			{
				case MSG_SHOW_WIFI_SCAN_RESULT:
					List<ScanResult> results = mWifiManager.getScanResults();
					Collections.sort(results, new WifiSignalLevelComparator());

					List<String> devices = new ArrayList<String>();
					String ssid = "";

					List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
					for(ScanResult item : results)
					{ 
						if(isDevicpAp(item.SSID))
						{
							devices.add(item.SSID);
							
							if(((String)msg.obj) == "onboarding")
							{
								continue;
							}
						}
						else if(ssid == "")
						{
							ssid = item.SSID;
						}

						Map<String, Object> map = new HashMap<String, Object>();  
						map.put("img_pre", R.drawable.wifi);		   
						map.put("text", item.SSID);  
						int imgId = 0;
						switch(WifiManager.calculateSignalLevel(item.level, 4))
						{
							case 0:
								imgId = R.drawable.wifi0;
								break;
							case 1:
								imgId = R.drawable.wifi1;
								break;
							case 2:
								imgId = R.drawable.wifi2;
								break;
							case 3:
								imgId = R.drawable.wifi3;
								break;
							default:
								break;
						}
						map.put("img", imgId);			 
						list.add(map);	
					} 

					setListAdapter(new SimpleAdapter(MainActivity.this, list, R.layout.wifi_item,	
										new String[]{"img_pre", "text", "img"},   
										new int[]{R.id.img_pre, R.id.text, R.id.img}));  
					getListView().setVisibility(View.VISIBLE);  
					
					if(results.size() > 1)// if there is only one, it is the devcie softAP
					{
						mOnBoardingSsid.setText(ssid);

						ArrayAdapter adapter = new ArrayAdapter(MainActivity.this, R.layout.target_device, devices);
						mTargetDeviceSpinner.setAdapter(adapter);
					}
				   	mScanWifi.setText("Scan"); 
					mScanWifi.setEnabled(true); 
					mStartBoardingDevice.setEnabled(true); 
					mSaveOnBoardingInfo.setEnabled(true);
					break;

				case MSG_START_UP_DONE:
					mScanWifi.setEnabled(true);
					mListConfiguredWifi.setEnabled(true);
					mSaveOnBoardingInfo.setEnabled(true);
					mStartBoardingDevice.setEnabled(true);

					scanDevice();
					break;

				case MSG_ON_BOARDING_SUCCESS:
					String successText = "success!";  
					Toast.makeText(getApplicationContext(), successText, Toast.LENGTH_LONG).show();  
					mSaveOnBoardingInfo.setText("Save");
					mSaveOnBoardingInfo.setEnabled(true);
					mOnBoardingLayout.setVisibility(View.GONE);
					getListView().setVisibility(View.GONE);

					// turn the wifi back
					joinDeviceAp(mPreviousSsid);

					scanDevice();
					break;

				case MSG_ON_BOARDING_FAIL:
					String failText = "onboarding failed, please retry.";  
					Toast.makeText(getApplicationContext(), failText, Toast.LENGTH_LONG).show();  
					mSaveOnBoardingInfo.setText("Save");
					mSaveOnBoardingInfo.setEnabled(true);

					// turn the wifi back
					joinDeviceAp(mPreviousSsid);
					break;

				case MSG_DEVICE_DISCOVERYED:
					String deviceIp = (String)msg.obj;
					String deviceDiscoveryedText = "detected " + deviceIp;
					Toast.makeText(getApplicationContext(), deviceDiscoveryedText, Toast.LENGTH_LONG).show();  

					mScanDevice.setText("ScanDevice");
					mScanDevice.setEnabled(true);
					mDeviceName.setText(deviceIp);
					mDeviceLayout.setVisibility(View.VISIBLE);
					break;

				case MSG_NO_DEVICE_DISCOVERYED:
					String noDiscoveryText = "no device was discoveryed.";
					Toast.makeText(getApplicationContext(), noDiscoveryText, Toast.LENGTH_LONG).show();  

					mScanDevice.setText("ScanDevice");
					mScanDevice.setEnabled(true);
					break;

				default:
					break;
			}
		}
	}
}
