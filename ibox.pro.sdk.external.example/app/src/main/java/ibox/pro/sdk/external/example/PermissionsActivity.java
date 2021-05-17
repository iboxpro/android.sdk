package ibox.pro.sdk.external.example;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import java.util.ArrayList;

public abstract class PermissionsActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!permissionGranted())
            requestPermissions();
    }

    protected final boolean permissionGranted() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                || (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
    }

    private void requestPermissions() {
        if (!permissionGranted()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ArrayList<String> permissions2request = new ArrayList<>();
                if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED)
                    permissions2request.add(Manifest.permission.READ_PHONE_STATE);
                if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                    permissions2request.add(Manifest.permission.ACCESS_COARSE_LOCATION);
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                    permissions2request.add(Manifest.permission.ACCESS_FINE_LOCATION);

                if (permissions2request.size() > 0)
                    requestPermissions(permissions2request.toArray(new String[]{}), Consts.RequestCodes.CODE_REQUEST_PERMISSIONS);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0)
            if (requestCode == Consts.RequestCodes.CODE_REQUEST_PERMISSIONS) {
                for (int result : grantResults)
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        finish();
                        return;
                    }
            }
    }
}
