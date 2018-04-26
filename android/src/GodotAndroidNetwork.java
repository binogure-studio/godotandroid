package org.godotengine.godot;

import android.util.Log;
import android.app.Activity;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.content.Context;
import android.content.Intent;

public class GodotAndroidNetwork {

	private static final String TAG = "GodotAndroidNetwork";
	private static ConnectivityManager connectivityManager = null;
	private static int instance_id;

	private static Activity activity = null;
	private static GodotAndroidNetwork mInstance = null;

	public static synchronized GodotAndroidNetwork getInstance (Activity p_activity) {
		if (mInstance == null) {
			mInstance = new GodotAndroidNetwork(p_activity);
		}

		return mInstance;
	}

	public GodotAndroidNetwork(final Activity p_activity) {
		activity = p_activity;
		connectivityManager = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
	}

	public void init(final int p_instance_id) {
		this.instance_id = p_instance_id;

		onStart();
	}

	private boolean isConnected(int type) {
		NetworkInfo networkInfo = connectivityManager.getNetworkInfo(type);

		return networkInfo.isConnected();
	}

	public boolean isOnline() {
		NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

		return (networkInfo != null && networkInfo.isConnected());
	}

	public boolean isWifiConnected() {
		return isConnected(ConnectivityManager.TYPE_WIFI);
	}

	public boolean isMobileConnected() {
		return isConnected(ConnectivityManager.TYPE_MOBILE);
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		// Nothing to do
	}

	public void onStart() {
		// Nothing to do
	}

	public void onPause() {
		// Nothing to do
	}

	public void onResume() {
		// Nothing to do
	}

	public void onStop() {
		activity = null;
	}
}
