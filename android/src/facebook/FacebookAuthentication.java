package org.godotengine.godot.facebook;

import android.app.Activity;
import android.content.Intent;
import android.hardware.camera2.params.Face;
import android.os.Bundle;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.godotengine.godot.GodotLib;
import org.godotengine.godot.GodotAndroidCommon;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.godot.game.R;

public class FacebookAuthentication extends GodotAndroidCommon {

	private static final String TAG = "FacebookAuthentication";

  private static int instance_id;
	private static Activity activity = null;
	private static FacebookAuthentication mInstance = null;

	private static CallbackManager mCallbackManager;
	private FirebaseAuth mAuth;
  private LoginManager mLoginManager;

	public static synchronized FacebookAuthentication getInstance (Activity p_activity) {
		if (mInstance == null) {
			mInstance = new FacebookAuthentication(p_activity);
		}

		return mInstance;
	}

	public FacebookAuthentication(Activity p_activity) {
		activity = p_activity;

    FacebookSdk.sdkInitialize(activity.getApplicationContext());

    mLoginManager = LoginManager.getInstance();
    mCallbackManager = CallbackManager.Factory.create();

    mLoginManager.registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
      @Override
      public void onSuccess(LoginResult loginResult) {
        AccessToken accessToken = loginResult.getAccessToken();

        firebaseAuthWithFacebook(accessToken);
      }

      @Override
      public void onCancel() {
        if (updateConnectionStatus(GodotConnectStatus.DISCONNECTED)) {
          GodotLib.calldeferred(instance_id, "facebook_auth_connect_cancelled", new Object[] {});
        }
      }

      @Override
      public void onError(FacebookException exception) {
        if (updateConnectionStatus(GodotConnectStatus.DISCONNECTED)) {
          GodotLib.calldeferred(instance_id, "facebook_auth_connect_failed", new Object[] { exception.toString() });
        }
      }
    });

    mAuth = FirebaseAuth.getInstance();
	}

  private void firebaseAuthWithFacebook(final AccessToken accessToken) {

    Log.d(TAG, "Auth to firebase using facebook.");

    AuthCredential credential = FacebookAuthProvider.getCredential(accessToken.getToken());
    mAuth.signInWithCredential(credential)
    .addOnCompleteListener(activity, new OnCompleteListener<AuthResult>() {
      @Override
      public void onComplete(@NonNull Task<AuthResult> task) {
        if (task.isSuccessful()) {
          // Prevent to be called more than once
          if (updateConnectionStatus(GodotConnectStatus.CONNECTED)) {
            FirebaseUser firebaseUser = mAuth.getCurrentUser();

            Log.d(TAG, "Connected to facebook.");

            GodotLib.calldeferred(instance_id, "facebook_auth_connected", new Object[]{ firebaseUser.getDisplayName() });
          }
        } else {
          if (updateConnectionStatus(GodotConnectStatus.DISCONNECTED)) {
            Log.w(TAG, task.getException());

            GodotLib.calldeferred(instance_id, "facebook_auth_connect_failed", new Object[]{ task.getException().toString() });
          }
        }
      }
    });
  }

	public void init(final int p_instance_id) {
    this.instance_id = p_instance_id;

    onStart();
  }

  public void connect() {
    if (updateConnectionStatus(GodotConnectStatus.CONNECTING)) {
      mLoginManager.logInWithPublishPermissions(activity, null);
    } else {
      Log.d(TAG, "Already connecting to facebook.");
    }
  }

	public void disconnect() {
    if (updateConnectionStatus(GodotConnectStatus.DISCONNECTING)) {
      Log.d(TAG, "Facebook signed out.");

		  mLoginManager.logOut();

      // Nothing else to check here
      updateConnectionStatus(GodotConnectStatus.DISCONNECTED);
      GodotLib.calldeferred(instance_id, "facebook_auth_disconnected", new Object[]{ });
    }
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
    // Forward activity result to Facebook SDK
    mCallbackManager.onActivityResult(requestCode, resultCode, data);
	}

	public void onStart() {
    if (updateConnectionStatus(GodotConnectStatus.CONNECTING)) {
			AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
				@Override
				protected Void doInBackground(Void... params) {
          Log.d(TAG, "Trying to connect silently");

          AccessToken accessToken = AccessToken.getCurrentAccessToken();

          if (accessToken != null && !accessToken.isExpired()) {
            firebaseAuthWithFacebook(accessToken);
          } else {
            updateConnectionStatus(GodotConnectStatus.DISCONNECTED);

            Log.d(TAG, "Failed to connect silently");
          }

					return null;
				}
			};

			task.execute();
    }
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
