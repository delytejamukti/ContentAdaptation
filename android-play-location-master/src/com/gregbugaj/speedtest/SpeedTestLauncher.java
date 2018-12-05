/*
	This file is part of SpeedTest.

    SpeedTest is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    SpeedTest is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with SpeedTest.  If not, see <http://www.gnu.org/licenses/>.

 */
package com.gregbugaj.speedtest;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

/**
 * Test speed of our network connection
 * @author Greg Bugaj http://www.gregbugaj.com
 * @version 1.0
 *
 */
public class SpeedTestLauncher extends Activity {

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mDecimalFormater=new DecimalFormat("##.##");
		//Request the progress bar to be shown in the title
		requestWindowFeature(Window.FEATURE_PROGRESS);
		setContentView(R.layout.main);
		bindListeners();
	}

	/**
	 * Setup event handlers and bind variables to values from xml
	 */
	private void bindListeners() {
		mBtnStart = (Button) findViewById(R.id.btnStart);
		mTxtSpeed = (TextView) findViewById(R.id.speed);
		mTxtConnectionSpeed = (TextView) findViewById(R.id.connectionspeeed);
		mTxtProgress = (TextView) findViewById(R.id.progress);
		mTxtNetwork = (TextView) findViewById(R.id.networktype);

		mBtnStart.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(final View view) {
				setProgressBarVisibility(true);
				mTxtSpeed.setText("Test started");	
				mBtnStart.setEnabled(false);
				mTxtNetwork.setText(R.string.network_detecting);
				new Thread(mWorker).start();				
			}
		});
	}



	private final Handler mHandler=new Handler(){
		@Override
		public void handleMessage(final Message msg) {
			switch(msg.what){
			case MSG_UPDATE_STATUS:
				final SpeedInfo info1=(SpeedInfo) msg.obj;
				mTxtSpeed.setText(String.format(getResources().getString(R.string.update_speed), mDecimalFormater.format(info1.kilobits)));
				// Title progress is in range 0..10000
				setProgress(100 * msg.arg1);
				mTxtProgress.setText(String.format(getResources().getString(R.string.update_downloaded), msg.arg2, EXPECTED_SIZE_IN_BYTES));
				break;
			case MSG_UPDATE_CONNECTION_TIME:
				mTxtConnectionSpeed.setText(String.format(getResources().getString(R.string.update_connectionspeed), msg.arg1));
				break;				
			case MSG_COMPLETE_STATUS:
				final  SpeedInfo info2=(SpeedInfo) msg.obj;
				mTxtSpeed.setText(String.format(getResources().getString(R.string.update_downloaded_complete), msg.arg1, info2.kilobits));
				
				mTxtProgress.setText(String.format(getResources().getString(R.string.update_downloaded), msg.arg1, EXPECTED_SIZE_IN_BYTES));
				
				if(networkType(info2.kilobits)==1){
					mTxtNetwork.setText(R.string.network_3g);
				}else{
					mTxtNetwork.setText(R.string.network_edge);
				}

				mBtnStart.setEnabled(true);
				setProgressBarVisibility(false);
				break;	
			default:
				super.handleMessage(msg);		
			}
		}
	};

	/**
	 * Our Slave worker that does actually all the work
	 */
	private final Runnable mWorker=new Runnable(){
		
		@Override
		public void run() {
			InputStream stream=null;
			try {
				int bytesIn=0;
				// String downloadFileUrl="http://www.gregbugaj.com/wp-content/uploads/2009/03/dummy.txt";	
				String downloadFileUrl="https://ecs7.tokopedia.net/img/cache/300/product-1/2016/9/18/901810/901810_6247b95f-bb53-49e5-b438-1230d9009443.jpg";	
				long startCon=System.currentTimeMillis(); 
				URL url=new URL(downloadFileUrl);
				URLConnection con=url.openConnection();
				con.setUseCaches(false);
				long connectionLatency=System.currentTimeMillis()- startCon;
				stream=con.getInputStream();

				Message msgUpdateConnection=Message.obtain(mHandler, MSG_UPDATE_CONNECTION_TIME);
				msgUpdateConnection.arg1=(int) connectionLatency;
				mHandler.sendMessage(msgUpdateConnection);

				long start=System.currentTimeMillis();
				int currentByte=0;
				long updateStart=System.currentTimeMillis();
				long updateDelta=0;
				int  bytesInThreshold=0;

				while((currentByte=stream.read())!=-1){	
					bytesIn++;
					bytesInThreshold++;
					if(updateDelta>=UPDATE_THRESHOLD){
						int progress=(int)((bytesIn/(double)EXPECTED_SIZE_IN_BYTES)*100);
						Message msg=Message.obtain(mHandler, MSG_UPDATE_STATUS, calculate(updateDelta, bytesInThreshold));
						msg.arg1=progress;
						msg.arg2=bytesIn;
						mHandler.sendMessage(msg);
						//Reset
						updateStart=System.currentTimeMillis();
						bytesInThreshold=0;
					}
					updateDelta = System.currentTimeMillis()- updateStart;
				}

				long downloadTime=(System.currentTimeMillis()-start);
				//Prevent AritchmeticException
				if(downloadTime==0){
					downloadTime=1;
				}

				Message msg=Message.obtain(mHandler, MSG_COMPLETE_STATUS, calculate(downloadTime, bytesIn));
				msg.arg1=bytesIn;
				mHandler.sendMessage(msg);
			} 
			catch (MalformedURLException e) {
				Log.e(TAG, e.getMessage());
			} catch (IOException e) {
				Log.e(TAG, e.getMessage());
			}finally{
				try {
					if(stream!=null){
						stream.close();
					}
				} catch (IOException e) {
					//Suppressed
				}
			}

		}
	};

	/**
	 * Get Network type from download rate
	 * @return 0 for Edge and 1 for 3G
	 */
	private int networkType(final double kbps){
		int type=1;//3G
		//Check if its EDGE
		if(kbps<EDGE_THRESHOLD){
			type=0;
		}
		return type;
	}

	/**
	 * 	
	 * 1 byte = 0.0078125 kilobits
	 * 1 kilobits = 0.0009765625 megabit
	 * 
	 * @param downloadTime in miliseconds
	 * @param bytesIn number of bytes downloaded
	 * @return SpeedInfo containing current speed
	 */
	private SpeedInfo calculate(final long downloadTime, final long bytesIn){
		SpeedInfo info=new SpeedInfo();
		//from mil to sec
		long bytespersecond   =(bytesIn / downloadTime) * 1000;
		double kilobits=bytespersecond * BYTE_TO_KILOBIT;
		double megabits=kilobits  * KILOBIT_TO_MEGABIT;
		info.downspeed=bytespersecond;
		info.kilobits=kilobits;
		info.megabits=megabits;

		return info;
	}

	/**
	 * Transfer Object
	 * @author devil
	 *
	 */
	private static class SpeedInfo{
		public double kilobits=0;
		public double megabits=0;
		public double downspeed=0;		
	}


	//Private fields	
	private static final String TAG = SpeedTestLauncher.class.getSimpleName();
	private static final int EXPECTED_SIZE_IN_BYTES = 1048576;//1MB 1024*1024

	private static final double EDGE_THRESHOLD = 176.0;
	private static final double BYTE_TO_KILOBIT = 0.0078125;
	private static final double KILOBIT_TO_MEGABIT = 0.0009765625;

	private Button mBtnStart;
	private TextView mTxtSpeed;
	private TextView mTxtConnectionSpeed;
	private TextView mTxtProgress;
	private TextView mTxtNetwork;

	private final int MSG_UPDATE_STATUS=0;
	private final int MSG_UPDATE_CONNECTION_TIME=1;
	private final int MSG_COMPLETE_STATUS=2;

	private final static int UPDATE_THRESHOLD=300;


	private DecimalFormat mDecimalFormater;

}