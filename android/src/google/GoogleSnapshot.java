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
import com.google.android.gms.common.api.Status;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.SnapshotsClient;
import com.google.android.gms.games.snapshot.Snapshot;
import com.google.android.gms.games.snapshot.SnapshotMetadata;
import com.google.android.gms.games.snapshot.SnapshotMetadataChange;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;

public class GoogleSnapshot {
	private static int instance_id;
	private static Activity activity = null;
	private static Context context = null;
	private static GoogleSnapshot mInstance = null;
	private static int script_id;

	private static final int MAX_SNAPSHOT_RESOLVE_RETRIES = 10;
	private static final String TAG = "GoogleSnapshot";

	public static GoogleSnapshot getInstance(Activity p_activity) {
		if (mInstance == null) {
			synchronized (GoogleSnapshot.class) {
				mInstance = new GoogleSnapshot(p_activity);
			}
		}

		return mInstance;
	}

	public GoogleSnapshot(Activity p_activity) {
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

	protected SnapshotsClient get_snapshot_client() {
		GoogleAuthentication googleAuthentication = GoogleAuthentication.getInstance(activity);

		return Games.getSnapshotsClient(activity, googleAuthentication.get_account());
	}

	public void snapshot_load(final String snapshotName, final int conflictResolutionPolicy) {
		if (is_connected()) {
			SnapshotsClient snapshotsClient = get_snapshot_client();

			snapshotsClient.open(snapshotName, true, conflictResolutionPolicy)
				.addOnFailureListener(new OnFailureListener() {
					@Override
					public void onFailure(@NonNull Exception e) {
						String message = e.getMessage();

						Log.e(TAG, "Error while opening Snapshot: " + message);

						GodotLib.calldeferred(instance_id, "google_snapshot_load_failed", new Object[] { message });
					}
				})
				.continueWith(new Continuation<SnapshotsClient.DataOrConflict<Snapshot>, String>() {
					@Override
					public String then(@NonNull Task<SnapshotsClient.DataOrConflict<Snapshot>> task) throws Exception {
						SnapshotsClient.DataOrConflict<Snapshot> result = task.getResult();

						if (result.isConflict()) {
							throw new Exception("Cannot solve all conflicts.");
						}

						// Opening the snapshot was a success without any conflict.
						Snapshot snapshot = result.getData();

						// Extract the raw data from the snapshot.
						return new String(snapshot.getSnapshotContents().readFully());
					}
				})
				.addOnCompleteListener(new OnCompleteListener<String>() {
					@Override
					public void onComplete(@NonNull Task<String> task) {
						if (task.isSuccessful()) {
							String result = task.getResult();

							GodotLib.calldeferred(instance_id, "google_snapshot_loaded", new Object[] { result });
						} else {
							Log.e(TAG, "Error while loading Snapshot: " + task.getException());

							GodotLib.calldeferred(instance_id, "google_snapshot_load_failed", new Object[]{ task.getException().getMessage() });
						}
					}
			});

			Log.i(TAG, "Loading snapshot");
		} else {
			String message = "PlayGameServices: Google not connected";

			GodotLib.calldeferred(instance_id, "google_snapshot_load_failed", new Object[] { message });
		}
	}

	private Task<Snapshot> force_resolve_conflicts(SnapshotsClient.DataOrConflict<Snapshot> result,	final int retryCount) {
		// Inspired by
		// https://developers.google.com/games/services/android/savedgames
    if (!result.isConflict()) {
			// There was no conflict, so return the result of the source.
			TaskCompletionSource<Snapshot> source = new TaskCompletionSource<>();
			source.setResult(result.getData());

			return source.getTask();
		}

    // There was a conflict. Overriding all of those.
    SnapshotsClient.SnapshotConflict conflict = result.getConflict();
		SnapshotsClient snapshotsClient = get_snapshot_client();

		return snapshotsClient.resolveConflict(conflict.getConflictId(), conflict.getConflictingSnapshot())
			.continueWithTask(
				new Continuation<SnapshotsClient.DataOrConflict<Snapshot>, Task<Snapshot>>() {
					@Override
					public Task<Snapshot> then(
						@NonNull Task<SnapshotsClient.DataOrConflict<Snapshot>> task)
						throws Exception {
							// Resolving the conflict may cause another conflict,
							// so recurse and try another resolution.
							if (retryCount < MAX_SNAPSHOT_RESOLVE_RETRIES) {
								return force_resolve_conflicts(task.getResult(), retryCount + 1);
							} else {
								return null;
							}
					}
				}
			);
	}

	public void snapshot_save(final String snapshotName, final String data, final String description, final boolean flag_force) {
		if (is_connected()) {
			Log.i(TAG, "Saving " + snapshotName);

			AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
				@Override
				protected Void doInBackground(Void... params) {
					final SnapshotsClient snapshotsClient = get_snapshot_client();

					snapshotsClient.open(snapshotName, true)
						.addOnFailureListener(new OnFailureListener() {
							@Override
							public void onFailure(@NonNull Exception e) {
								String message = e.getMessage();

								Log.e(TAG, "Error while opening Snapshot: " + message);

								GodotLib.calldeferred(instance_id, "google_snapshot_save_failed", new Object[] { message });
							}
						})
						.continueWith(new Continuation<SnapshotsClient.DataOrConflict<Snapshot>, Void>() {
							@Override
							public Void then(@NonNull Task<SnapshotsClient.DataOrConflict<Snapshot>> subTask) throws Exception {
								SnapshotsClient.DataOrConflict<Snapshot> result = subTask.getResult();
								Snapshot snapshot = null;

								if (result.isConflict()) {
									if (flag_force) {
										snapshot = Tasks.await(force_resolve_conflicts(result, 0));
									} else {
										String message = "Cannot save snapshot " + snapshotName + " (set the force flag to force a conflict resolution)";
										Log.w(TAG, message);
					
										GodotLib.calldeferred(instance_id, "google_snapshot_save_failed", new Object[] { message });
					
										return null;
									}
								} else {
									snapshot = result.getData();
								}
								
								if (snapshot == null) {
									String message = "Cannot save snapshot " + snapshotName + " cannot resolve conflicts";

									Log.w(TAG, message);
					
									GodotLib.calldeferred(instance_id, "google_snapshot_save_failed", new Object[] { message });

									return null;
								}

								snapshot.getSnapshotContents().writeBytes(data.getBytes());
		
								// Update the metadata
								SnapshotMetadataChange metadataChange = new SnapshotMetadataChange.Builder().setDescription(description).build();
					
								// Handle success and failure for the commit and close.
								snapshotsClient.commitAndClose(snapshot, metadataChange).addOnCompleteListener(new OnCompleteListener<SnapshotMetadata>() {
                  @Override
                  public void onComplete(@NonNull Task<SnapshotMetadata> task) {
                    if (task.isSuccessful()) {
                      GodotLib.calldeferred(instance_id, "google_snapshot_saved", new Object[] { });
                    } else {
											String message = "Cannot save snapshot: " + task.getException();

											Log.w(TAG, task.getException());
							
											GodotLib.calldeferred(instance_id, "google_snapshot_save_failed", new Object[] { message });
                    }
                  }
								});

								return null;
							}
						});

					return null;
				}
			};
			task.execute();

		} else {
			String message = "PlayGameServices: Google not connected";

			GodotLib.calldeferred(instance_id, "google_snapshot_save_failed", new Object[] { message });
    }
	}
}
