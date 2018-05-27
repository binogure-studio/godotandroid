
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
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;

import com.godot.game.BuildConfig;
import com.google.android.gms.games.SnapshotsClient;
import com.google.android.gms.games.leaderboard.LeaderboardVariant;

import org.godotengine.godot.GodotLib;
import org.godotengine.godot.GodotAndroidRequest;

import java.io.File;
import java.util.Locale;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;

import org.json.JSONObject;
import org.json.JSONException;

import org.godotengine.godot.Dictionary;

// Import godot utils
import org.godotengine.godot.GodotAndroidShare;
import org.godotengine.godot.GodotAndroidNetwork;

// Import google play service
import org.godotengine.godot.google.GoogleAchievements;
import org.godotengine.godot.google.GoogleAuthentication;
import org.godotengine.godot.google.GooglePlayer;
import org.godotengine.godot.google.GoogleLeaderboard;
import org.godotengine.godot.google.GoogleSnapshot;

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

	private GodotAndroidShare godotAndroidShare;
	private GodotAndroidNetwork godotAndroidNetwork;

	private GoogleAchievements googleAchievements;
	private GoogleAuthentication googleAuthentication;
	private GooglePlayer googlePlayer;
	private GoogleLeaderboard googleLeaderboard;
	private GoogleSnapshot googleSnapshot;
		
	private FirebaseCurrentUser firebaseCurrentUser;
	private FirebaseCurrentAnalytics firebaseCurrentAnalytics;
	private FirebaseCurrentInvite firebaseCurrentInvite;
	private FirebaseCurrentAuthentication firebaseCurrentAuthentication;

	public static final Dictionary GOOGLE_LEADERBOARD_TIMESPAN;

	private boolean google_initialized = false;
	private boolean firebase_initialized = false;
	private boolean godot_generic_initialized = false;

	static {
		GOOGLE_LEADERBOARD_TIMESPAN = new Dictionary();

		GOOGLE_LEADERBOARD_TIMESPAN.put("TIME_SPAN_WEEKLY", Integer.valueOf(LeaderboardVariant.TIME_SPAN_WEEKLY));
		GOOGLE_LEADERBOARD_TIMESPAN.put("TIME_SPAN_ALL_TIME", Integer.valueOf(LeaderboardVariant.TIME_SPAN_ALL_TIME));
		GOOGLE_LEADERBOARD_TIMESPAN.put("TIME_SPAN_DAILY", Integer.valueOf(LeaderboardVariant.TIME_SPAN_DAILY));
	};

	public static final Dictionary GOOGLE_SNAPSHOT_RESOLUTION_POLICIES;

	static {
		GOOGLE_SNAPSHOT_RESOLUTION_POLICIES = new Dictionary();

		GOOGLE_SNAPSHOT_RESOLUTION_POLICIES.put("RESOLUTION_POLICY_HIGHEST_PROGRESS", Integer.valueOf(SnapshotsClient.RESOLUTION_POLICY_HIGHEST_PROGRESS));
		GOOGLE_SNAPSHOT_RESOLUTION_POLICIES.put("RESOLUTION_POLICY_LAST_KNOWN_GOOD", Integer.valueOf(SnapshotsClient.RESOLUTION_POLICY_LAST_KNOWN_GOOD));
		GOOGLE_SNAPSHOT_RESOLUTION_POLICIES.put("RESOLUTION_POLICY_LONGEST_PLAYTIME", Integer.valueOf(SnapshotsClient.RESOLUTION_POLICY_LONGEST_PLAYTIME));
		GOOGLE_SNAPSHOT_RESOLUTION_POLICIES.put("RESOLUTION_POLICY_MANUAL", Integer.valueOf(SnapshotsClient.RESOLUTION_POLICY_MANUAL));
		GOOGLE_SNAPSHOT_RESOLUTION_POLICIES.put("RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED", Integer.valueOf(SnapshotsClient.RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED));
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
			"google_leaderboard_submit", "google_leaderboard_show", "google_leaderboard_showlist", "get_google_leaderboard_timespan",
			"google_leaderboard_load_top_scores", "google_leaderboard_load_player_centered_scores", "google_leaderboard_load_player_score", 

			// GoogleSnapshot
			"google_snapshot_load", "google_snapshot_save", "get_google_resolution_policies",

			// GoogleAchievements
			"google_achievement_unlock", "google_achievement_increment", "google_achievement_show_list",

			// Firebase
			"firebase_initialize",

			// Firebase analytics
			"firebase_analytics_status",

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
			"godot_initialize", "godot_share", "godot_get_shared_directory",

			// Network
			"godot_is_online", "godot_is_wifi_connected", "godot_is_mobile_connected", "godot_get_country_code_iso"
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

		godotAndroidShare = GodotAndroidShare.getInstance(activity);
		godotAndroidNetwork = GodotAndroidNetwork.getInstance(activity);
	}

	public Dictionary get_google_resolution_policies() {
		return GOOGLE_SNAPSHOT_RESOLUTION_POLICIES;
	}

	public Dictionary get_google_leaderboard_timespan() {
		return GOOGLE_LEADERBOARD_TIMESPAN;
	}

	public void godot_initialize(final int instance_id) {
		AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				godotAndroidShare.init(instance_id);
				godotAndroidNetwork.init(instance_id);

				godot_generic_initialized = true;

				GodotLib.calldeferred(instance_id, "godot_android_initialized", new Object[] { });
				return null;
			}
		};

		task.execute();
	}

	public void firebase_initialize(final int instance_id) {
		AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				firebaseCurrentUser.init(instance_id);
				firebaseCurrentInvite.init(instance_id);
				firebaseCurrentAnalytics.init(instance_id);
				firebaseCurrentAuthentication.init(instance_id);

				// Static class
				FirebaseCurrentNotification.init(instance_id);

				firebase_initialized = true;

				GodotLib.calldeferred(instance_id, "firebase_initialized", new Object[] { });

				return null;
			}
		};

		task.execute();
	}

	public void firebase_analytics_status(final boolean status) {
		firebaseCurrentAnalytics.set_analytics_status(status);
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
		AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				googleAchievements.init(instance_id);
				googleAuthentication.init(instance_id);
				googlePlayer.init(instance_id);
				googleLeaderboard.init(instance_id);
				googleSnapshot.init(instance_id);

				google_initialized = true;

				GodotLib.calldeferred(instance_id, "google_initialized", new Object[] { });

				return null;
			}
		};

		task.execute();
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
	public void google_leaderboard_load_player_score(final String leaderboard_id, final int time_span) {
		googleLeaderboard.leaderboard_load_player_score(leaderboard_id, time_span);
	}

	public void google_leaderboard_load_top_scores(final String leaderboard_id, final int time_span, final int max_results, final boolean force_reload) {
		googleLeaderboard.leaderboard_load_top_scores(leaderboard_id, time_span, max_results, force_reload);
	}

	public void google_leaderboard_load_player_centered_scores(final String leaderboard_id, final int time_span, final int max_results, final boolean force_reload) {
		googleLeaderboard.leaderboard_load_player_centered_scores(leaderboard_id, time_span, max_results, force_reload);
	}

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

	public String godot_get_shared_directory() {
		return godotAndroidShare.get_shared_directory();
	}

	public void godot_share(final String title, final String message, final String image_filename) {
		activity.runOnUiThread(new Runnable() {
			public void run() {
				godotAndroidShare.share(title, message, image_filename);
			}
		});
	}

	public boolean godot_is_online() {
		return godotAndroidNetwork.isOnline();
	}

	public boolean godot_is_wifi_connected() {
		return godotAndroidNetwork.isWifiConnected();
	}

	public boolean godot_is_mobile_connected() {
		return godotAndroidNetwork.isMobileConnected();
	}

	public String godot_get_country_code_iso() {
		try {
			final TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
			final String simCountry = telephonyManager.getSimCountryIso();

			// SIM country code is available
			if (simCountry != null && simCountry.length() == 2) {
				return simCountry.toLowerCase(Locale.US);
			} else if (telephonyManager.getPhoneType() != TelephonyManager.PHONE_TYPE_CDMA) {
				// device is not 3G (would be unreliable)
				String networkCountry = telephonyManager.getNetworkCountryIso();

				// network country code is available
				if (networkCountry != null && networkCountry.length() == 2) {
					return networkCountry.toLowerCase(Locale.US);
				}
			}
		} catch (Exception ex) {
			Log.i(TAG, "Cannot determine the country code: " + ex.getMessage());
		}

		return "NO_COUNTRY_CODE";
	}

	protected void onMainActivityResult (int requestCode, int resultCode, Intent data) {
		if (google_initialized) {
			// Trigger google's services
			googleAchievements.onActivityResult(requestCode, resultCode, data);
			googlePlayer.onActivityResult(requestCode, resultCode, data);
			googleAuthentication.onActivityResult(requestCode, resultCode, data);
			googleLeaderboard.onActivityResult(requestCode, resultCode, data);
			googleSnapshot.onActivityResult(requestCode, resultCode, data);
		}

		if (firebase_initialized) {
			// Trigger Firebase
			firebaseCurrentUser.onActivityResult(requestCode, resultCode, data);
			firebaseCurrentAnalytics.onActivityResult(requestCode, resultCode, data);
			firebaseCurrentInvite.onActivityResult(requestCode, resultCode, data);
			firebaseCurrentAuthentication.onActivityResult(requestCode, resultCode, data);
		}

		if (godot_generic_initialized) {
			// Trigger Godot Utils
			godotAndroidShare.onActivityResult(requestCode, resultCode, data);
			godotAndroidNetwork.onActivityResult(requestCode, resultCode, data);
		}
	}

	protected void onMainPause () {
		if (google_initialized) {
			// Trigger google's services
			googleAchievements.onPause();
			googlePlayer.onPause();
			googleAuthentication.onPause();
			googleLeaderboard.onPause();
			googleSnapshot.onPause();
		}

		if (firebase_initialized) {
			// Trigger Firebase
			firebaseCurrentUser.onPause();
			firebaseCurrentAnalytics.onPause();
			firebaseCurrentInvite.onPause();
			firebaseCurrentAuthentication.onPause();
		}

		if (godot_generic_initialized) {
			// Trigger Godot Utils
			godotAndroidShare.onPause();
			godotAndroidNetwork.onPause();
		}
	}

	protected void onMainResume () {
		if (google_initialized) {
			// Trigger google's services
			googleAchievements.onResume();
			googlePlayer.onResume();
			googleAuthentication.onResume();
			googleLeaderboard.onResume();
			googleSnapshot.onResume();
		}

		if (firebase_initialized) {
			// Trigger Firebase
			firebaseCurrentUser.onResume();
			firebaseCurrentAnalytics.onResume();
			firebaseCurrentInvite.onResume();
			firebaseCurrentAuthentication.onResume();
		}

		if (godot_generic_initialized) {
			// Trigger Godot Utils
			godotAndroidShare.onResume();
			godotAndroidNetwork.onResume();
		}
	}

	protected void onMainDestroy () {
		if (google_initialized) {
			// Trigger google's services
			googleAchievements.onStop();
			googlePlayer.onStop();
			googleAuthentication.onStop();
			googleLeaderboard.onStop();
			googleSnapshot.onStop();
		}

		if (firebase_initialized) {
			// Trigger Firebase
			firebaseCurrentUser.onStop();
			firebaseCurrentAnalytics.onStop();
			firebaseCurrentInvite.onStop();
			firebaseCurrentAuthentication.onStop();
		}

		if (godot_generic_initialized) {
			// Trigger Godot Utils
			godotAndroidShare.onStop();
			godotAndroidNetwork.onStop();
		}
	}
}
