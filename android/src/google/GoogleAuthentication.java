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

import org.json.JSONObject;
import org.json.JSONException;

import org.godotengine.godot.GodotLib;
import org.godotengine.godot.GodotAndroidCommon;
import org.godotengine.godot.GodotAndroidRequest;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.ApiException;
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

import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import com.godot.game.R;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class GoogleAuthentication extends GodotAndroidCommon {
	private static int instance_id;
	private static Activity activity = null;
	private static Context context = null;
	private static GoogleAuthentication mInstance = null;

	private FirebaseAuth mAuth;
	private GoogleApiClient mGoogleApiClient;
	private GoogleSignInAccount mAccount;
	private Bundle mBundle;

	private static final String TAG = "GoogleAuthentication";

	public static synchronized GoogleAuthentication getInstance(Activity p_activity) {
		if (mInstance == null) {
			mInstance = new GoogleAuthentication(p_activity);
		}

		return mInstance;
	}

	public GoogleAuthentication(Activity p_activity) {
		activity = p_activity;

		String webclientId = activity.getString(R.string.default_web_client_id);
		GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN)
		.requestIdToken(webclientId)
		// Since we are using SavedGames, we need to add the SCOPE_APPFOLDER to access Google Drive.
		.requestScopes(Drive.SCOPE_APPFOLDER)
		.build();

    mGoogleApiClient = new GoogleApiClient.Builder(activity)
		.addApi(Games.API)
		.addScope(Games.SCOPE_GAMES)
		.addApi(Auth.GOOGLE_SIGN_IN_API, options)
		.build();

		mAuth = FirebaseAuth.getInstance();
  }

	public void onConnected() {
		// Ensure we are not calling it twice in a row
		if (updateConnectionStatus(GodotConnectStatus.CONNECTED)) {
			FirebaseUser firebaseUser = mAuth.getCurrentUser();

			GodotLib.calldeferred(instance_id, "google_auth_connected", new Object[]{ firebaseUser.getDisplayName() });
		}
	}

	public void connect() {
		// try to connect only once at a time
		if (updateConnectionStatus(GodotConnectStatus.CONNECTING)) {
			Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);

			activity.startActivityForResult(signInIntent, GodotAndroidRequest.GOOGLE_AUTHENTICATION_REQUEST);
		}
	}

	public void onDisconnected() {
		if (updateConnectionStatus(GodotConnectStatus.DISCONNECTED)) {
			Log.i(TAG, "Google signed out.");

			GodotLib.calldeferred(instance_id, "google_auth_disconnected", new Object[] {});
		}
	}

	public void init(final int p_instance_id) {
		this.instance_id = p_instance_id;

		onStart();
	}

  public GoogleSignInAccount get_account() {
    return mAccount;
  }

	public void disconnect() {
		// try to disconnect only once at a time
		if (updateConnectionStatus(GodotConnectStatus.DISCONNECTING)) {
			// Firebase sign out
			mAuth.signOut();

			// Google sign out
			if (mGoogleApiClient.isConnected()) {
				Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(new ResultCallback<Status>() {
					@Override
					public void onResult(@NonNull Status status) {
						onDisconnected();
					}
				});
			}
		}
	}

	private void firebaseAuthWithGoogle(final GoogleSignInAccount account) {
		AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);

		mAuth.signInWithCredential(credential).addOnCompleteListener(activity, new OnCompleteListener<AuthResult>() {
			@Override
			public void onComplete(@NonNull Task<AuthResult> task) {
				if (task.isSuccessful()) {
					// Only on connection success
					mAccount = account;

					onConnected();
				} else {
					String message = task.getException().getMessage();

					// If sign in fails, display a message to the user.
					Log.w(TAG, "Failed to connect to firebase: " + task.getException());
					GodotLib.calldeferred(instance_id, "google_auth_connect_failed", new Object[] { message });

					onDisconnected();
				}
			}
		});
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == GodotAndroidRequest.GOOGLE_AUTHENTICATION_REQUEST) {
			GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);

			if (result.isSuccess()) {
        GoogleSignInAccount account = result.getSignInAccount();

				firebaseAuthWithGoogle(account);
			} else {
				onDisconnected();

				Log.w(TAG, "Failed to connect: " + result.getStatus());
        GodotLib.calldeferred(instance_id, "google_auth_connect_failed", new Object[] { result.getStatus().getStatusMessage() });
			}
		}
	}

	private void silentConnectHandler(GoogleSignInResult googleSignInResult) {
		if (updateConnectionStatus(GodotConnectStatus.DISCONNECTED)) {
			mGoogleApiClient.disconnect();

			connect();
		}
	}

	private void silentConnect() {
		if (updateConnectionStatus(GodotConnectStatus.CONNECTING)) {
			// Has to run using a background thread (not blocking the UI thread)
			AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
				@Override
				protected Void doInBackground(Void... params) {
					Log.d(TAG, "Trying to perform silent connect...");

					// Tries to connect in an async way only if the user is not already connected
					mGoogleApiClient.connect(GoogleApiClient.SIGN_IN_MODE_OPTIONAL);
				
					// If the user's cached credentials are valid, the OptionalPendingResult will be "done"
					// and the GoogleSignInResult will be available instantly.
					OptionalPendingResult<GoogleSignInResult> pendingResult = Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient);

					if (pendingResult.isDone()) {
						silentConnectHandler(pendingResult.get());
					} else {
						// There's no immediate result ready, waits for the async callback.
						pendingResult.setResultCallback(new ResultCallback<GoogleSignInResult>() {
							@Override
							public void onResult(@NonNull GoogleSignInResult result) {
								silentConnectHandler(result);
							}
						});
					}

					return null;
				}
			};

			task.execute();
		}
	}

	public void onStart() {
		silentConnect();
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
}
