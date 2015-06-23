package com.s3lab.guoguo.v1;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class WelcomeActivity extends Activity {

	private Handler uiHandler;
	private AccountManager am;

	private String userName;
	private String pwd;

	private EditText userNameField;
	private EditText pwdField;

	private HandlerThread welcomeThread;
	private Handler welcomeHandler;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_welcome);

		userNameField = (EditText) findViewById(R.id.userName);
		pwdField = (EditText) findViewById(R.id.userPwd);

		Button loginButton = (Button) findViewById(R.id.loginbutton);
		Button signupButton = (Button) findViewById(R.id.signupbutton);

		welcomeThread = new HandlerThread("welcomeThread");
		welcomeThread.start();
		welcomeHandler = new Handler(welcomeThread.getLooper());
		Log.v("WelcomeActivity", "thread and handler ready");

		uiHandler = new Handler(getMainLooper()) {
			public void handleMessage(Message msg) {
				Log.v("uiHandler", "uiHandler received message, its what is "
						+ msg.what);
				switch (msg.what) {
				case 1:
					Log.v("uiHandler", "case 1");
					gotoMainActivity();
					break;
				case 2:
					Log.v("uiHandler", "case 2");
					Toast.makeText(getApplicationContext(),
							"no username found", Toast.LENGTH_SHORT).show();
					break;
				case 3:
					Log.v("uiHandler", "case 3");
					Toast.makeText(getApplicationContext(),
							"password not match", Toast.LENGTH_SHORT).show();
					break;
				case 4:
					Log.v("uiHandler", "case 4");
					Toast.makeText(getApplicationContext(), "unknown error",
							Toast.LENGTH_SHORT).show();
					break;
				case 5:
					Log.v("uiHandler", "case 5");
					Toast.makeText(getApplicationContext(),
							"username already existed", Toast.LENGTH_SHORT)
							.show();
					break;
				}
			}
		};

		am = new AccountManager();
		am.init();

		loginButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Log.v("Main", "login clicked");

				 if(checkValidation()){
					 //gotoMainActivity();
					 welcomeHandler.post(login);
				}
			}
		});

		signupButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Log.v("Main", "signup clicked");
				if (checkValidation()) {
					welcomeHandler.post(signup);
				}
			}
		});
	}

	private void gotoWarmupActivity() {
		Intent toMainActivity = new Intent().setClass(getApplicationContext(),
				WarmupActivity.class);
		Bundle data = new Bundle();
		data.putString("userName", userName);
		toMainActivity.putExtras(data);
		startActivity(toMainActivity);
		overridePendingTransition(R.anim.fadeout, R.anim.fadein);

	}

	private void gotoMainActivity() {
		Intent toMainActivity = new Intent().setClass(getApplicationContext(),
				MainActivity.class);
		Bundle data = new Bundle();
		data.putString("userName", userName);
		toMainActivity.putExtras(data);
		startActivity(toMainActivity);
		overridePendingTransition(R.anim.fadeout, R.anim.fadein);

	}

	private Runnable signup = new Runnable() {
		@Override
		public void run() {
			Log.v("welcomeThread", "signup running");
			int code = am.handleSignupIntent(userName, pwd);
			Message msg = new Message();
			msg.what = code;
			uiHandler.sendMessage(msg);
			Log.v("welcomeThread", "signup done");

		}
	};

	private Runnable login = new Runnable() {
		@Override
		public void run() {
			Log.v("welcomeThread", "login running");
			int code = am.handleLoginIntent(userName, pwd);
			Message msg = new Message();
			msg.what = code;
			uiHandler.sendMessage(msg);
			Log.v("welcomeThread", "login done");

		}

	};

	private boolean checkValidation() {
		userName = userNameField.getText().toString();

		pwd = pwdField.getText().toString();

		if ((userName.contentEquals("")) || (pwd.contentEquals(""))) {
			Toast.makeText(getApplicationContext(),
					"Account name and passwords are required",
					Toast.LENGTH_SHORT).show();
			return false;
		}
		return true;

	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_HOME) {
			android.os.Process.killProcess(android.os.Process.myPid());
		}
		return super.onKeyDown(keyCode, event);
	}

}
