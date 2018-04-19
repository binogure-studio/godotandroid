package org.godotengine.godot.google;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
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
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
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
import com.google.firebase.auth.UserInfo;

public class GoogleAuthentication extends GodotAndroidCommon {
	
	private static GoogleAuthentication mInstance = null;

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

	private void disconnect_from_google() {
		// Google sign out
		if (mGoogleApiClient.isConnected()) {
			Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(new ResultCallback<Status>() {
				@Override
				public void onResult(@NonNull Status status) {
					onDisconnected();
				}
			});
		} else {
			onDisconnected();
		}
	}

	public void disconnect() {
		// try to disconnect only once at a time
		if (updateConnectionStatus(GodotConnectStatus.DISCONNECTING)) {
			FirebaseUser firebaseUser = mAuth.getCurrentUser();

			// We don't want to logout from firebase, we ant to logout from google.
      if (firebaseUser != null) {
        firebaseUser.unlink(GoogleAuthProvider.PROVIDER_ID)
        .addOnCompleteListener(activity, new OnCompleteListener<AuthResult>() {
          @Override
          public void onComplete(@NonNull Task<AuthResult> task) {
            if (task.isSuccessful()) {
              // Auth provider unlinked from account
							disconnect_from_google();
            } else {
              String message = task.getException().getMessage();

              // If sign in fails, display a message to the user.
              Log.w(TAG, "Failed to disconnect from firebase: " + task.getException());

              updateConnectionStatus(GodotConnectStatus.CONNECTED);
              GodotLib.calldeferred(instance_id, "google_auth_disconnect_failed", new Object[]{ message });
            }
          }
        });
      } else {
        // Nothing else to check here
        disconnect_from_google();
      }
		}
	}

	private void firebaseAuthWithGoogle(final GoogleSignInAccount account) {
		AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
    FirebaseUser firebaseUser = mAuth.getCurrentUser();
    Task<AuthResult> authResultTask;

    if (firebaseUser != null) {
      for (UserInfo userInfo : firebaseUser.getProviderData()) {
        // Already logged using google
        if (userInfo.getProviderId().equals(GoogleAuthProvider.PROVIDER_ID)) {
          if (userInfo.getUid().equals(account.getId())) {
						Log.i(TAG, "Already logged in");

						mAccount = account;
            onConnected();
          } else {
						String message = "Failed to connect to firebase: users' id don't match. (" + userInfo.getUid() + " != " + account.getId() + ")";

						// If sign in fails, display a message to the user.
						Log.w(TAG, message);
						GodotLib.calldeferred(instance_id, "google_auth_connect_failed", new Object[] { message });

            updateConnectionStatus(GodotConnectStatus.DISCONNECTED);
          }

          return;
        }
      }

      // Link account
      authResultTask = firebaseUser.linkWithCredential(credential);
    } else {
      // SignIn
      authResultTask = mAuth.signInWithCredential(credential);
    }

		authResultTask.addOnCompleteListener(activity, new OnCompleteListener<AuthResult>() {
			@Override
			public void onComplete(@NonNull Task<AuthResult> task) {
				if (task.isSuccessful()) {
					mAccount = account;

					onConnected();
				} else {
					String message = task.getException().getMessage();

					// If sign in fails, display a message to the user.
					Log.w(TAG, "Failed to connect to firebase: " + task.getException());
					GodotLib.calldeferred(instance_id, "google_auth_connect_failed", new Object[] { message });

					updateConnectionStatus(GodotConnectStatus.DISCONNECTED);
				}
			}
		});
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == GodotAndroidRequest.GOOGLE_AUTHENTICATION_REQUEST) {
			GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
			
			silentConnectHandler(result);
		}
	}

	private void silentConnectHandler(GoogleSignInResult result) {
		if (result != null && result.isSuccess()) {
			GoogleSignInAccount account = result.getSignInAccount();

			firebaseAuthWithGoogle(account);
		} else if (result.getStatus().getStatusCode() == GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
			Log.i(TAG, "Failed to connect: " + result.getStatus());

			onDisconnected();
		} else {
			updateConnectionStatus(GodotConnectStatus.DISCONNECTED);

			Log.w(TAG, "Failed to connect: " + result.getStatus());
			GodotLib.calldeferred(instance_id, "google_auth_connect_failed", new Object[] { result.getStatus().getStatusMessage() });
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
