package com.s3lab.guoguo.v1.jedis;

import android.content.Context;
import android.os.AsyncTask;

public class JedisService {
	private JedisInterface jedisInterface;

	public JedisService(Context context) {
		jedisInterface = new JedisInterface(context);
	}

	public void start() {
		new StartJedisTast().execute();
	}

	public void sendData(String channel, String message) {
		new SendDataTask().execute(channel, message);
	}

	public void stop() {
		jedisInterface.disconnect();
	}

	class StartJedisTast extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			jedisInterface.connect();
			return null;
		}
	};

	class SendDataTask extends AsyncTask<String, Void, Void> {
		@Override
		protected Void doInBackground(String... params) {
			jedisInterface.publish(params[0], params[1]);
			return null;
		}
	};

}
