package org.godotengine.godot.google;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;

import java.util.Arrays;
import java.util.List;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.nio.channels.FileChannel;

import org.json.JSONObject;
import org.json.JSONException;

import org.godotengine.godot.GodotLib;
import org.godotengine.godot.GodotAndroidRequest;
import com.google.android.gms.common.images.ImageManager;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.PlayersClient;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;

public class GooglePlayer {
	private static int instance_id;
	private static Activity activity = null;
	private static Context context = null;
	private static GooglePlayer mInstance = null;
	private Player currentPlayer = null;
	private static final String pictures_local_name = "playgame_photo.png";
	private static PlayersClient mPlayersClient = null;

	private static int script_id;
	private static final String TAG = "GooglePlayer";

	public static GooglePlayer getInstance(Activity p_activity) {
		if (mInstance == null) {
			synchronized (GooglePlayer.class) {
				mInstance = new GooglePlayer(p_activity);
			}
		}

		return mInstance;
	}

	public GooglePlayer(Activity p_activity) {
		activity = p_activity;
		context = activity.getApplicationContext();
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
			mPlayersClient = null;
		}

		return isConnected;
	}

	public void disconnected() {
		mPlayersClient = null;
	}

	protected PlayersClient get_player_client() {
		if (mPlayersClient == null) {
			GoogleAuthentication googleAuthentication = GoogleAuthentication.getInstance(activity);

			mPlayersClient = Games.getPlayersClient(activity, googleAuthentication.get_account());
		}

		return mPlayersClient;
	}

	public void load_current_player() {
		// Reset the current user
		currentPlayer = null;

		AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				PlayersClient playersClient = get_player_client();
				Task<Player> currentPlayerTask = playersClient.getCurrentPlayer();

				currentPlayerTask.addOnCompleteListener(activity, new OnCompleteListener<Player>() {
					@Override
					public void onComplete(@NonNull Task<Player> task) {
						if (task.isSuccessful()) {
							currentPlayer = task.getResult();

							copy_user_picture(currentPlayer.getIconImageUri());
						} else {
							Log.e(TAG, "Failed to load player's informations: " + task.getException());
						}
					}
				});

				return null;
			}
		};

		task.execute();
	}

	private void copy_user_picture(Uri sourceUri) {
		copy_user_picture(sourceUri, pictures_local_name);
	}

	public void copy_user_picture(Uri sourceUri, final String local_path) {
		ImageManager imageManager = ImageManager.create(context);

		imageManager.loadImage(new ImageManager.OnImageLoadedListener() {
			@Override
			public void onImageLoaded(Uri arg0, Drawable drawable, boolean arg2) {
				if (drawable != null) {
					Bitmap bitmap = ((BitmapDrawable)drawable).getBitmap();

					try {
						FileOutputStream out = context.openFileOutput(local_path, Context.MODE_PRIVATE);

						bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
						out.flush();
						out.close();
					} catch (Exception e) {
						Log.w(TAG, "Failed to copy the user's profile image: " + e);
					}
				} else {
					Log.w(TAG, "Drawable not found ...");
				}
			}
		}, sourceUri);
	}

	public JSONObject get_user_details() {
		JSONObject userDetails = new JSONObject();

		if (currentPlayer != null) {
			try {
				userDetails.put("playergame:name", currentPlayer.getDisplayName());
				userDetails.put("playergame:uid", currentPlayer.getPlayerId());
				userDetails.put("playergame:photo_path", "user://" + pictures_local_name);
			} catch (JSONException e) {
				Log.w(TAG, "Failed to load player's game informations: " + e);
			}
		}

		return userDetails;
	}
}
