package com.s3lab.guoguo.v1;

import java.util.ArrayList;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import android.util.Log;

public class AccountManager {
	JedisPool pool;
	JedisPoolConfig conf;

	Jedis toolJedis;

	String redisIP;

	static final int SUCCESS = 1;
	static final int NOUSER = 2;
	static final int WRONGPWD = 3;
	static final int UNKNOWN = 4;
	static final int DUPLICATED = 5;

	public AccountManager() {

	}

	public void init() {

		redisIP = "10.137.35.33";

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
		// toolJedis.connect();

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

	/* signup event */
	public int handleSignupIntent(String username, String pwd) {
		Log.v("Event Engine", "start to handle signup");

		String accountPool = "accountPool";
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

	/* today special event */
	public ArrayList<String> handleTSIntent(String hashName) {

		Log.v("Event Engine", "start to handle TS");
		ArrayList<String> result = new ArrayList<String>(2);

		try {
			String tsDesName = "description";
			String tsImgName = "image";

			toolJedis = pool.getResource();
			toolJedis.connect();

			String des = toolJedis.hget(hashName, tsDesName);
			// String imgSource = toolJedis.hget(hashName, tsImgName);
			// byte[] imgByte_compressed = imgSource.getBytes();
			// byte[] imgByte = Snappy.uncompress(imgByte_compressed);
			// String imgStr = new String(imgByte);

			result.add(0, des);
			// result.add(imgStr);
		} catch (Exception e) {
			e.printStackTrace();
		}
		Log.v("Event Engine", "TS handle done");

		// TODO handle error, like null content

		return result;

	}

	public void handleError(int code) {

		// TODO
	}

}
