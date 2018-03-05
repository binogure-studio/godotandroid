
package org.godotengine.godot;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.IntentSender.SendIntentException;
import android.util.Log;
import android.view.View;
import android.os.Bundle;

import com.google.android.gms.games.SnapshotsClient;

import java.util.Arrays;
import java.util.List;
import java.util.HashMap;

import org.json.JSONObject;
import org.json.JSONException;

// Import google play service
import org.godotengine.godot.google.GoogleAchievements;
import org.godotengine.godot.google.GoogleAuthentication;
import org.godotengine.godot.google.GoogleLeaderboard;
import org.godotengine.godot.google.GoogleSnapshot;

public class GodotAndroid extends Godot.SingletonBase {

	public static final HashMap<String, Integer> GOOGLE_SNAPSHOT_RESOLUTION_POLICIES;

	private static Context context;
	private static Activity activity;

	static {
		GOOGLE_SNAPSHOT_RESOLUTION_POLICIES = new HashMap<String, Integer>();

		GOOGLE_SNAPSHOT_RESOLUTION_POLICIES.put("RESOLUTION_POLICY_HIGHEST_PROGRESS", new Integer(SnapshotsClient.RESOLUTION_POLICY_HIGHEST_PROGRESS));
		GOOGLE_SNAPSHOT_RESOLUTION_POLICIES.put("RESOLUTION_POLICY_LAST_KNOWN_GOOD", new Integer(SnapshotsClient.RESOLUTION_POLICY_LAST_KNOWN_GOOD));
		GOOGLE_SNAPSHOT_RESOLUTION_POLICIES.put("RESOLUTION_POLICY_LONGEST_PLAYTIME", new Integer(SnapshotsClient.RESOLUTION_POLICY_LONGEST_PLAYTIME));
		GOOGLE_SNAPSHOT_RESOLUTION_POLICIES.put("RESOLUTION_POLICY_MANUAL", new Integer(SnapshotsClient.RESOLUTION_POLICY_MANUAL));
		GOOGLE_SNAPSHOT_RESOLUTION_POLICIES.put("RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED", new Integer(SnapshotsClient.RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED));
	};

	static public Godot.SingletonBase initialize (Activity p_activity) {
		return new GodotAndroid(p_activity);
	}

	public GodotAndroid(Activity p_activity) {
		registerClass ("GodotAndroid", new String[] {
			// Google's services
			"google_initialize",

			// GoogleAuthentication
			"google_connect", "google_disconnect", "google_is_connected",

			// GoogleLeaderboard
			"google_leaderboard_submit", "google_leaderboard_show", "google_leaderboard_showlist",

			// GoogleSnapshot
			"google_snapshot_load", "google_snapshot_save",

			// GoogleAchievements
			"google_achievement_unlock", "google_achievement_increment", "google_achievement_show_list"
		});

		activity = p_activity;
	}

	// Google GoogleAuthentication
	public void google_initialize(final int instance_id) {
		activity.runOnUiThread(new Runnable() {
			public void run() {
				GoogleAchievements.getInstance(activity).init(instance_id);
				GoogleAuthentication.getInstance(activity).init(instance_id);
				GoogleLeaderboard.getInstance(activity).init(instance_id);
				GoogleSnapshot.getInstance(activity).init(instance_id);
			}
		});
	}

	public void google_connect() {
		activity.runOnUiThread(new Runnable() {
			public void run() {
				GoogleAuthentication.getInstance(activity).connect();
			}
		});
	}

	public void google_disconnect() {
		activity.runOnUiThread(new Runnable() {
			public void run() {
				GoogleAuthentication.getInstance(activity).disconnect();
			}
		});
	}

	public boolean google_is_connected() {
		return GoogleAuthentication.getInstance(activity).isConnected();
	}

	// Google Leaderboards
	public void google_leaderboard_submit(final String id, final int score) {
		activity.runOnUiThread(new Runnable() {
			public void run() {
				GoogleLeaderboard.getInstance(activity).leaderboard_submit(id, score);
			}
		});
	}

	public void google_leaderboard_show(final String id) {
		activity.runOnUiThread(new Runnable() {
			public void run() {
				GoogleLeaderboard.getInstance(activity).leaderboard_show(id);
			}
		});
	}

	public void google_leaderboard_showlist() {
		activity.runOnUiThread(new Runnable() {
			public void run() {
				GoogleLeaderboard.getInstance(activity).leaderboard_showlist();
			}
		});
	}

	// Google snapshots
	public void google_snapshot_load(final String snapshotName, final int conflictResolutionPolicy) {
		activity.runOnUiThread(new Runnable() {
			public void run() {
				GoogleSnapshot.getInstance(activity).snapshot_load(snapshotName, conflictResolutionPolicy);
			}
		});
	}

	public void google_snapshot_save(final String snapshotName, final String data, final String description, final boolean flag_force) {
		activity.runOnUiThread(new Runnable() {
			public void run() {
				GoogleSnapshot.getInstance(activity).snapshot_save(snapshotName, data, description, flag_force);
			}
		});
	}

	// Google achievements
	public void google_achievement_unlock(final String id) {
		activity.runOnUiThread(new Runnable() {
			public void run() {
				GoogleAchievements.getInstance(activity).achievement_unlock(id);
			}
		});
	}

	public void google_google_achievement_increment(final String id, final int amount) {
		activity.runOnUiThread(new Runnable() {
			public void run() {
				GoogleAchievements.getInstance(activity).achievement_increment(id, amount);
			}
		});
	}

	public void google_achievement_show_list() {
		activity.runOnUiThread(new Runnable() {
			public void run() {
				GoogleAchievements.getInstance(activity).achievement_show_list();
			}
		});
	}

	protected void onMainActivityResult (int requestCode, int resultCode, Intent data) {
		// Trigger google's services
		GoogleAchievements.getInstance(activity).onActivityResult(requestCode, resultCode, data);
		GoogleAuthentication.getInstance(activity).onActivityResult(requestCode, resultCode, data);
		GoogleLeaderboard.getInstance(activity).onActivityResult(requestCode, resultCode, data);
		GoogleSnapshot.getInstance(activity).onActivityResult(requestCode, resultCode, data);
	}

	protected void onMainPause () {
		// Trigger google's services
		GoogleAchievements.getInstance(activity).onPause();
		GoogleAuthentication.getInstance(activity).onPause();
		GoogleLeaderboard.getInstance(activity).onPause();
		GoogleSnapshot.getInstance(activity).onPause();
	}

	protected void onMainResume () {
		// Trigger google's services
		GoogleAchievements.getInstance(activity).onResume();
		GoogleAuthentication.getInstance(activity).onResume();
		GoogleLeaderboard.getInstance(activity).onResume();
		GoogleSnapshot.getInstance(activity).onResume();
	}

	protected void onMainDestroy () {
		// Trigger google's services
		GoogleAchievements.getInstance(activity).onStop();
		GoogleAuthentication.getInstance(activity).onStop();
		GoogleLeaderboard.getInstance(activity).onStop();
		GoogleSnapshot.getInstance(activity).onStop();
	}
}
