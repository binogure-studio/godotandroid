package org.godotengine.godot;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.IntentSender.SendIntentException;
import android.net.Uri;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.View;
import android.os.Bundle;

import com.godot.game.BuildConfig;

import org.godotengine.godot.GodotLib;
import org.godotengine.godot.GodotAndroidRequest;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;

import org.json.JSONObject;
import org.json.JSONException;

import org.godotengine.godot.Dictionary;


public class GodotAndroidShare {
	private static final String TAG = "GodotAndroidShare";

	private static int instance_id;
	private static Context context;
	private static Activity activity = null;
	private static GodotAndroidShare mInstance = null;

	public static synchronized GodotAndroidShare getInstance (Activity p_activity) {
		if (mInstance == null) {
			mInstance = new GodotAndroidShare(p_activity);
		}

		return mInstance;
	}

	public GodotAndroidShare(Activity p_activity) {
		activity = p_activity;
		context = activity.getApplicationContext();
	}

	public void init(final int p_instance_id) {
		this.instance_id = p_instance_id;

		onStart();
	}

	public String get_shared_directory() {
		return "/shared";
	}

	public void share(final String title, final String message, final String image_filename, final String package_name) {
		Intent shareIntent = new Intent();
		String type = "text/plain";

		shareIntent.setAction(Intent.ACTION_SEND);

		if (image_filename.length() > 0) {
			File imagePath = new File(context.getFilesDir(), get_shared_directory());
			File imageFile = new File(imagePath, image_filename);
			Uri contentUri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".fileprovider", imageFile);

			if (contentUri != null) {
				shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
				shareIntent.setDataAndType(contentUri, context.getContentResolver().getType(contentUri));
				shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);

				type = "*/*";
			} else {
				Log.w(TAG, "File not found: " + get_shared_directory() + "/" + image_filename);
			}
		}

		if (package_name != null) {
			shareIntent.setPackage(package_name);
		}

		shareIntent.putExtra(Intent.EXTRA_TEXT, message);
		shareIntent.setType(type);

		activity.startActivityForResult(Intent.createChooser(shareIntent, title), GodotAndroidRequest.GODOT_SHARE_REQUEST);
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == GodotAndroidRequest.GODOT_SHARE_REQUEST) {
			// We cannot say if the user has shared a message or anything thanks to android :-/
			GodotLib.calldeferred(instance_id, "godot_share_success", new Object[] { });
		}
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