package net.johnsonlau.demo.wifi;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import android.view.View.OnClickListener;
import android.view.View;

public class MainActivity extends Activity
{
	private Button mCheckBtn;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
		init();
    }

	private void init()
	{
		mCheckBtn = (Button) findViewById(R.id.check);
		mCheckBtn.setOnClickListener(new MyListener());
	}

	private class MyListener implements OnClickListener 
	{
		@Override
		public void onClick(View v)
		{
			switch(v.getId())
			{
				case R.id.check:
					Toast.makeText(MainActivity.this, "hello", android.widget.Toast.LENGTH_LONG).show();
					break;
				default:
					break;
			}
		}
	}
}
