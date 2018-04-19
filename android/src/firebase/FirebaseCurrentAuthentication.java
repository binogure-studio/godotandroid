package org.godotengine.godot.firebase;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import org.godotengine.godot.GodotLib;
import org.godotengine.godot.GodotAndroidCommon;
import org.godotengine.godot.GodotAndroidRequest;

import com.godot.game.R;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class FirebaseCurrentAuthentication extends GodotAndroidCommon {

	private static final String TAG = "FirebaseCurrentAuthentication";
	private static int instance_id;
	private static Activity activity = null;
	private static FirebaseCurrentAuthentication mInstance = null;

	private FirebaseAuth mAuth;

	public static synchronized FirebaseCurrentAuthentication getInstance (Activity p_activity) {
		if (mInstance == null) {
			mInstance = new FirebaseCurrentAuthentication(p_activity);
		}

		return mInstance;
	}

	public FirebaseCurrentAuthentication(Activity p_activity) {
		activity = p_activity;

		mAuth = FirebaseAuth.getInstance();
	}

	public void init(final int p_instance_id) {
		this.instance_id = p_instance_id;

		onStart();
	}

	public void connect() {
		FirebaseUser firebaseUser = mAuth.getCurrentUser();

		if (firebaseUser != null) {
			Log.d(TAG, "Already connected to firebase");

			return;
		}

		// try to connect only once at a time
		if (updateConnectionStatus(GodotConnectStatus.CONNECTING)) {
			mAuth.signInAnonymously().addOnCompleteListener(activity, new OnCompleteListener<AuthResult>() {
				@Override
				public void onComplete(@NonNull Task<AuthResult> task) {
					if (task.isSuccessful()) {
						updateConnectionStatus(GodotConnectStatus.CONNECTED);

						Log.d(TAG, "Connected to firebase");

						GodotLib.calldeferred(instance_id, "firebase_auth_connected", new Object[]{ });
					} else {
						updateConnectionStatus(GodotConnectStatus.DISCONNECTED);

						Log.w(TAG, "Failed to connect to firebase", task.getException());

						GodotLib.calldeferred(instance_id, "firebase_auth_connect_failed", new Object[]{ task.getException().getMessage() });
					}
				}
			});
		}
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
    // Nothing to do
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
}
