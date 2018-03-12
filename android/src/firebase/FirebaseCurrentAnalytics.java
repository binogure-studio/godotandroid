package org.godotengine.godot.firebase;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.Locale;

public class FirebaseCurrentAnalytics {
	private static final String TAG = "FirebaseCurrentAnalytics";

  private static int instance_id;
	private static Activity activity = null;
  private static FirebaseCurrentAnalytics mInstance = null;

	private FirebaseAnalytics mFirebaseAnalytics;
  private FirebaseAuth mAuth;

	public static synchronized FirebaseCurrentAnalytics getInstance (Activity p_activity) {
		if (mInstance == null) {
			mInstance = new FirebaseCurrentAnalytics(p_activity);
		}

		return mInstance;
	}

	public FirebaseCurrentAnalytics(Activity p_activity) {
    activity = p_activity;

		mAuth = FirebaseAuth.getInstance();
		mFirebaseAnalytics = FirebaseAnalytics.getInstance(activity);
	}

	public void init(final int p_instance_id) {
    this.instance_id = p_instance_id;

    onStart();
  }

	public void log_event(final String eventName, final HashMap<String, Object> params) {
		Bundle bundle = new Bundle();

		for (Map.Entry param : params.entrySet()) {
			Object value = param.getValue();
			String key = param.getKey();

			if (value instanceof Boolean) {
				bundle.putBoolean(key, (Boolean) value);
			} else if (value instanceof Integer) {
				bundle.putInt(key, (Integer) value);
			} else if (value instanceof Double) {
				bundle.putDouble(key, (Double) value);
			} else if (value instanceof String) {
				bundle.putString(key, (String) value);
			} else {
				if (value != null) {
					bundle.putString(key, value.toString());
				}
			}
		}

		mFirebaseAnalytics.logEvent(name, bundle);
	}

	public void tutorial_begin(final String name) {
		HashMap<String, Object> params = new HashMap<String, Object>();

		params.put(FirebaseAnalytics.Param.LEVEL, name);

		log_event(FirebaseAnalytics.Event.TUTORIAL_BEGIN, params);
	}

	public void tutorial_complete(final String name) {
		HashMap<String, Object> params = new HashMap<String, Object>();

		params.put(FirebaseAnalytics.Param.LEVEL, name);

		log_event(FirebaseAnalytics.Event.TUTORIAL_COMPLETE, params);
	}

	public void purchase(final String item) {
		HashMap<String, Object> params = new HashMap<String, Object>();

		params.put(FirebaseAnalytics.Param.ITEM_ID, item);

		log_event(FirebaseAnalytics.Event.ECOMMERCE_PURCHASE, params);		
	}

	public void unlock_achievement(final String achievement) {
		HashMap<String, Object> params = new HashMap<String, Object>();

		params.put(FirebaseAnalytics.Param.ACHIEVEMENT_ID, achievement);

		log_event(FirebaseAnalytics.Event.UNLOCK_ACHIEVEMENT, params);
	}

	public void join_group(final String group) {
		HashMap<String, Object> params = new HashMap<String, Object>();

		params.put(FirebaseAnalytics.Param.GROUP_ID, group);

		log_event(FirebaseAnalytics.Event.JOIN_GROUP, params);
	}

	public void login() {
		HashMap<String, Object> params = new HashMap<String, Object>();

		log_event(FirebaseAnalytics.Event.LOGIN, params);
	}

	public void level_up(final String name) {
		HashMap<String, Object> params = new HashMap<String, Object>();

		params.put(FirebaseAnalytics.Param.LEVEL, name);

		log_event(FirebaseAnalytics.Event.LEVEL_UP, params);
	}

	public void post_score(final int score) {
		HashMap<String, Object> params = new HashMap<String, Object>();

		params.put(FirebaseAnalytics.Param.SCORE, score);

		log_event(FirebaseAnalytics.Event.POST_SCORE, params);
	}

	public void select_content(final String name) {
		HashMap<String, Object> params = new HashMap<String, Object>();

		params.put(FirebaseAnalytics.Param.CONTENT, name);

		log_event(FirebaseAnalytics.Event.SELECT_CONTENT, params);
	}

	public void share() {
		HashMap<String, Object> params = new HashMap<String, Object>();

		log_event(FirebaseAnalytics.Event.SHARE, params);
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