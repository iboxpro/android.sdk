package ibox.pro.sdk.external.example;

import android.app.Activity;
import android.content.Context;

public class Utils {
    public static String getString(Activity activity, String key) {
        return activity.getSharedPreferences(activity.getApplicationContext().getPackageName(), Context.MODE_PRIVATE).getString(key, "");
    }

    public static void setString(Activity activity,String key, String value) {
        activity.getSharedPreferences(activity.getApplicationContext().getPackageName(), Context.MODE_PRIVATE).edit().putString(key, value).commit();
    }
}
