package org.godotengine.godot;

import android.app.Activity;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.EnumMap;
import java.io.IOException;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class GodotAndroidCommon {
	protected int instance_id;
	protected Activity activity = null;
	protected FirebaseAuth mAuth;

	public enum GodotConnectStatus {
		INITIALIZED,
		DISCONNECTED,
		DISCONNECTING,
		CONNECTING,
		CONNECTED
	}

	public static final Map<GodotConnectStatus, GodotConnectStatus[]> GODOT_CONNECTION_TRANSITIONS;

	static {
		GODOT_CONNECTION_TRANSITIONS = new EnumMap<GodotConnectStatus, GodotConnectStatus[]>(GodotConnectStatus.class);

		GODOT_CONNECTION_TRANSITIONS.put(GodotConnectStatus.INITIALIZED, new GodotConnectStatus[] {GodotConnectStatus.DISCONNECTED, GodotConnectStatus.CONNECTING});
		GODOT_CONNECTION_TRANSITIONS.put(GodotConnectStatus.DISCONNECTED, new GodotConnectStatus[] {GodotConnectStatus.CONNECTING});
		GODOT_CONNECTION_TRANSITIONS.put(GodotConnectStatus.DISCONNECTING, new GodotConnectStatus[] {GodotConnectStatus.DISCONNECTED, GodotConnectStatus.CONNECTED});
		GODOT_CONNECTION_TRANSITIONS.put(GodotConnectStatus.CONNECTING, new GodotConnectStatus[] {GodotConnectStatus.DISCONNECTED, GodotConnectStatus.CONNECTED});
		GODOT_CONNECTION_TRANSITIONS.put(GodotConnectStatus.CONNECTED, new GodotConnectStatus[] {GodotConnectStatus.DISCONNECTING});
	};

	private GodotConnectStatus godotConnectionStatus = GodotConnectStatus.INITIALIZED;

	protected boolean updateConnectionStatus(GodotConnectStatus nextConnectionStatus) {
		return updateConnectionStatus(nextConnectionStatus, false);
	}

	public synchronized boolean updateConnectionStatus(GodotConnectStatus nextConnectionStatus, boolean flagForce) {
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

	public boolean isConnected() {
		return godotConnectionStatus == GodotConnectStatus.CONNECTED;
	}

	public boolean isConnecting() {
		return godotConnectionStatus == GodotConnectStatus.CONNECTING;
	}
}