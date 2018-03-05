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

import java.util.Arrays;
import java.util.List;
import java.io.IOException;

import org.json.JSONObject;
import org.json.JSONException;

import org.godotengine.godot.GodotLib;

import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.games.AchievementsClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.LeaderboardsClient;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;

public class GoogleLeaderboard {
	private static int instance_id;
	private static Activity activity = null;
	private static Context context = null;
	private static GoogleLeaderboard mInstance = null;

	private static int script_id;

	private static final int REQUEST_LEADERBOARD = 9003;
	
	private static final String TAG = "GoogleLeaderboard";

	public static GoogleLeaderboard getInstance(Activity p_activity) {
		if (mInstance == null) {
			synchronized (GoogleLeaderboard.class) {
				mInstance = new GoogleLeaderboard(p_activity);
			}
		}

		return mInstance;
	}

	public GoogleLeaderboard(Activity p_activity) {
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

	protected LeaderboardsClient get_leaderboard_client() {
		GoogleAuthentication googleAuthentication = GoogleAuthentication.getInstance(activity);

		return Games.getLeaderboardsClient(activity, googleAuthentication.get_account());
	}

	public void leaderboard_submit(String id, int score) {
		if (is_connected()) {
      LeaderboardsClient leaderboardsClient = get_leaderboard_client();

			leaderboardsClient.submitScore(id, score);

			GodotLib.calldeferred(instance_id, "google_leaderboard_submitted", new Object[] { id, score });
		} else {
			String message = "PlayGameServices: Google not connected";

			GodotLib.calldeferred(instance_id, "google_leaderboard_submit_failed", new Object[] { message });
    }
	}

	public void leaderboard_show(final String l_id) {
		if (is_connected()) {
      LeaderboardsClient leaderboardsClient = get_leaderboard_client();

			leaderboardsClient.getLeaderboardIntent(l_id)
      .addOnSuccessListener(new OnSuccessListener<Intent>() {
				@Override
				public void onSuccess (Intent intent) {
					activity.startActivityForResult(intent, REQUEST_LEADERBOARD);

					GodotLib.calldeferred(instance_id, "google_leaderboard_showd", new Object[] { });
				}
			})
			.addOnFailureListener(new OnFailureListener() {
				@Override
				public void onFailure(@NonNull Exception e) {
					String message = "Failed to show leaderboard: " + e.getMessage();

					Log.d(TAG, message);

					GodotLib.calldeferred(instance_id, "google_leaderboard_show_failed", new Object[] { message });
				}
			});

		} else {
			String message = "PlayGameServices: Google not connected";

			GodotLib.calldeferred(instance_id, "google_leaderboard_show_failed", new Object[] { message });
    }
	}

	public void leaderboard_showlist() {
		if (is_connected()) {
      LeaderboardsClient leaderboardsClient = get_leaderboard_client();

			leaderboardsClient.getAllLeaderboardsIntent()
			.addOnSuccessListener(new OnSuccessListener<Intent>() {
				@Override
				public void onSuccess (Intent intent) {
					Log.d(TAG, "Showing leaderboards");
					activity.startActivityForResult(intent, REQUEST_LEADERBOARD);

					GodotLib.calldeferred(instance_id, "google_leaderboard_showlisted", new Object[] { });
				}
			})
			.addOnFailureListener(new OnFailureListener() {
				@Override
				public void onFailure(@NonNull Exception e) {
					String message = "Failed to show list leaderboard: " + e.getMessage();

					Log.d(TAG, message);

					GodotLib.calldeferred(instance_id, "google_leaderboard_showlist_failed", new Object[] { message });
				}
			});

		} else {
			String message = "PlayGameServices: Google not connected";

			GodotLib.calldeferred(instance_id, "google_leaderboard_showlist_failed", new Object[] { message });
    }
	}
}
