
package org.godotengine.godot;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.IntentSender.SendIntentException;
import android.net.Uri;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.View;
import android.os.Bundle;

import com.godot.game.BuildConfig;
import com.google.android.gms.games.SnapshotsClient;

import org.godotengine.godot.GodotAndroidRequest;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;

import org.json.JSONObject;
import org.json.JSONException;

import org.godotengine.godot.Dictionary;

// Import google play service
import org.godotengine.godot.google.GoogleAchievements;
import org.godotengine.godot.google.GoogleAuthentication;
import org.godotengine.godot.google.GooglePlayer;
import org.godotengine.godot.google.GoogleLeaderboard;
import org.godotengine.godot.google.GoogleSnapshot;

// Import facebook
import org.godotengine.godot.facebook.FacebookAuthentication;
import org.godotengine.godot.facebook.FacebookShare;

// Import firebase
import org.godotengine.godot.firebase.FirebaseCurrentUser;
import org.godotengine.godot.firebase.FirebaseCurrentAnalytics;
import org.godotengine.godot.firebase.FirebaseCurrentInvite;
import org.godotengine.godot.firebase.FirebaseCurrentNotification;
import org.godotengine.godot.firebase.FirebaseCurrentAuthentication;

public class GodotAndroid extends Godot.SingletonBase {

	private static final String TAG = "GodotAndroid";
	private static Context context;
	private static Activity activity;

	private GoogleAchievements googleAchievements;
	private GoogleAuthentication googleAuthentication;
	private GooglePlayer googlePlayer;
	private GoogleLeaderboard googleLeaderboard;
	private GoogleSnapshot googleSnapshot;

	private FacebookAuthentication facebookAuthentication;
	private FacebookShare facebookShare;

	private FirebaseCurrentUser firebaseCurrentUser;
	private FirebaseCurrentAnalytics firebaseCurrentAnalytics;
	private FirebaseCurrentInvite firebaseCurrentInvite;
	private FirebaseCurrentAuthentication firebaseCurrentAuthentication;

	public static final Dictionary GOOGLE_SNAPSHOT_RESOLUTION_POLICIES;

	static {
		GOOGLE_SNAPSHOT_RESOLUTION_POLICIES = new Dictionary();

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
			"google_snapshot_load", "google_snapshot_save", "get_google_resolution_policies",

			// GoogleAchievements
			"google_achievement_unlock", "google_achievement_increment", "google_achievement_show_list",

			// Facebook
			"facebook_initialize",

			// FacebookAuthentication
			"facebook_connect", "facebook_disconnect", "facebook_is_connected",

			// FacebookShare
			"facebook_share_link",

			// Firebase
			"firebase_initialize",

			// FirebaseCurrentUser
			"firebase_get_user_details",

			// FirebaseCurrentAnalytics
			"firebase_analytics_log_event", "firebase_analytics_tutorial_begin", "firebase_analytics_tutorial_complete", "firebase_analytics_purchase",
			"firebase_analytics_unlock_achievement", "firebase_analytics_join_group", "firebase_analytics_login", "firebase_analytics_level_up", 
			"firebase_analytics_post_score", "firebase_analytics_select_content", "firebase_analytics_share",

			// FirebaseCurrentInvite
			"firebase_invite",

			// FirebaseMessaging
			"firebase_get_fcm",

			// FirebaseCurrentAuthentication
			"firebase_connect",

			// Share
			"share", "get_shared_directory"
		});

		activity = p_activity;
		context = activity.getApplicationContext();

		// Initiliaze singletons here
		firebaseCurrentUser = FirebaseCurrentUser.getInstance(activity);
		firebaseCurrentAnalytics = FirebaseCurrentAnalytics.getInstance(activity);
		firebaseCurrentInvite = FirebaseCurrentInvite.getInstance(activity);
		firebaseCurrentAuthentication = FirebaseCurrentAuthentication.getInstance(activity);

		googleAchievements = GoogleAchievements.getInstance(activity);
		googleAuthentication = GoogleAuthentication.getInstance(activity);
		googlePlayer = GooglePlayer.getInstance(activity);
		googleLeaderboard = GoogleLeaderboard.getInstance(activity);
		googleSnapshot = GoogleSnapshot.getInstance(activity);

		facebookAuthentication = FacebookAuthentication.getInstance(activity);
		facebookShare = FacebookShare.getInstance(activity);
	}

	public Dictionary get_google_resolution_policies() {
		return GOOGLE_SNAPSHOT_RESOLUTION_POLICIES;
	}

	public void firebase_initialize(final int instance_id) {
		activity.runOnUiThread(new Runnable() {
			public void run() {
				firebaseCurrentUser.init(instance_id);
				firebaseCurrentAnalytics.init(instance_id);
				firebaseCurrentInvite.init(instance_id);
				firebaseCurrentAuthentication.init(instance_id);

				// Static class
				FirebaseCurrentNotification.init(instance_id);
			}
		});
	}

	public String firebase_get_fcm() {
		// Static class
		return FirebaseCurrentNotification.getFirebaseCloudMessageToken();
	}

	public void firebase_invite(final String message, final String action_text, final String custom_image_uri, final String deep_link_uri) {
		activity.runOnUiThread(new Runnable() {
			public void run() {
				if (custom_image_uri.length() > 0 && deep_link_uri.length() > 0 ) {
					firebaseCurrentInvite.invite(message, action_text, custom_image_uri, deep_link_uri);
				} else if (custom_image_uri.length() > 0) {
					firebaseCurrentInvite.invite_with_image(message, action_text, custom_image_uri);
				} else if (deep_link_uri.length() > 0) {
					firebaseCurrentInvite.invite_with_deeplink(message, action_text, deep_link_uri);
				} else {
					firebaseCurrentInvite.invite(message, action_text);
				}
			}
		});
	}

	public void firebase_connect() {
		activity.runOnUiThread(new Runnable() {
			public void run() {
				firebaseCurrentAuthentication.connect();
			}
		});
	}

	public String firebase_get_user_details() {
		return firebaseCurrentUser.get_user_details();
	}

	public void firebase_analytics_log_event(final String event_name, final Dictionary params) {
		firebaseCurrentAnalytics.log_event(event_name, params);
	}

	public void firebase_analytics_tutorial_begin(final String name) {
		firebaseCurrentAnalytics.tutorial_begin(name);
	}

	public void firebase_analytics_tutorial_complete(final String name) {
		firebaseCurrentAnalytics.tutorial_complete(name);
	}

	public void firebase_analytics_purchase(final String item) {
		firebaseCurrentAnalytics.purchase(item);
	}

	public void firebase_analytics_unlock_achievement(final String achievement) {
		firebaseCurrentAnalytics.unlock_achievement(achievement);
	}

	public void firebase_analytics_join_group(final String group) {
		firebaseCurrentAnalytics.join_group(group);
	}

	public void firebase_analytics_login() {
		firebaseCurrentAnalytics.login();
	}

	public void firebase_analytics_level_up(final String name) {
		firebaseCurrentAnalytics.level_up(name);
	}

	public void firebase_analytics_post_score(final String level, final int score) {
		firebaseCurrentAnalytics.post_score(level, score);
	}

	public void firebase_analytics_select_content(final String name) {
		firebaseCurrentAnalytics.select_content(name);
	}

	public void firebase_analytics_share() {
		firebaseCurrentAnalytics.share();
	}

	public void google_initialize(final int instance_id) {
		activity.runOnUiThread(new Runnable() {
			public void run() {
				googleAchievements.init(instance_id);
				googleAuthentication.init(instance_id);
				googlePlayer.init(instance_id);
				googleLeaderboard.init(instance_id);
				googleSnapshot.init(instance_id);
			}
		});
	}

	public void facebook_initialize(final int instance_id) {
		activity.runOnUiThread(new Runnable() {
			public void run() {
				facebookAuthentication.init(instance_id);
				facebookShare.init(instance_id);
			}
		});
	}

	public void facebook_connect() {
		activity.runOnUiThread(new Runnable() {
			public void run() {
				facebookAuthentication.connect();
			}
		});
	}

	public void facebook_disconnect() {
		activity.runOnUiThread(new Runnable() {
			public void run() {
				facebookAuthentication.disconnect();
			}
		});
	}

	public boolean facebook_is_connected() {
		return facebookAuthentication.isConnected();
	}

  public void facebook_share_link(final String link, final String quote, final String hashtag, final String imageUrl) {
		activity.runOnUiThread(new Runnable() {
			public void run() {
				if (quote.length() > 0 && hashtag.length() > 0 && imageUrl.length() > 0) {
					facebookShare.share_link(link, quote, hashtag, imageUrl);
				} else if (quote.length() > 0 && hashtag.length() > 0) {
					facebookShare.share_link(link, quote, hashtag);
				} else if (quote.length() > 0) {
					facebookShare.share_link(link, quote);
				} else {
					facebookShare.share_link(link);
				}
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

	public String get_shared_directory() {
		return "/shared";
	}

	public void share(final String title, final String message, final String image_filename) {
		activity.runOnUiThread(new Runnable() {
			public void run() {
				Intent shareIntent = new Intent();

				shareIntent.setAction(Intent.ACTION_SEND);

				if (image_filename.length() > 0) {
					File imagePath = new File(context.getFilesDir(), get_shared_directory());
					File imageFile = new File(imagePath, image_filename);
					Uri contentUri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".fileprovider", imageFile);

					if (contentUri != null) {
						shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
						shareIntent.setDataAndType(contentUri, context.getContentResolver().getType(contentUri));
						shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
					} else {
						Log.w(TAG, "File not found: " + get_shared_directory() + "/" + image_filename);
					}
				}

				shareIntent.putExtra(Intent.EXTRA_TEXT, message);
				shareIntent.setType("*/*");
				activity.startActivity(Intent.createChooser(shareIntent, title));
			}
		});
	}

	protected void onMainActivityResult (int requestCode, int resultCode, Intent data) {
		// Trigger google's services
		googleAchievements.onActivityResult(requestCode, resultCode, data);
		googlePlayer.onActivityResult(requestCode, resultCode, data);
		googleAuthentication.onActivityResult(requestCode, resultCode, data);
		googleLeaderboard.onActivityResult(requestCode, resultCode, data);
		googleSnapshot.onActivityResult(requestCode, resultCode, data);

		// Trigger Facebook
		facebookAuthentication.onActivityResult(requestCode, resultCode, data);
		facebookShare.onActivityResult(requestCode, resultCode, data);

		// Trigger Firebase
		firebaseCurrentUser.onActivityResult(requestCode, resultCode, data);
		firebaseCurrentAnalytics.onActivityResult(requestCode, resultCode, data);
		firebaseCurrentInvite.onActivityResult(requestCode, resultCode, data);
		firebaseCurrentAuthentication.onActivityResult(requestCode, resultCode, data);
	}

	protected void onMainPause () {
		// Trigger google's services
		googleAchievements.onPause();
		googlePlayer.onPause();
		googleAuthentication.onPause();
		googleLeaderboard.onPause();
		googleSnapshot.onPause();

		// Trigger Facebook
		facebookAuthentication.onPause();
		facebookShare.onPause();

		// Trigger Firebase
		firebaseCurrentUser.onPause();
		firebaseCurrentAnalytics.onPause();
		firebaseCurrentInvite.onPause();
		firebaseCurrentAuthentication.onPause();
	}

	protected void onMainResume () {
		// Trigger google's services
		googleAchievements.onResume();
		googlePlayer.onResume();
		googleAuthentication.onResume();
		googleLeaderboard.onResume();
		googleSnapshot.onResume();

		// Trigger Facebook
		facebookAuthentication.onResume();
		facebookShare.onResume();

		// Trigger Firebase
		firebaseCurrentUser.onResume();
		firebaseCurrentAnalytics.onResume();
		firebaseCurrentInvite.onResume();
		firebaseCurrentAuthentication.onResume();
	}

	protected void onMainDestroy () {
		// Trigger google's services
		googleAchievements.onStop();
		googlePlayer.onStop();
		googleAuthentication.onStop();
		googleLeaderboard.onStop();
		googleSnapshot.onStop();

		// Trigger Facebook
		facebookAuthentication.onStop();
		facebookShare.onStop();

		// Trigger Firebase
		firebaseCurrentUser.onStop();
		firebaseCurrentAnalytics.onStop();
		firebaseCurrentInvite.onStop();
		firebaseCurrentAuthentication.onStop();
	}
}
