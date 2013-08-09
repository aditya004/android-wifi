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
import android.app.ListActivity;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView;
import android.widget.SimpleAdapter;
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
	private Button mScanWifiBtn;

	private WifiManager mWifiManager;

	private static final short MSG_SCAN_WIFI = 1;
	private static final short MSG_SHOW_WIFI_SCAN_RESULT = 2;

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
					Collections.sort(results, new WifiSingleLevelComparator());

					List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
					for(int i=0; i < results.size(); i++)
					{ 
						ScanResult item = results.get(i);
						
						Map<String, Object> map = new HashMap<String, Object>();  
						map.put("img_pre", R.drawable.processor);           
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
					
					mScanWifiBtn.setText("SCAN WIFI");
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

	private void init()
	{
		ListView lv = getListView();  
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() 
		{  
            @Override  
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) 
			{  
                Map<String, Object> item = (Map<String, Object>)parent.getItemAtPosition(pos);  
                Toast.makeText(getApplicationContext(), (String)item.get("text"), Toast.LENGTH_SHORT).show();  
            }             
        });  

		mWifiManager=(WifiManager)getSystemService(Context.WIFI_SERVICE);
		mScanWifiBtn = (Button) findViewById(R.id.scan_wifi);

		mScanWifiBtn.setOnClickListener(new MyListener());

		HandlerThread backendThread = new HandlerThread("BackendHandler");
        backendThread.start();
        mBackendHandler = new Handler(backendThread.getLooper())
		{
			@Override
			public void handleMessage(Message msg) 
			{
				switch (msg.what) 
				{
					case MSG_SCAN_WIFI:
						mWifiManager.startScan();
						sleep(3000);
						Message msg2 = mHandler.obtainMessage(MSG_SHOW_WIFI_SCAN_RESULT);
						mHandler.sendMessage(msg2);
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
					Message msg = mBackendHandler.obtainMessage(MSG_SCAN_WIFI);
					mBackendHandler.sendMessage(msg);

					String infoText = "scanning...";
					mScanWifiBtn.setText(infoText);
					//Toast.makeText(MainActivity.this,infoText, android.widget.Toast.LENGTH_LONG).show();
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

	public class WifiSingleLevelComparator implements Comparator
	{
		@Override
		public int compare(Object arg1, Object arg2) 
		{
		   ScanResult item1=(ScanResult)arg1;
		   ScanResult item2=(ScanResult)arg2;

		   return item2.level - item1.level;
		}
	}
}
