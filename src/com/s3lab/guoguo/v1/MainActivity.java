package com.s3lab.guoguo.v1;

import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.LinearGradient;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainActivity extends FragmentActivity {

	// private static final String STATE_SELECTED_NAVIGATION_ITEM =
	// "selected_navigation_item";
	String userName;

	DataServiceThread dataThread;
	DataServiceHandler dataHandler;
	boolean stopped = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		final Button button = (Button) findViewById(R.id.stop_button);

		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (stopped)
					dataHandler.post(start);
				else
					dataHandler.post(stop);
				stopped = !stopped;
				button.setText(stopped ? "Start" : "stop");

			}
		});
		// final ActionBar actionBar = getActionBar();
		// actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		//
		// //
		// actionBar.addTab(actionBar.newTab().setText("Guide").setTabListener(onGuideTabClick));
		// actionBar.addTab(actionBar.newTab().setText("Social")
		// .setTabListener(onSocialTabClick));
		// actionBar.addTab(actionBar.newTab().setText("Map")
		// .setTabListener(onMapTabClick));

		Bundle data = getIntent().getExtras();
		userName = data.getString("userName");

		dataThread = new DataServiceThread("data service");
		dataThread.start();

		dataHandler = new DataServiceHandler(dataThread.getLooper());

		dataHandler.post(start);

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

	TabListener onGuideTabClick = new TabListener() {

		@Override
		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
		}

		@Override
		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			Fragment fragment = new GuideFragment();
			// Bundle args = new Bundle();
			// args.putInt(DummySectionFragment.ARG_SECTION_NUMBER,
			// tab.getPosition() + 1);
			// fragment.setArguments(args);
			getSupportFragmentManager().beginTransaction()
					.replace(R.id.container, fragment).commit();
		}

		@Override
		public void onTabReselected(Tab tab, FragmentTransaction ft) {
		}
	};

	TabListener onSocialTabClick = new TabListener() {

		@Override
		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
		}

		@Override
		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			Fragment fragment = new SocialFragment();

			getSupportFragmentManager().beginTransaction()
					.replace(R.id.container, fragment).commit();
		}

		@Override
		public void onTabReselected(Tab tab, FragmentTransaction ft) {
		}
	};

	TabListener onMapTabClick = new TabListener() {

		@Override
		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
		}

		@Override
		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			Log.v("MainActivity", "map tab selected");
			// fragment.registerReceiver();
			MapFragment fragment = new MapFragment();

			getSupportFragmentManager().beginTransaction()
					.replace(R.id.container, fragment).commit();
		}

		@Override
		public void onTabReselected(Tab tab, FragmentTransaction ft) {
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
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		// if (savedInstanceState.containsKey(STATE_SELECTED_NAVIGATION_ITEM)) {
		// getActionBar().setSelectedNavigationItem(
		// savedInstanceState.getInt(STATE_SELECTED_NAVIGATION_ITEM));
		// }
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		// outState.putInt(STATE_SELECTED_NAVIGATION_ITEM,
		// getActionBar().getSelectedNavigationIndex());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main2, menu);
		return true;
	}

}
