package com.s3lab.guoguo.v1.jedis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import android.content.Context;

import com.s3lab.guoguo.v1.R;

public class JedisInterface {

	private JedisPoolConfig conf;
	private JedisPool pool;
	private Jedis uploaderJedis;

	public JedisInterface(Context context) {
		conf = new JedisPoolConfig();
		conf.setTestOnBorrow(true);
		conf.setMaxWait(10000);
		pool = new JedisPool(conf, context.getString(R.string.jedis_server));
	}

	public void connect() {
		uploaderJedis = pool.getResource();
	}

	public void disconnect() {
		if (pool != null && uploaderJedis != null)
			pool.returnResource(uploaderJedis);
	}

	public void publish(String channel, String message) {
		if (uploaderJedis != null)
			uploaderJedis.publish(channel, message);
	}
}
