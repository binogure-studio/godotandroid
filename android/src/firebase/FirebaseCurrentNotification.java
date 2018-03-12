package org.godotengine.godot.firebase;

import android.app.Activity;
import android.util.Log;
import android.content.Intent;

import org.godotengine.godot.GodotLib;
import org.godotengine.godot.GodotAndroidRequest;

import com.google.firebase.auth.FirebaseAuth;

import java.util.HashMap;
import java.util.Map;

public class FirebaseCurrentNotification extends FirebaseMessagingService {
	private static final String TAG = "FirebaseCurrentNotification";

  private static int instance_id;
	private static Activity activity = null;
  private static FirebaseCurrentNotification mInstance = null;

  private FirebaseAuth mAuth;

	public static synchronized FirebaseCurrentNotification getInstance (Activity p_activity) {
		if (mInstance == null) {
			mInstance = new FirebaseCurrentNotification(p_activity);
		}

		return mInstance;
	}

	public FirebaseCurrentNotification(Activity p_activity) {
    activity = p_activity;

		mAuth = FirebaseAuth.getInstance();
	}

  @Override
  public void onMessageReceived(RemoteMessage remoteMessage) {
    Bundle notification = remoteMessage.getNotification();

    // Check if message contains a notification payload.
    if (notification != null) {
      // Convert payload to Dictionary
      Map<String, String> payloadMap = remoteMessage.getData();
      Dictionary payload = new Dictionary();

      payload.putAll(payloadMap);

      String title = notification.geTitle();
      String body = notification.getBody();
      String tag = notification.getTag();
      String link = notification.getLink().toString();

      GodotLib.calldeferred(instance_id, "firebase_notification", new Object[] {
        title, body, tag, link, payload
      });
    }
  }

	public void init(final int p_instance_id) {
    this.instance_id = p_instance_id;

    onStart();
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