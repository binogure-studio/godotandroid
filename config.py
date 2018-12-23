
def can_build(plat):
    return (plat == "android")

def configure(env):
    if env["platform"] == "android":
        env.android_add_maven_repository("url 'https://maven.fabric.io/public'")

        # Firebase dependencies
        env.android_add_dependency("compile 'com.google.firebase:firebase-core:16.0.6'")
        env.android_add_dependency("compile 'com.google.firebase:firebase-auth:16.1.0'")
        env.android_add_dependency("compile 'com.google.firebase:firebase-invites:16.0.6'")
        env.android_add_dependency("compile 'com.google.firebase:firebase-messaging:17.3.4'")
        env.android_add_dependency("compile 'com.google.firebase:firebase-appindexing:17.1.0'")

        # Play service dependencies
        env.android_add_dependency("compile 'com.google.android.gms:play-services-auth:16.0.1'")
        env.android_add_dependency("compile 'com.google.android.gms:play-services-games:16.0.0'")
        env.android_add_dependency("compile 'com.google.android.gms:play-services-drive:16.0.0'")

        # Play service google drive
        env.android_add_dependency("compile('com.google.apis:google-api-services-drive:v3-rev136-1.25.0') { exclude group: 'org.apache.httpcomponents' }")

        env.android_add_gradle_classpath('com.google.gms:google-services:4.1.0')
        env.android_add_gradle_classpath('io.fabric.tools:gradle:1.25.0')

        env.android_add_gradle_plugin('com.google.gms.google-services')
        env.android_add_gradle_plugin('io.fabric')
        env.android_add_java_dir("android/src")

    	env.android_add_res_dir("res")
        env.android_add_to_manifest("android/AndroidManifestChunk.xml")
        env.android_add_to_permissions("android/AndroidPermissionsChunk.xml")

        env.android_add_default_config("minSdkVersion 15")
        env.android_add_default_config("applicationId 'org.binogurestudio.sneakin'")

        env.disable_module()
