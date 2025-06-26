package com.mpos.sdk.example.dialogs;

import android.content.Context;
import android.widget.Toast;

import com.mpos.sdk.PaymentController;
import com.mpos.sdk.PaymentControllerException;
import com.mpos.sdk.example.R;

public class AutoconfigDialog extends ReaderServiceDialog {
    public AutoconfigDialog(Context context) {
        super(context);
    }

    @Override
    protected void startServiceAction() throws PaymentControllerException {
        PaymentController.getInstance().startAutoConfig();
    }


    @Override
    public void onAutoConfigFinished(boolean success, String config, boolean isDefault) {
        super.onAutoConfigFinished(success, config, isDefault);
        stopProgress();
        StringBuilder result = new StringBuilder()
                .append(success ? getContext().getString(R.string.success) : getContext().getString(R.string.failed))
                .append(" ")
                .append(isDefault ? getContext().getString(R.string.settings_autoconfig_default) : config);
        Toast.makeText(getContext(), result.toString(),Toast.LENGTH_LONG).show();
        dismiss();
    }



}
