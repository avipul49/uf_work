package com.s3lab.guoguo.v1;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class MainActivity extends FragmentActivity implements OnClickListener {

	String userName;

	DataServiceThread dataThread;
	DataServiceHandler dataHandler;
	boolean stopped = false;
	private LineGraphSeries<DataPoint> series;
	Button startButton;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		startButton = (Button) findViewById(R.id.stop_button);

		startButton.setOnClickListener(this);

		Bundle data = getIntent().getExtras();
		userName = data.getString("userName");

		GraphView graph = (GraphView) findViewById(R.id.graph);
		series = new LineGraphSeries<DataPoint>(new DataPoint[] {});
		graph.addSeries(series);

		dataThread = new DataServiceThread("data service");
		dataThread.start();

		dataHandler = new DataServiceHandler(dataThread.getLooper());
		dataHandler.post(start);
	}

	@Override
	public void onClick(View v) {
		if (stopped)
			dataHandler.post(start);
		else
			dataHandler.post(stop);
		stopped = !stopped;
		startButton.setText(stopped ? "Start" : "stop");
	}

	private Runnable stop = new Runnable() {

		@Override
		public void run() {
			Log.v("data service", "about to stop");
			Intent sstop = new Intent().setClass(getApplicationContext(),
					DataService.class);
			stopService(sstop);
		}

	};

	private Runnable start = new Runnable() {

		@Override
		public void run() {
			Log.v("data service", "about to start");
			Intent sstart = new Intent().setClass(getApplicationContext(),
					DataService.class);
			Log.v("data service", "intent done");
			Bundle dataToService = new Bundle();
			dataToService.putString("userName", userName);
			sstart.putExtras(dataToService);
			startService(sstart);
			Log.v("data service", "start service called");
		}

	};

	private class DataServiceHandler extends Handler {
		public DataServiceHandler(Looper looper) {
			super(looper);
			Log.v("Handler", "Handler ready");
		}

		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			Log.v("Handler", "Message Received");

		}
	}

	private class DataServiceThread extends HandlerThread {

		public DataServiceThread(String name) {
			super(name);
		}

		protected void onLooperPrepared() {
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		IntentFilter filter = new IntentFilter();
		filter.addAction(DataService.AUDIO_BYTES_RECIEVED);
		registerReceiver(myReceiver, filter);
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(myReceiver);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main2, menu);
		return true;
	}

	BroadcastReceiver myReceiver = new BroadcastReceiver() {
		public void onReceive(android.content.Context context, Intent intent) {
			float[] data = intent.getFloatArrayExtra("data");
			if (data != null) {
				new ShowGraph(data).execute();
			}
		};
	};

	class ShowGraph extends AsyncTask<Void, Void, DataPoint[]> {
		private float[] preProcessContainer;

		public ShowGraph(float[] preProcessContainer) {
			this.preProcessContainer = preProcessContainer;
		}

		@Override
		protected DataPoint[] doInBackground(Void... params) {
			return generateData(preProcessContainer);
		}

		@Override
		protected void onPostExecute(DataPoint[] result) {
			series.resetData(result);
		}

		private DataPoint[] generateData(float[] data) {
			int count = data.length;
			DataPoint[] values = new DataPoint[count];
			for (int i = 0; i < count; i++) {
				double x = i;
				DataPoint v = new DataPoint(x, data[i]);
				values[i] = v;
			}
			return values;
		}
	}

}