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
import java.util.Iterator;
import java.io.IOException;

import org.json.JSONObject;
import org.json.JSONException;

import org.godotengine.godot.GodotLib;
import org.godotengine.godot.GodotAndroidRequest;
import org.godotengine.godot.google.GooglePlayer;

import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.games.AchievementsClient;
import com.google.android.gms.games.AnnotatedData;
import com.google.android.gms.games.Games;

import com.google.android.gms.games.LeaderboardsClient;
import com.google.android.gms.games.LeaderboardsClient.LeaderboardScores;
import com.google.android.gms.games.leaderboard.LeaderboardScore;
import com.google.android.gms.games.leaderboard.LeaderboardScoreBuffer;
import com.google.android.gms.games.leaderboard.LeaderboardVariant;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;

public class GoogleLeaderboard {
	private static final int MAX_RESULT = 10;
	private static int instance_id;
	private static Activity activity = null;
	private static Context context = null;
	private static GoogleLeaderboard mInstance = null;
	private static LeaderboardsClient mLeaderboardsClient = null;

	private static int script_id;
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
		boolean isConnected = googleAuthentication.isConnected();

		if (!isConnected) {
			// Reset the client
			mLeaderboardsClient = null;
		}

		return isConnected;
	}

	protected LeaderboardsClient get_leaderboard_client() {
		if (mLeaderboardsClient == null) {
			GoogleAuthentication googleAuthentication = GoogleAuthentication.getInstance(activity);

			mLeaderboardsClient = Games.getLeaderboardsClient(activity, googleAuthentication.get_account());
		}

		return mLeaderboardsClient;
	}

	public void disconnected() {
		mLeaderboardsClient = null;
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
					activity.startActivityForResult(intent, GodotAndroidRequest.GOOGLE_LEADERBOARD_REQUEST);

					GodotLib.calldeferred(instance_id, "google_leaderboard_showd", new Object[] { l_id });
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
					activity.startActivityForResult(intent, GodotAndroidRequest.GOOGLE_LEADERBOARD_REQUEST);

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

	public void leaderboard_load_player_score(final String leaderboard_id) {
		leaderboard_load_player_score(leaderboard_id, LeaderboardVariant.TIME_SPAN_ALL_TIME);
	}

	public void leaderboard_load_player_score(final String leaderboard_id, final int time_span) {
		if (is_connected()) {
			AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
				@Override
				protected Void doInBackground(Void... params) {
					LeaderboardsClient leaderboardsClient = get_leaderboard_client();

					// Forcing LeaderboardVariant.COLLECTION_PUBLIC value because others values are deprecated.
					// See: https://developers.google.com/android/reference/com/google/android/gms/games/leaderboard/LeaderboardVariant#COLLECTION_SOCIAL
					leaderboardsClient.loadCurrentPlayerLeaderboardScore(leaderboard_id, time_span, LeaderboardVariant.COLLECTION_PUBLIC)
						.addOnFailureListener(new OnFailureListener() {
							@Override
							public void onFailure(@NonNull Exception e) {
								String message = e.getMessage();

								Log.e(TAG, "Error while loading score: " + message);

								GodotLib.calldeferred(instance_id, "google_leaderboard_load_score_failed", new Object[] { message });
							}
						})
						.addOnCompleteListener(new OnCompleteListener<AnnotatedData<LeaderboardScore>>() {
							@Override
							public void onComplete(@NonNull Task<AnnotatedData<LeaderboardScore>> task) {
								if (task.isSuccessful()) {
									AnnotatedData<LeaderboardScore> result = task.getResult();
									LeaderboardScore leaderboardScore = result.get();
									long score = -1;
									long rank = -1;

									if (leaderboardScore != null) {
										score = leaderboardScore.getRawScore();
										rank = leaderboardScore.getRank();
									}

									GodotLib.calldeferred(instance_id, "google_leaderboard_loaded_score", new Object[] { String.valueOf(score), String.valueOf(rank) });
								} else {
									Log.e(TAG, "Error while loading score: " + task.getException());

									GodotLib.calldeferred(instance_id, "google_leaderboard_load_score_failed", new Object[]{ task.getException().getMessage() });
								}
							}
					});

					return null;
				}
			};

			task.execute();

			Log.i(TAG, "Loading user's score");
		} else {
			String message = "PlayGameServices: Google not connected";

			GodotLib.calldeferred(instance_id, "google_leaderboard_load_score_failed", new Object[] { message });
		}
	}

	public void leaderboard_load_player_centered_scores(final String leaderboard_id) {
		leaderboard_load_player_centered_scores(leaderboard_id, LeaderboardVariant.TIME_SPAN_ALL_TIME, MAX_RESULT, false);
	}

	public void leaderboard_load_player_centered_scores(final String leaderboard_id, final int time_span) {
		leaderboard_load_player_centered_scores(leaderboard_id, time_span, MAX_RESULT, false);
	}

	public void leaderboard_load_player_centered_scores(final String leaderboard_id, final int time_span, final int max_results) {
		leaderboard_load_player_centered_scores(leaderboard_id, time_span, max_results, false);
	}

	public void leaderboard_load_player_centered_scores(final String leaderboard_id, final int time_span, final int max_results, final boolean force_reload) {
		if (is_connected()) {
			AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
				@Override
				protected Void doInBackground(Void... params) {
					LeaderboardsClient leaderboardsClient = get_leaderboard_client();

					// Forcing LeaderboardVariant.COLLECTION_PUBLIC value because others values are deprecated.
					// See: https://developers.google.com/android/reference/com/google/android/gms/games/leaderboard/LeaderboardVariant#COLLECTION_SOCIAL
					leaderboardsClient.loadPlayerCenteredScores(leaderboard_id, time_span, LeaderboardVariant.COLLECTION_PUBLIC, max_results, force_reload)
						.addOnFailureListener(new OnFailureListener() {
							@Override
							public void onFailure(@NonNull Exception e) {
								String message = e.getMessage();

								Log.e(TAG, "Error while loading centered scores: " + message);

								GodotLib.calldeferred(instance_id, "google_leaderboard_loaded_centered_scores", new Object[] { message });
							}
						})
						.addOnCompleteListener(new OnCompleteListener<AnnotatedData<LeaderboardScores>>() {
							@Override
							public void onComplete(@NonNull Task<AnnotatedData<LeaderboardScores>> task) {
								if (task.isSuccessful()) {
									JSONObject leaderboard_result = new JSONObject();
									AnnotatedData<LeaderboardScores> result = task.getResult();
									LeaderboardScoreBuffer leaderboardScores = result.get().getScores();
									GooglePlayer googlePlayer = GooglePlayer.getInstance(activity);

									try {
										for (Iterator<LeaderboardScore> iterator = leaderboardScores.iterator(); iterator.hasNext(); ) {
											LeaderboardScore leaderboardScore = iterator.next();
											String displayName = leaderboardScore.getScoreHolderDisplayName();

											leaderboard_result.put(String.valueOf(leaderboardScore.getRank()) + ":score", leaderboardScore.getRawScore());
											leaderboard_result.put(String.valueOf(leaderboardScore.getRank()) + ":name", displayName);
											leaderboard_result.put(String.valueOf(leaderboardScore.getRank()) + ":photo_path", "user://" + displayName + ".png");

											googlePlayer.copy_user_picture(leaderboardScore.getScoreHolderIconImageUri(), displayName + ".png");
										}
									} catch (JSONException e) {
										Log.w(TAG, "Failed to load player's game informations: " + e);
									}

									GodotLib.calldeferred(instance_id, "google_leaderboard_loaded_centered_scores", new Object[] { leaderboard_result.toString() });
								} else {
									Log.e(TAG, "Error while loading centered scores: " + task.getException());

									GodotLib.calldeferred(instance_id, "google_leaderboard_load_centered_scores_failed", new Object[]{ task.getException().getMessage() });
								}
							}
					});

					return null;
				}
			};

			task.execute();
			Log.i(TAG, "Loading scores centered on user");
		} else {
			String message = "PlayGameServices: Google not connected";

			GodotLib.calldeferred(instance_id, "google_leaderboard_load_centered_scores_failed", new Object[] { message });
		}
	}

	public void leaderboard_load_top_scores(final String leaderboard_id) {
		leaderboard_load_top_scores(leaderboard_id, LeaderboardVariant.TIME_SPAN_ALL_TIME, MAX_RESULT, false);
	}

	public void leaderboard_load_top_scores(final String leaderboard_id, final int time_span) {
		leaderboard_load_top_scores(leaderboard_id, time_span, MAX_RESULT, false);
	}

	public void leaderboard_load_top_scores(final String leaderboard_id, final int time_span, final int max_results) {
		leaderboard_load_top_scores(leaderboard_id, time_span, max_results, false);
	}

	public void leaderboard_load_top_scores(final String leaderboard_id, final int time_span, final int max_results, final boolean force_reload) {
		if (is_connected()) {
			AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
				@Override
				protected Void doInBackground(Void... params) {
					LeaderboardsClient leaderboardsClient = get_leaderboard_client();

					// Forcing LeaderboardVariant.COLLECTION_PUBLIC value because others values are deprecated.
					// See: https://developers.google.com/android/reference/com/google/android/gms/games/leaderboard/LeaderboardVariant#COLLECTION_SOCIAL
					leaderboardsClient.loadTopScores(leaderboard_id, time_span, LeaderboardVariant.COLLECTION_PUBLIC, max_results, force_reload)
						.addOnFailureListener(new OnFailureListener() {
							@Override
							public void onFailure(@NonNull Exception e) {
								String message = e.getMessage();

								Log.e(TAG, "Error while loading top scores: " + message);

								GodotLib.calldeferred(instance_id, "google_leaderboard_load_top_scores_failed", new Object[] { message });
							}
						})
						.addOnCompleteListener(new OnCompleteListener<AnnotatedData<LeaderboardScores>>() {
							@Override
							public void onComplete(@NonNull Task<AnnotatedData<LeaderboardScores>> task) {
								if (task.isSuccessful()) {
									JSONObject leaderboard_result = new JSONObject();
									AnnotatedData<LeaderboardScores> result = task.getResult();
									LeaderboardScoreBuffer leaderboardScores = result.get().getScores();
									GooglePlayer googlePlayer = GooglePlayer.getInstance(activity);

									try {
										for (Iterator<LeaderboardScore> iterator = leaderboardScores.iterator(); iterator.hasNext(); ) {
											LeaderboardScore leaderboardScore = iterator.next();
											String displayName = leaderboardScore.getScoreHolderDisplayName();

											leaderboard_result.put(String.valueOf(leaderboardScore.getRank()) + ":score", leaderboardScore.getRawScore());
											leaderboard_result.put(String.valueOf(leaderboardScore.getRank()) + ":name", displayName);
											leaderboard_result.put(String.valueOf(leaderboardScore.getRank()) + ":photo_path", "user://" + displayName + ".png");

											googlePlayer.copy_user_picture(leaderboardScore.getScoreHolderIconImageUri(), displayName + ".png");
										}
									} catch (JSONException e) {
										Log.w(TAG, "Failed to load player's game informations: " + e);
									}

									GodotLib.calldeferred(instance_id, "google_leaderboard_loaded_top_score", new Object[] { leaderboard_result.toString() });
								} else {
									Log.e(TAG, "Error while loading top scores: " + task.getException());

									GodotLib.calldeferred(instance_id, "google_leaderboard_load_top_scores_failed", new Object[]{ task.getException().getMessage() });
								}
							}
					});

					return null;
				}
			};

			task.execute();
			Log.i(TAG, "Loading top scores");
		} else {
			String message = "PlayGameServices: Google not connected";

			GodotLib.calldeferred(instance_id, "google_leaderboard_load_top_scores_failed", new Object[] { message });
		}
	}
}
