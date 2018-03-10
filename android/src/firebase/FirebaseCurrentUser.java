package org.godotengine.godot.firebase;

import android.app.Activity;
import android.util.Log;
import android.content.Intent;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONObject;
import org.json.JSONException;

public class FirebaseCurrentUser {
	private static final String TAG = "FirebaseCurrentUser";

  private static int instance_id;
	private static Activity activity = null;
  private static FirebaseCurrentUser mInstance = null;

  private FirebaseAuth mAuth;

	public static synchronized FirebaseCurrentUser getInstance (Activity p_activity) {
		if (mInstance == null) {
			mInstance = new FirebaseCurrentUser(p_activity);
		}

		return mInstance;
	}

	public FirebaseCurrentUser(Activity p_activity) {
    activity = p_activity;

		mAuth = FirebaseAuth.getInstance();
	}

	public void init(final int p_instance_id) {
    this.instance_id = p_instance_id;

    onStart();
  }

  public String get_user_details() {
    JSONObject userDetails = new JSONObject();
    FirebaseUser firebaseUser = mAuth.getCurrentUser();

    if (firebaseUser != null) {
			try {
				userDetails.put("name", firebaseUser.getDisplayName());
				userDetails.put("email", firebaseUser.getEmail());
				userDetails.put("photo_uri", firebaseUser.getPhotoUrl());
			} catch (JSONException e) {
				Log.w(TAG, "Failed to get the current user: " + e);
			}
    } else {
			Log.w(TAG, "Failed to get the current user: not connected");
		}

    return userDetails.toString();
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