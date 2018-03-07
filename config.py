
def can_build(plat):
    return (plat == "android")

def configure(env):
    if env["platform"] == "android":
        env.android_add_maven_repository("url 'https://maven.google.com'")

        gms_version = "11.8.0"

        # Firebase dependencies
        env.android_add_dependency("compile 'com.google.firebase:firebase-core:" + gms_version + "'")
        env.android_add_dependency("compile 'com.google.firebase:firebase-auth:" + gms_version + "'")

        # Play service dependencies
        env.android_add_dependency("compile 'com.google.android.gms:play-services-auth:" + gms_version + "'")
        env.android_add_dependency("compile 'com.google.android.gms:play-services-games:" + gms_version + "'")
        env.android_add_dependency("compile 'com.google.android.gms:play-services-drive:" + gms_version + "'")

        env.android_add_gradle_classpath("com.google.gms:google-services:3.1.1")
        env.android_add_gradle_plugin("com.google.gms.google-services")

        # Facebook dependencies
        env.android_add_dependency("compile 'com.facebook.android:facebook-login:4.31.0'")

        env.android_add_java_dir("android/src")

    	env.android_add_res_dir("res")
        env.android_add_to_manifest("android/AndroidManifestChunk.xml")
        env.android_add_to_permissions("android/AndroidPermissionsChunk.xml")

        env.android_add_default_config("minSdkVersion 15")
        env.android_add_default_config("applicationId 'org.binogurestudio.sneakin'")

        env.disable_module()
