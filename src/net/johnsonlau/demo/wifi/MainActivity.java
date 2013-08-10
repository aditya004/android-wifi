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
	private Button mListConfiguredWifi;
	private Button mStartBoardingDevice;
	private Button mOnBoardDevice;
	private TextView mInfo;
	private TextView mSsid;
	private EditText mPwd;
	private LinearLayout mBoardLayout;
	private Spinner mTargetDeviceSpinner;

	private WifiManager mWifiManager;
	private String mPreviousSsid = "";

	private static final short MSG_SCAN_WIFI = 1;
	private static final short MSG_SHOW_WIFI_SCAN_RESULT = 2;
	private static final short MSG_START_UP = 3;
	private static final short MSG_ENABLE_BUTTONS = 4;
	private static final short MSG_ON_BOARD_DEVICE = 5;
	private static final short MSG_ON_BOARDING_SUCCESS = 7;

	private static final String DEVICE_AP_PREFIX = "WiFly";
	private static final String DEVICE_IP = "1.2.3.4";
	private static final int DEVICE_TCP_PORT = 2000;

	private Handler mBackendHandler;
	private Handler mHandler = new Handler() 
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
						if(item.SSID.indexOf(DEVICE_AP_PREFIX) != -1)
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
						mSsid.setText(ssid);

						ArrayAdapter adapter = new ArrayAdapter(MainActivity.this, R.layout.target_device, devices);
						mTargetDeviceSpinner.setAdapter(adapter);
					}
				   	mScanWifi.setText("SCAN WIFI"); 
					mScanWifi.setEnabled(true); 
					mStartBoardingDevice.setEnabled(true); 
					mOnBoardDevice.setEnabled(true);
					break;

				case MSG_ENABLE_BUTTONS:
					mScanWifi.setEnabled(true);
					mListConfiguredWifi.setEnabled(true);
					mOnBoardDevice.setEnabled(true);
					mStartBoardingDevice.setEnabled(true);
					break;

				case MSG_ON_BOARDING_SUCCESS:
					String text = "finish onboarding, please give a try.";  
					Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();  
					mOnBoardDevice.setEnabled(true);

					// turn the wifi back
					joinDeviceAp(mPreviousSsid);
					break;

				default:
					break;
			}
		}
	};


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
		mOnBoardDevice.setEnabled(false);
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
				mSsid.setText(ssid);
			}			  
		});  

		mWifiManager=(WifiManager)getSystemService(Context.WIFI_SERVICE);

		mResetView = (Button) findViewById(R.id.reset_view);
		mScanWifi = (Button) findViewById(R.id.scan_wifi);
		mListConfiguredWifi = (Button) findViewById(R.id.list_configured_wifi);
		mOnBoardDevice = (Button) findViewById(R.id.on_board_device);
		mStartBoardingDevice = (Button) findViewById(R.id.start_boarding_device);
		mInfo = (TextView) findViewById(R.id.info);
		mSsid = (TextView) findViewById(R.id.ssid);
		mPwd = (EditText) findViewById(R.id.pwd);
		mBoardLayout = (LinearLayout) findViewById(R.id.board_layout);
		mTargetDeviceSpinner = (Spinner) findViewById(R.id.target_device_spinner);

		mResetView.setOnClickListener(new MyListener());
		mScanWifi.setOnClickListener(new MyListener());
		mListConfiguredWifi.setOnClickListener(new MyListener());
		mOnBoardDevice.setOnClickListener(new MyListener());
		mStartBoardingDevice.setOnClickListener(new MyListener());

		mBoardLayout.setVisibility(View.GONE);

		HandlerThread backendThread = new HandlerThread("BackendHandler");
		backendThread.start();
		mBackendHandler = new Handler(backendThread.getLooper())
		{
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
						Message enableButtonsMsg = mHandler.obtainMessage(MSG_ENABLE_BUTTONS);
						mHandler.sendMessage(enableButtonsMsg);
						break;

					case MSG_SCAN_WIFI:
						mWifiManager.startScan();
						sleep(3000);
						Message showWifiScanResultMsg = mHandler.obtainMessage(MSG_SHOW_WIFI_SCAN_RESULT, msg.obj);
						mHandler.sendMessage(showWifiScanResultMsg);
						break;

					case MSG_ON_BOARD_DEVICE:
						saveInfoToDevice((String)msg.obj);

						Message onBoardingSuccessMsg = mHandler.obtainMessage(MSG_ON_BOARDING_SUCCESS);
						mHandler.sendMessage(onBoardingSuccessMsg);
						break;

					default:
						break;
				}
			}
		};
	}

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

				case R.id.on_board_device:
					onBoardDevice();
					break;

				case R.id.reset_view:
					resetView();
					break;

				default:
					break;
			}
		}
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
		// emptyp the target device spinner
		List<String> devices = new ArrayList<String>();
		ArrayAdapter adapter = new ArrayAdapter(MainActivity.this, R.layout.target_device, devices);
		mTargetDeviceSpinner.setAdapter(adapter);

		mSsid.setText("");
		mOnBoardDevice.setEnabled(false);
		mStartBoardingDevice.setEnabled(false);
		mInfo.setVisibility(View.GONE);

		startScanWifi("onboarding");
	}

	private void onBoardDevice()
	{
		mOnBoardDevice.setEnabled(false);

		String targetDeviceSsid = mTargetDeviceSpinner.getSelectedItem().toString();
		String ssid = mSsid.getText().toString();
		String password = mPwd.getText().toString();

		WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
		if(wifiInfo != null)
		{
			mPreviousSsid = wifiInfo.getSSID();
		}

		addDeviceAp(targetDeviceSsid);
		joinDeviceAp(targetDeviceSsid);

		// write the ssid and password to device
		String obj = "*BOARDING*\n" + ssid + "\n" + password;
		Message msg = mBackendHandler.obtainMessage(MSG_ON_BOARD_DEVICE, obj);
		mBackendHandler.sendMessage(msg);
	}

	private void resetView()
	{
		mInfo.setVisibility(View.GONE);
		getListView().setVisibility(View.GONE);  
		mBoardLayout.setVisibility(View.GONE);
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
			mBoardLayout.setVisibility(View.GONE);
		}
		else
		{
			mBoardLayout.setVisibility(View.VISIBLE);
		}

		mScanWifi.setEnabled(false);
		String infoText = "scanning...";
		mScanWifi.setText(infoText);
		//Toast.makeText(MainActivity.this, infoText, android.widget.Toast.LENGTH_LONG).show();
	}

	private void saveInfoToDevice(String msg)
	{
		sleep(10000); // waiting for the wifi ready
		Utils.PrintLog("writing(" + DEVICE_IP + ":" + DEVICE_TCP_PORT + "): " + msg);
		try
		{
			Socket socket = new Socket(DEVICE_IP, DEVICE_TCP_PORT);  
			try
			{
				PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);  
				out.println(msg);  
				out.flush();  
			}
			catch (Exception ex1) 
			{  
				Utils.PrintLog("ex1: " + ex1.toString());
			} 
			finally 
			{ 
				socket.close();  
			}  
		}
		catch (Exception ex2) 
		{  
			Utils.PrintLog("ex2: " + ex2.toString());
		} 
	}
}
