package org.godotengine.godot.facebook;

import android.app.Activity;
import android.content.Intent;
import android.hardware.camera2.params.Face;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.godotengine.godot.GodotLib;

import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.share.Sharer;
import com.facebook.share.model.ShareHashtag;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.widget.ShareDialog;

import com.godot.game.R;

public class FacebookShare {

	private static final String TAG = "FacebookShare";

  private static int instance_id;
	private static Activity activity = null;
	private static FacebookShare mInstance = null;
  
  private CallbackManager mCallbackManager;
  private ShareDialog mShareDialog;


	public static synchronized FacebookShare getInstance (Activity p_activity) {
		if (mInstance == null) {
			mInstance = new FacebookShare(p_activity);
		}

		return mInstance;
	}

	public FacebookShare(Activity p_activity) {
		activity = p_activity;

    mShareDialog = new ShareDialog(activity);
    mCallbackManager = CallbackManager.Factory.create();

    mShareDialog.registerCallback(mCallbackManager, new FacebookCallback<Sharer.Result>() {
      @Override
      public void onSuccess(Sharer.Result result) {
        GodotLib.calldeferred(instance_id, "facebook_share_success", new Object[] {});
      }

      @Override
      public void onCancel() {
        GodotLib.calldeferred(instance_id, "facebook_share_cancelled", new Object[]{});
      }

      @Override
      public void onError(FacebookException error) {
        GodotLib.calldeferred(instance_id, "facebook_share_failed", new Object[]{error.toString()});
      }
    });
	}

	protected boolean is_connected() {
		FacebookAuthentication facebookAuthentication = FacebookAuthentication.getInstance(activity);

		return facebookAuthentication.isConnected();
  }

  public void share_link(final String link) {
    if (is_connected() && ShareDialog.canShow(ShareLinkContent.class)) {
      ShareLinkContent linkContent = new ShareLinkContent.Builder()
      .setContentUrl(Uri.parse(link))
      .build();

      mShareDialog.show(linkContent);
    }
  }

  public void share_link(final String link, final String quote) {
    if (is_connected() && ShareDialog.canShow(ShareLinkContent.class)) {
      ShareLinkContent linkContent = new ShareLinkContent.Builder()
      .setContentUrl(Uri.parse(link))
      .setQuote(quote)
      .build();

      mShareDialog.show(linkContent);
    }
  }

  public void share_link(final String link, final String quote, final String hashtag) {
    if (is_connected() && ShareDialog.canShow(ShareLinkContent.class)) {
      ShareHashtag shareHashtag = new ShareHashtag.Builder()
      .setHashtag(hashtag)
      .build();

      ShareLinkContent linkContent = new ShareLinkContent.Builder()
      .setContentUrl(Uri.parse(link))
      .setQuote(quote)
      .setShareHashtag(shareHashtag)
      .build();

      mShareDialog.show(linkContent);
    }
  }

	public void init(final int p_instance_id) {
    this.instance_id = p_instance_id;
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
