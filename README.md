# godot-android

Android services for Godot Engine 2.1, including:

* Google play game service
   * Authentication
   * Leaderboards
   * Achievements
   * Snapshots
* Facebook
   * Authentication
   * Share
* Firebase
   * User details
   * Analytics
   * Invite
   * Notification **WIP**


# Usage

## Preparing godot-android module

### Google play game service/facebook configurations

Open `res/values/ids.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>

<resources>
	<string name="facebook_app_id">[YOUR FACEBOOK APP ID HERE]</string>
	<string name="google_play_app_id">[YOUR GOOGLE PLAY GAME APP ID HERE]</string>
</resources>
```

### Firebase configuration

Copy `google-services.json` file into `godot/platform/android/java/`.

#### Note:

I strongly recommand you to have a `google-services.java` for debug purpose and another one for your release. (put them respectivly in `godot/platform/android/java/src/debug` and `godot/platform/android/java/src/release`, create the directories if needed).

### Compiling android's templates (Linux)

[Prerequisites documentation](http://docs.godotengine.org/en/2.1/development/compiling/compiling_for_android.html)

```sh
export CXX=g++
export CC=gcc

# This one is optional
export SCRIPT_AES256_ENCRYPTION_KEY=YOUR_ENCRYPTION_KEY

# Place where the NDK/SDK are
export ANDROID_HOME=/usr/lib/android-sdk
export ANDROID_NDK_ROOT=/usr/lib/android-sdk/ndk-bundle

# Godot engine source directory
cd ./godot

scons -j2 CXX=$CXX CC=$CC platform=android tools=no target=debug
scons -j2 CXX=$CXX CC=$CC platform=android tools=no target=debug android_arch=x86
scons -j2 CXX=$CXX CC=$CC platform=android tools=no target=release
scons -j2 CXX=$CXX CC=$CC platform=android tools=no target=release android_arch=x86

cd platform/android/java
./gradlew clean
./gradlew build
cd -
```

## Godot Engine settings

### Using compiled templates

`Export` > `Android` > `Custom Package`, change fields `Debug` and `Release` to use the compiled android's templates (`bin` directory).

### Loading the module

Edit `engine.cfg` and add an `android` part as following:

```ini
[android]
modules="org/godotengine/godot/GodotAndroid"
```

## Initializing the module using GDScript

Here is an example

```python
extends Node

onready var godot_android = Globals.get_singleton('GodotAndroid')

func _ready():
  if OS.get_name() == 'Android' and godot_android != null:
    godot_android.google_initialize(get_instance_ID())
  else:
    godot_android = null

func google_auth_connected(user):
  print('User %s has logged in' % [user])

func google_auth_connect_failed(message):
  print('Login failed %s' % [ message ])

func google_connect():
  if godot_android != null:
    godot_android.google_connect()
```

# API

## Functions

### Google

|name|parameters|return|description|
|---|---|---|---|
|`google_initialize`|`int instance_id`|`void`|Initialize and connect to google play game service automatically. Google callbacks will be done using the instance_id. |
|`google_connect`||`void`|Connect to google play game service|
|`google_disconnect`||`void`|Disconnect from google play game service|
|`google_is_connected`||`boolean`|Return `true` if connected, `false` otherwise|
|`google_leaderboard_submit`|`String id, int score`|`void`|Submit a score to the given leaderboard|
|`google_leaderboard_show`|`String id`|`void`|Show the given leaderboard|
|`google_leaderboard_showlist`||`void`|Show the leaderboards' list|
|`google_snapshot_load`|`String name, int conflictResolutionPolicy`|`void`|Load the given snapshot. `godot_android` exposes the following values for the resolution policy `RESOLUTION_POLICY_HIGHEST_PROGRESS`, `RESOLUTION_POLICY_LAST_KNOWN_GOOD`, `RESOLUTION_POLICY_LONGEST_PLAYTIME`, `RESOLUTION_POLICY_MANUAL`, `RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED`|
|`google_snapshot_save`|`String name, String data, String description, boolean force`|`void`|Save a given snapshot. Use the `force` to overwrite a conflicting savegame|
|`get_google_resolution_policies`||`Dictionnary`|Return the google snapshot available policies|
|`google_achievement_unlock`|`String id`|`void`|Unlock the given achievement|
|`google_achievement_increment`|`String id, int amount`|`void`|Increment by `amount` the given achievement|
|`google_achievement_show_list`||`void`|Show the achievement list|

### Facebook

|name|parameters|return|description|
|---|---|---|---|
|`facebook_initialize`|`int instance_id`|`void`|Initialize and connect to facebook automatically. Facebook callbacks will be done using the instance_id. |
|`facebook_connect`||`void`|Connect to facebook|
|`facebook_disconnect`||`void`|Disconnect from facebook|
|`facebook_is_connected`||`boolean`|Return `true` if connected, `false` otherwise|
|`facebook_share_link`|`String link, [String quote], [String hashtag]`|`void`|Share a link via facebook|

### Firebase

|name|parameters|return|description|
|---|---|---|---|
|`firebase_initialize`|`int instance_id`|`void`|Initialize firebase. Firebase callbacks will be done using the instance_id. |
|`firebase_get_user_details`||`String`|Return the current firebase user. Need to `parse_json` in order to exploit it.|
|`firebase_log_event`|`String event_name, HashMap<String, Object> params`|`void`|Log custom event|
|`firebase_tutorial_begin`|`String name`|`void`|Log event `tutoriel_begin`|
|`firebase_tutorial_complete`|`String name`|`void`|Log event `tutorial_complete`|
|`firebase_purchase`|`String item`|`void`|Log event `purchase`|
|`firebase_unlock_achievement`|`String achievement`|`void`|Log event `unlock_achievement`|
|`firebase_join_group`|`String group`|`void`|Log event `join_group`|
|`firebase_login`||`void`|Log event `login`|
|`firebase_level_up`|`String name`|`void`|Log event `level_up`||
|`firebase_post_score`|`int score`|`void`|Log event `post_score`||
|`firebase_select_content`|`String name`|`void`|Log event `select_content`||
|`firebase_share`||`void`|Log event `share`|
|`firebase_invite`|`String message, String action_text, [String custom_image_uri], [String deep_link_uri]`|`void`|Send an application invitation (**WIP** on `deepLink`)|


## Callbacks

### Google

|name|parameters|description|
|---|---|---|
|`google_auth_connected`|`String username`|Called once connected to google play game service. username might be empty (not null)|
|`google_auth_disconnected`||Called once disconnected|
|`google_auth_connect_failed`|`String message`|Called when connection has failed. `message` is the reason of the failure|
|`google_achievement_unlocked`|`String id`|Called once the achievement has been unlocked|
|`google_achievement_unlock_failed`|`String message`|Called if the achievement unlocking has failed|
|`google_achievement_increased`|`String id, int amount`|Called once the achivement has been increased|
|`google_achievement_increment_failed`|`String message`|Called if the achievement increment has failed|
|`google_leaderboard_submitted`|`String id, int score`|Called once the leaderboard hs been updated|
|`google_leaderboard_submit_failed`|`String message`|Called if the leaderboard has not been updated|
|`google_leaderboard_showd`|`String id`|Called once the leaderboard has been showd|
|`google_leaderboard_show_failed`|`String message`|Called if there is an issue when trying to show the leaderboard|
|`google_leaderboard_showlisted`||Called once the leaderboards have been listed|
|`google_leaderboard_showlist_failed`|`String message`|Call if it failed to show the leaderboards' list|
|`google_snapshot_loaded`|`String data`|Called once the snapshot has been loaded|
|`google_snapshot_load_failed`|`String message`|Called if it failed to load the snapshot|
|`google_snapshot_saved`||Called once the snapshot has been saved|
|`google_snapshot_save_failed`|`String message`|Called if it failed to save the snapshot|

### Facebook

|name|parameters|description|
|---|---|---|
|`facebook_auth_connected`|`String username`|Called once connected to google play game service. username might be empty (not null)|
|`facebook_auth_disconnected`||Called once disconnected|
|`facebook_auth_connect_failed`|`String message`|Called when connection has failed. `message` is the reason of the failure|
|`facebook_auth_connect_cancelled`||Called if the user has cancelled the login|
|`facebook_share_success`||Called once link has been shared|
|`facebook_share_cancelled`||Called if share has been cancelled|
|`facebook_share_failed`|`String message`|Called if share has failed|

### Firebase

|name|parameters|description|
|---|---|---|
|`firebase_invite_success`|`String id`|Called once the application invitation has been sent|
|`firebase_invite_failed`|`String message`|Called if it failed to send the application invitation|

# Log

Used tag for adb login:

|Service|Tag|
|---|---|
|Google achievements|GoogleAchievements|
|Google authentication|GoogleAuthentication|
|Google leaderboards|GoogleLeaderboard|
|Google snapshot|GoogleSnapshot|
|Facebook authentication|FacebookAuthentication|
|Facebook share|FacebookShare|
|Firebase current user|FirebaseCurrentUser|

Example of a logcat command filtering only the Google Authentication service

```sh
# We keep godot:V to listen to godot's log
adb -d logcat godot:V GoogleAuthentication:V AndroidRuntime:V ValidateServiceOp:V *:S
```

# License

[See LICENSE file](./LICENSE)