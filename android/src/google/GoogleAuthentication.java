package org.godotengine.godot.google;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;

import java.util.Arrays;
import java.util.List;
import java.io.IOException;

import org.json.JSONObject;
import org.json.JSONException;

import org.godotengine.godot.GodotLib;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.PlayersClient;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Tasks;


public class GoogleAuthentication {
	private static int instance_id;
	private static Activity activity = null;
	private static Context context = null;
	private static GoogleAuthentication mInstance = null;

	private static final int GOOGLE_SIGN_IN_REQUEST	= 9001;

	private GoogleSignInClient mGoogleSignInClient;
	private GoogleSignInAccount mAccount;

	private static final String TAG = "GoogleAuthentication";

	public static GoogleAuthentication getInstance(Activity p_activity) {
		if (mInstance == null) {
			synchronized (GoogleAuthentication.class) {
				mInstance = new GoogleAuthentication(p_activity);
			}
		}

		return mInstance;
	}

	public GoogleAuthentication(Activity p_activity) {
		activity = p_activity;

		mGoogleSignInClient = GoogleSignIn.getClient(p_activity, new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN)
		// Since we are using SavedGames, we need to add the SCOPE_APPFOLDER to access Google Drive.
		.requestScopes(Drive.SCOPE_APPFOLDER)
		.build());
  }

	public void init(final int p_instance_id) {
		this.instance_id = p_instance_id;

		onStart();
	}

  public GoogleSignInAccount get_account() {
    return mAccount;
  }

	public boolean isConnected() {
		mAccount = GoogleSignIn.getLastSignedInAccount(activity);

		return mAccount != null;
	}

	public void connect() {
		if (mGoogleSignInClient == null) {
			String message = "GoogleSignInClient is not initialized";

			Log.d(TAG, message);
			
			GodotLib.calldeferred(instance_id, "google_auth_connect_failed", new Object[] { message });
		}

		if (isConnected()) {
			Log.d(TAG, "Google service is already connected");
			
			GodotLib.calldeferred(instance_id, "google_auth_connected", new Object[] { "" });
		}

		activity.startActivityForResult(mGoogleSignInClient.getSignInIntent(), GOOGLE_SIGN_IN_REQUEST);
	}

	public void onConnected() {
		Log.d(TAG, "Google signed in, retrieving player's data");

		PlayersClient playersClient = Games.getPlayersClient(activity, mAccount);

		playersClient.getCurrentPlayer()
		.addOnSuccessListener(new OnSuccessListener<Player>() {
			@Override
			public void onSuccess(Player player) {
				GodotLib.calldeferred(instance_id, "google_auth_connected", new Object[] { player.getDisplayName() });
			}
		})
		.addOnFailureListener(new OnFailureListener() {
      @Override
      public void onFailure(@NonNull Exception e) {
        Log.d(TAG, "Failed to retrieved player's data");

				// Signed in, but failed to retrieve player's data.
				GodotLib.calldeferred(instance_id, "google_auth_connected", new Object[] { "" });
      }
		});
	}

	public void disconnect() {
		mGoogleSignInClient.signOut()
		.addOnCompleteListener(activity, new OnCompleteListener<Void>() {
			@Override
			public void onComplete(@NonNull Task<Void> task) {
				onDisconnected();
			}
		});
	}

	public void onDisconnected() {
		Log.i(TAG, "Google signed out.");

		GodotLib.calldeferred(instance_id, "google_auth_disconnected", new Object[] {});
	}

	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == GOOGLE_SIGN_IN_REQUEST) {
			Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(intent);

      try {
        mAccount = task.getResult(ApiException.class);

				onConnected();
			} catch (ApiException apiException) {
				String message = apiException.getMessage();

        if (message == null || message.isEmpty()) {
          message = apiException.toString();
        }

        GodotLib.calldeferred(instance_id, "google_auth_connect_failed", new Object[] { message });
			}
		}
	}

	private void signInSilently() {
		if (isConnected()) {
			return;
		}

		GoogleSignInClient signInClient = GoogleSignIn.getClient(activity,
		GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN);

		signInClient.silentSignIn().addOnCompleteListener(activity, new OnCompleteListener<GoogleSignInAccount>() {
			@Override
			public void onComplete(@NonNull Task<GoogleSignInAccount> task) {
				if (task.isSuccessful()) {
					// The signed in account is stored in the task's result.
					try {
						mAccount = task.getResult(ApiException.class);

						onConnected();
					} catch (ApiException e) {
						Log.w(TAG, "Failed to connect silently: " + e.getStatusCode() + ", Message: " + e.getStatusMessage());

						connect();
					}
				} else {
					// Player will need to sign-in explicitly using via UI
					Log.d(TAG, "Silent login failed, trying an explicit login");

					connect();
				}
			}
		});
	}

	public void onStart() {
		if (isConnected()) {
			onConnected();
		} else {
			Log.d(TAG, "Google not connected");

			// Try to sign in silently
			signInSilently();
		}
	}

	public void onPause() {
		// Nothing to do
	}

	public void onResume() {
		// Need to sign in again on pause/resume
		signInSilently();
	}

	public void onStop() {
		activity = null;
	}
}
