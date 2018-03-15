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
import com.google.firebase.auth.UserInfo;

import com.godot.game.R;

import java.util.List;

public class FacebookAuthentication extends GodotAndroidCommon {

	private static final String TAG = "FacebookAuthentication";
	private static FacebookAuthentication mInstance = null;

	private CallbackManager mCallbackManager;
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

  public void onConnected() {
    if (updateConnectionStatus(GodotConnectStatus.CONNECTED)) {
      FirebaseUser firebaseUser = mAuth.getCurrentUser();

      // Already auth
      Log.d(TAG, "Connected to facebook.");

      GodotLib.calldeferred(instance_id, "facebook_auth_connected", new Object[]{ firebaseUser.getDisplayName() });
    }
  }

  public void onConnectionFailed(String message) {
    if (updateConnectionStatus(GodotConnectStatus.DISCONNECTED)) {
      Log.w(TAG, message);

      GodotLib.calldeferred(instance_id, "facebook_auth_connect_failed", new Object[]{ message });

      // Disconnect from facebook
      mLoginManager.logOut();
    }
  }

  private void firebaseAuthWithFacebook(final AccessToken accessToken) {

    Log.d(TAG, "Auth to firebase using facebook.");

    AuthCredential credential = FacebookAuthProvider.getCredential(accessToken.getToken());
    FirebaseUser firebaseUser = mAuth.getCurrentUser();
    Task<AuthResult> authResultTask;

    if (firebaseUser != null) {
      for (UserInfo userInfo : firebaseUser.getProviderData()) {
        // Already logged using facebook
        if (userInfo.getProviderId().equals(FacebookAuthProvider.PROVIDER_ID)) {
          if (userInfo.getUid().equals(accessToken.getUserId())) {
            Log.i(TAG, "Already logged in");

            onConnected();
          } else {
            onConnectionFailed("Facebook users' id don't match. (" + userInfo.getUid() + " != " + accessToken.getUserId() + ")");
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
    
    if (authResultTask != null) {
      authResultTask.addOnCompleteListener(activity, new OnCompleteListener<AuthResult>() {
        @Override
        public void onComplete(@NonNull Task<AuthResult> task) {
          if (task.isSuccessful()) {
            // Prevent to be called more than once
            onConnected();
          } else {
            onConnectionFailed(task.getException().toString());
          }
        }
      });
    } else {
      onConnected();
    }
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

      FirebaseUser firebaseUser = mAuth.getCurrentUser();

      if (firebaseUser != null) {
        firebaseUser.unlink(FacebookAuthProvider.PROVIDER_ID)
        .addOnCompleteListener(activity, new OnCompleteListener<AuthResult>() {
          @Override
          public void onComplete(@NonNull Task<AuthResult> task) {
            if (task.isSuccessful()) {
              // Auth provider unlinked from account
              updateConnectionStatus(GodotConnectStatus.DISCONNECTED);

              GodotLib.calldeferred(instance_id, "facebook_auth_disconnected", new Object[]{ });
            } else {
              String message = task.getException().getMessage();

              // If sign in fails, display a message to the user.
              Log.w(TAG, "Failed to disconnect from firebase: " + task.getException());

              updateConnectionStatus(GodotConnectStatus.CONNECTED);
              GodotLib.calldeferred(instance_id, "facebook_auth_disconnect_failed", new Object[]{ message });
            }
          }
        });
      } else {
        // Nothing else to check here
        updateConnectionStatus(GodotConnectStatus.DISCONNECTED);
        GodotLib.calldeferred(instance_id, "facebook_auth_disconnected", new Object[]{ });
      }
    }
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
    // Forward activity result to Facebook SDK
    mCallbackManager.onActivityResult(requestCode, resultCode, data);
	}

  private void refreshToken() {
    AccessToken.refreshCurrentAccessTokenAsync(new AccessToken.AccessTokenRefreshCallback() {
      @Override
      public void OnTokenRefreshed(AccessToken accessToken) {
        firebaseAuthWithFacebook(accessToken);
      }

      @Override
      public void OnTokenRefreshFailed(FacebookException exception) {
        updateConnectionStatus(GodotConnectStatus.DISCONNECTED);

        Log.d(TAG, "Failed to connect silently, " + exception.getMessage());
      }
    });
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
            // Try to refresh the facebook token
            refreshToken();
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
