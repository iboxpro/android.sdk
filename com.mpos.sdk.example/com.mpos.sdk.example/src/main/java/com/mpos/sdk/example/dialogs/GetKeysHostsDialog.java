package com.mpos.sdk.example.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import androidx.annotation.NonNull;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Map;

import com.mpos.sdk.PaymentController;
import com.mpos.sdk.example.R;

public class GetKeysHostsDialog extends Dialog {

    private TextView lblTitle;
    private EditText edtPin;
    private ImageView imgSpinner;
    private View btnOk;

    private RotateAnimation spinRotation;

    public GetKeysHostsDialog(@NonNull Context context) {
        super(context);
        init();
    }

    private void init() {
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_keys_hosts);
        getWindow().getAttributes().gravity = Gravity.CENTER_VERTICAL;

        lblTitle = (TextView) findViewById(R.id.keys_dlg_lbl_title);
        edtPin = (EditText) findViewById(R.id.keys_dlg_edt_pin);
        imgSpinner = (ImageView) findViewById(R.id.keys_dlg_spinner);
        btnOk = findViewById(R.id.keys_dlg_btn_ok);

        spinRotation = new RotateAnimation(
                0f,
                360f,
                Animation.RELATIVE_TO_SELF,
                0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f);
        spinRotation.setDuration(1200);
        spinRotation.setInterpolator(new LinearInterpolator());
        spinRotation.setRepeatMode(Animation.RESTART);
        spinRotation.setRepeatCount(Animation.INFINITE);

        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new GetHostsTask().execute(edtPin.getText().toString());
            }
        });
    }

    protected void startProgress() {
        lblTitle.setText(R.string.progress);
        imgSpinner.setVisibility(View.VISIBLE);
        imgSpinner.startAnimation(spinRotation);
        edtPin.setVisibility(View.GONE);
        btnOk.setVisibility(View.GONE);
    }

    protected void stopProgress() {
        lblTitle.setText(R.string.payment_toast_pin_request);
        imgSpinner.clearAnimation();
        imgSpinner.setVisibility(View.GONE);
        edtPin.setVisibility(View.VISIBLE);
        btnOk.setVisibility(View.VISIBLE);
    }

    private class GetHostsTask extends AsyncTask<String, Void, Map<String, String>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            startProgress();
        }

        @Override
        protected Map<String, String> doInBackground(String... strings) {
            String pin = strings[0];
            return PaymentController.getInstance().getKeysHosts(getContext(), pin);
        }

        @Override
        protected void onPostExecute(final Map<String, String> hosts) {
            super.onPostExecute(hosts);
            stopProgress();

            if (hosts != null && hosts.size() > 0) {
                final CharSequence [] keys = hosts.keySet().toArray(new CharSequence[hosts.size()]);
                new AlertDialog.Builder(getContext())
                        .setItems(keys, new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                                new KeysInjectionDialog(getContext(), hosts.get(keys[i])).show();
                            }
                        })
                .create().show();
                dismiss();
            } else
                Toast.makeText(getContext(), R.string.common_error, Toast.LENGTH_LONG).show();
        }
    }
}
