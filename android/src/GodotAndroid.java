
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

	private GoogleAchievements googleAchievements;
	private GoogleAuthentication googleAuthentication;
	private GoogleLeaderboard googleLeaderboard;
	private GoogleSnapshot googleSnapshot;

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
				googleAchievements = GoogleAchievements.getInstance(activity);
				googleAchievements.init(instance_id);

				googleAuthentication = GoogleAuthentication.getInstance(activity);
				googleAuthentication.init(instance_id);

				googleLeaderboard = GoogleLeaderboard.getInstance(activity);
				googleLeaderboard.init(instance_id);

				googleSnapshot = GoogleSnapshot.getInstance(activity);
				googleSnapshot.init(instance_id);
			}
		});
	}

	public void google_connect() {
		activity.runOnUiThread(new Runnable() {
			public void run() {
				googleAuthentication.connect();
			}
		});
	}

	public void google_disconnect() {
		activity.runOnUiThread(new Runnable() {
			public void run() {
				googleAuthentication.disconnect();
			}
		});
	}

	public boolean google_is_connected() {
		return googleAuthentication.isConnected();
	}

	// Google Leaderboards
	public void google_leaderboard_submit(final String id, final int score) {
		activity.runOnUiThread(new Runnable() {
			public void run() {
				googleLeaderboard.leaderboard_submit(id, score);
			}
		});
	}

	public void google_leaderboard_show(final String id) {
		activity.runOnUiThread(new Runnable() {
			public void run() {
				googleLeaderboard.leaderboard_show(id);
			}
		});
	}

	public void google_leaderboard_showlist() {
		activity.runOnUiThread(new Runnable() {
			public void run() {
				googleLeaderboard.leaderboard_showlist();
			}
		});
	}

	// Google snapshots
	public void google_snapshot_load(final String snapshotName, final int conflictResolutionPolicy) {
		googleSnapshot.snapshot_load(snapshotName, conflictResolutionPolicy);
	}

	public void google_snapshot_save(final String snapshotName, final String data, final String description, final boolean flag_force) {
		googleSnapshot.snapshot_save(snapshotName, data, description, flag_force);
	}

	// Google achievements
	public void google_achievement_unlock(final String id) {
		activity.runOnUiThread(new Runnable() {
			public void run() {
				googleAchievements.achievement_unlock(id);
			}
		});
	}

	public void google_achievement_increment(final String id, final int amount) {
		activity.runOnUiThread(new Runnable() {
			public void run() {
				googleAchievements.achievement_increment(id, amount);
			}
		});
	}

	public void google_achievement_show_list() {
		activity.runOnUiThread(new Runnable() {
			public void run() {
				googleAchievements.achievement_show_list();
			}
		});
	}

	protected void onMainActivityResult (int requestCode, int resultCode, Intent data) {
		// Trigger google's services
		googleAchievements.onActivityResult(requestCode, resultCode, data);
		googleAuthentication.onActivityResult(requestCode, resultCode, data);
		googleLeaderboard.onActivityResult(requestCode, resultCode, data);
		googleSnapshot.onActivityResult(requestCode, resultCode, data);
	}

	protected void onMainPause () {
		// Trigger google's services
		googleAchievements.onPause();
		googleAuthentication.onPause();
		googleLeaderboard.onPause();
		googleSnapshot.onPause();
	}

	protected void onMainResume () {
		// Trigger google's services
		googleAchievements.onResume();
		googleAuthentication.onResume();
		googleLeaderboard.onResume();
		googleSnapshot.onResume();
	}

	protected void onMainDestroy () {
		// Trigger google's services
		googleAchievements.onStop();
		googleAuthentication.onStop();
		googleLeaderboard.onStop();
		googleSnapshot.onStop();
	}
}
