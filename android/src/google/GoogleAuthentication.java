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
import java.util.Map;
import java.util.EnumMap;
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

public class GoogleAuthentication {
	private static int instance_id;
	private static Activity activity = null;
	private static Context context = null;
	private static GoogleAuthentication mInstance = null;
	private static final int GOOGLE_SIGN_IN_REQUEST	= 9001;

	private enum GodotConnectStatus {
		INITIALIZED,
		DISCONNECTED,
		DISCONNECTING,
		CONNECTING,
		CONNECTED
	}

	private static final Map<GodotConnectStatus, GodotConnectStatus[]> GODOT_CONNECTION_TRANSITIONS;
	private static GodotConnectStatus godotConnectionStatus = GodotConnectStatus.INITIALIZED;

	static {
		GODOT_CONNECTION_TRANSITIONS = new EnumMap<GodotConnectStatus, GodotConnectStatus[]>(GodotConnectStatus.class);

		GODOT_CONNECTION_TRANSITIONS.put(GodotConnectStatus.INITIALIZED, new GodotConnectStatus[] {GodotConnectStatus.DISCONNECTED, GodotConnectStatus.CONNECTING});
		GODOT_CONNECTION_TRANSITIONS.put(GodotConnectStatus.DISCONNECTED, new GodotConnectStatus[] {GodotConnectStatus.CONNECTING});
		GODOT_CONNECTION_TRANSITIONS.put(GodotConnectStatus.DISCONNECTING, new GodotConnectStatus[] {GodotConnectStatus.DISCONNECTED, GodotConnectStatus.CONNECTED});
		GODOT_CONNECTION_TRANSITIONS.put(GodotConnectStatus.CONNECTING, new GodotConnectStatus[] {GodotConnectStatus.DISCONNECTED, GodotConnectStatus.CONNECTED});
		GODOT_CONNECTION_TRANSITIONS.put(GodotConnectStatus.CONNECTED, new GodotConnectStatus[] {GodotConnectStatus.DISCONNECTING});
	};

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

	private boolean updateConnectionStatus(GodotConnectStatus nextConnectionStatus) {
		return updateConnectionStatus(nextConnectionStatus, false);
	}

	private synchronized boolean updateConnectionStatus(GodotConnectStatus nextConnectionStatus, boolean flagForce) {
		boolean connectionStatusUpdated = flagForce;
		GodotConnectStatus[] validTransitions = GODOT_CONNECTION_TRANSITIONS.get(godotConnectionStatus);

		if (!connectionStatusUpdated) {
			for (GodotConnectStatus validTransition : validTransitions) {
				if (validTransition == nextConnectionStatus) {
					connectionStatusUpdated = true;
					godotConnectionStatus = nextConnectionStatus;

					break;
				}
			}
		} else {
			godotConnectionStatus = nextConnectionStatus;
		}

		return connectionStatusUpdated;
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

	public boolean isConnected() {
		return godotConnectionStatus == GodotConnectStatus.CONNECTED;
	}

	public boolean isConnecting() {
		return godotConnectionStatus == GodotConnectStatus.CONNECTING;
	}

	public void onConnected() {
		if (updateConnectionStatus(GodotConnectStatus.CONNECTED)) {

			// Ensure we are not calling it twice in a row
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
	}

	public void connect() {
		// try to connect only once at a time
		if (updateConnectionStatus(GodotConnectStatus.CONNECTING)) {
			Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);

			activity.startActivityForResult(signInIntent, GOOGLE_SIGN_IN_REQUEST);
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
		if (requestCode == GOOGLE_SIGN_IN_REQUEST) {
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
		// We have to disconnect from here, in order to be able to connect with firebase
		mGoogleApiClient.disconnect();

		if (googleSignInResult != null) {
			GoogleSignInAccount account = googleSignInResult.getSignInAccount();

			if (account != null) {
				firebaseAuthWithGoogle(account);
			} else if (updateConnectionStatus(GodotConnectStatus.DISCONNECTED)) {
				Log.d(TAG, "Silent connection failed, need an explicit connection");

				// Need a fresh new authentication
				connect();
			}
		} else if (updateConnectionStatus(GodotConnectStatus.DISCONNECTED)) {
			Log.d(TAG, "Silent connection failed, need an explicit connection");

			// Need a fresh new authentication
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
