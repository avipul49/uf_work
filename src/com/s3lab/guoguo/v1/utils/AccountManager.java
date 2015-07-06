package com.s3lab.guoguo.v1.utils;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import android.content.Context;
import android.util.Log;

import com.s3lab.guoguo.v1.R;

public class AccountManager {
	JedisPool pool;
	JedisPoolConfig conf;

	Jedis toolJedis;

	String redisIP;
	private Context context;

	static final int SUCCESS = 1;
	static final int NOUSER = 2;
	static final int WRONGPWD = 3;
	static final int UNKNOWN = 4;
	static final int DUPLICATED = 5;

	public AccountManager(Context context) {
		this.context = context;
	}

	public void init() {

		redisIP = context.getString(R.string.jedis_server);

		conf = new JedisPoolConfig();
		conf.setTestOnBorrow(true);
		conf.setMaxActive(10000);
		conf.setMaxIdle(5000);
		conf.setMaxWait(10000);
		pool = new JedisPool(conf, redisIP);

		Log.v("Event Engine", "init done");

	}

	/* login event */
	public int handleLoginIntent(String username, String pwd) {
		Log.v("Event Engine", "start to handle login");

		String accountPool = "accountPool";
		String passwordField = "password";

		toolJedis = pool.getResource();

		if (toolJedis.hlen(username) == 0) {
			return NOUSER;
		} else if (toolJedis.hexists(accountPool, passwordField)) {
			return UNKNOWN;
		} else {
			String existingPwd = toolJedis.hget(username, passwordField);
			if (!(existingPwd.contentEquals(pwd))) {
				return WRONGPWD;
			}
		}

		return SUCCESS;
	}

	public int handleSignupIntent(String username, String pwd) {
		Log.v("Event Engine", "start to handle signup");

		String passwordField = "password";

		toolJedis = pool.getResource();
		toolJedis.connect();

		if (toolJedis.hlen(username) == 1) {
			pool.returnResource(toolJedis);
			return DUPLICATED;
		}

		toolJedis.hset(username, passwordField, pwd);
		pool.returnResource(toolJedis);
		return SUCCESS;
	}

	public void handleError(int code) {
	}

}
