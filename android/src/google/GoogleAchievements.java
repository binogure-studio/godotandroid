package org.godotengine.godot.google;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.util.Log;
import android.view.View;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import com.google.android.gms.tasks.Tasks;

import java.util.Arrays;
import java.util.List;
import java.io.IOException;

import org.json.JSONObject;
import org.json.JSONException;

import org.godotengine.godot.GodotLib;
import org.godotengine.godot.GodotAndroidRequest;

import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.games.AchievementsClient;
import com.google.android.gms.games.achievement.Achievements;
import com.google.android.gms.games.Games;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;

public class GoogleAchievements {
	private static int instance_id;
	private static Activity activity = null;
	private static Context context = null;
	private static GoogleAchievements mInstance = null;
	private static int script_id;

	private static final String TAG = "GoogleAchievements";

	public static GoogleAchievements getInstance(Activity p_activity) {
		if (mInstance == null) {
			synchronized (GoogleAchievements.class) {
				mInstance = new GoogleAchievements(p_activity);
			}
		}

		return mInstance;
	}

	public GoogleAchievements(Activity p_activity) {
		activity = p_activity;
	}

	public void init(final int p_instance_id) {
		this.instance_id = p_instance_id;
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

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		// Nothing to do
	}

	protected boolean is_connected() {
		GoogleAuthentication googleAuthentication = GoogleAuthentication.getInstance(activity);

		return googleAuthentication.isConnected();
	}

	protected AchievementsClient get_achievement_client() {
		GoogleAuthentication googleAuthentication = GoogleAuthentication.getInstance(activity);

		return Games.getAchievementsClient(activity, googleAuthentication.get_account());
	}

	public void achievement_unlock(final String achievement_id) {
		if (is_connected()) {
			AchievementsClient achievementsClient = get_achievement_client();

			achievementsClient.unlock(achievement_id);
			GodotLib.calldeferred(instance_id, "google_achievement_unlocked", new Object[] { achievement_id });
		} else {
			String message = "PlayGameServices: Google not connected";

			Log.i(TAG, "PlayGameServices: Google not connected");

			GodotLib.calldeferred(instance_id, "google_achievement_unlock_failed", new Object[] { message });
		}
	}

	public void achievement_increment(final String achievement_id, final int amount) {
		if (is_connected()) {
			AchievementsClient achievementsClient = get_achievement_client();

			achievementsClient.increment(achievement_id, amount);

			GodotLib.calldeferred(instance_id, "google_achievement_increased", new Object[] { achievement_id, amount });
		} else {
			String message = "PlayGameServices: Google not connected";

			GodotLib.calldeferred(instance_id, "google_achievement_increment_failed", new Object[] { message });
		}
	}

	public void achievement_show_list() {
		if (is_connected()) {
			AchievementsClient achievementsClient = get_achievement_client();

			achievementsClient.getAchievementsIntent()
			.addOnSuccessListener(new OnSuccessListener<Intent>() {
				@Override
				public void onSuccess(Intent intent) {
					activity.startActivityForResult(intent, GodotAndroidRequest.GOOGLE_ACHIEVEMENT_REQUEST);
				}
			})
			.addOnFailureListener(new OnFailureListener() {
				@Override
				public void onFailure(@NonNull Exception e) {
					Log.d(TAG, "Failed to show achievments: " + e.toString());
				}
			});

		} else {
			Log.i(TAG, "PlayGameServices: Google not connected");
		}
	}
}
