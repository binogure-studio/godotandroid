# godot-android

Android services for Godot Engine 2.1, including:

* Google play game service

   * Authentication

   * Leaderboards

   * Achievements

   * Snapshots

* Firebase **WIP**

* Facebook **WIP**


# Usage

## Compiling Godot Engine w/ templates

Copy `google-services.json` file into `godot/platform/android/java/`. Then compile Godot Engine.

## Godot Engine settings

Edit `engine.cfg` and add an `android` part as following:

```ini
[android]
modules="org/godotengine/godot/GodotAndroid"
```

## Initliazing the module using GDScript

Here is an example

```python
onready var godot_android = Globals.get_singleton("GodotAndroid")

func _ready():
	if OS.get_name() == "Android":
		godot_android.init(get_instance_id())

```

# API
```python

# Google play Snapshots
# conflict_resolution values
# RESOLUTION_POLICY_HIGHEST_PROGRESS = 4
# RESOLUTION_POLICY_LAST_KNOWN_GOOD = 2
# RESOLUTION_POLICY_LONGEST_PLAYTIME = 1
# RESOLUTION_POLICY_MANUAL = -1
# RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED = 3
```

# Log

Used tag for adb login:

|Service|Tag|
|---|---|
|Google achievements|GoogleAchievements|
|Google authentication|GoogleAuthentication|
|Google leaderboards|GoogleLeaderboard|
|Google snapshot|GoogleSnapshot|

Example of a logcat command filtering only the Google Authentication service

```shell
# We keep godot:V to listen to godot's log
adb -d logcat godot:V GoogleAuthentication:V *:S
```

# License

[See LICENSE file](./LICENSE)