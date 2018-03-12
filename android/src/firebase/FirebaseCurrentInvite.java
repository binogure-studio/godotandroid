package org.godotengine.godot.firebase;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import org.godotengine.godot.GodotLib;
import org.godotengine.godot.GodotAndroidRequest;

import com.google.android.gms.appinvite.AppInviteInvitation;
import com.google.firebase.appinvite.FirebaseAppInvite;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.PendingDynamicLinkData;

import com.godot.game.R;


public class FirebaseCurrentInvite {
	private static final String TAG = "FirebaseCurrentInvite";
  private static final int FIREBASE_INVITE_REQUEST = 0;

  private static int instance_id;
	private static Activity activity = null;
  private static FirebaseCurrentInvite mInstance = null;

	public static synchronized FirebaseCurrentInvite getInstance (Activity p_activity) {
		if (mInstance == null) {
			mInstance = new FirebaseCurrentInvite(p_activity);
		}

		return mInstance;
	}

	public FirebaseCurrentInvite(Activity p_activity) {
    activity = p_activity;
	}

	public void init(final int p_instance_id) {
    this.instance_id = p_instance_id;

    onStart();
  }

  private boolean is_message_valid(final String message) {
    boolean is_valid = true;

    if (message.length() > AppInviteInvitation.IntentBuilder.MAX_MESSAGE_LENGTH) {
      String error_message = "Cannot send invite, message too long (max " + AppInviteInvitation.IntentBuilder.MAX_MESSAGE_LENGTH + " characters)";

			Log.e(TAG, error_message);

      GodotLib.calldeferred(instance_id, "firebase_invite_failed", new Object[] { error_message });

			is_valid = false;
		}

    return is_valid;
  }

	public void invite(final String message, final String action_text) {
		if (!is_message_valid(message)) {
      return;
		}

		Intent intent = new AppInviteInvitation.IntentBuilder("Invite Friends")
		.setMessage(message)
		.setCallToActionText(action_text)
		.build();

		activity.startActivityForResult(intent, GodotAndroidRequest.FIREBASE_INVITE_REQUEST);
  }

	public void invite_with_image(final String message, final String action_text, final String custom_image_uri) {
		if (!is_message_valid(message)) {
      return;
		}

		Intent intent = new AppInviteInvitation.IntentBuilder("Invite Friends")
		.setMessage(message)
    .setCustomImage(Uri.parse(custom_image_uri))
		.setCallToActionText(action_text)
		.build();

		activity.startActivityForResult(intent, GodotAndroidRequest.FIREBASE_INVITE_REQUEST);
  }

	public void invite_with_deeplink(final String message, final String action_text, final String deep_link_uri) {
		if (!is_message_valid(message)) {
      return;
		}

		Intent intent = new AppInviteInvitation.IntentBuilder("Invite Friends")
		.setMessage(message)
		.setDeepLink(Uri.parse(deep_link_uri))
		.setCallToActionText(action_text)
		.build();

		activity.startActivityForResult(intent, GodotAndroidRequest.FIREBASE_INVITE_REQUEST);
  }

	public void invite(final String message, final String action_text, final String custom_image_uri, final String deep_link_uri) {
		if (!is_message_valid(message)) {
      return;
		}

		Intent intent = new AppInviteInvitation.IntentBuilder("Invite Friends")
		.setMessage(message)
		.setDeepLink(Uri.parse(deep_link_uri))
    .setCustomImage(Uri.parse(custom_image_uri))
		.setCallToActionText(action_text)
		.build();

		activity.startActivityForResult(intent, GodotAndroidRequest.FIREBASE_INVITE_REQUEST);
  }

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == GodotAndroidRequest.FIREBASE_INVITE_REQUEST) {
      if (resultCode == activity.RESULT_OK) {
        // Get the invitation IDs of all sent messages
        String[] ids = AppInviteInvitation.getInvitationIds(resultCode, data);

        for (String id : ids) {
          GodotLib.calldeferred(instance_id, "firebase_invite_success", new Object[] { id });
        }
      } else {
        String message = "Failed to send invite";

        Log.e(TAG, message);

        GodotLib.calldeferred(instance_id, "firebase_invite_failed", new Object[] { message });
      }
    }
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