package com.mpos.sdk.example.dialogs;

import android.content.Context;
import android.widget.Toast;

import com.mpos.sdk.PaymentController;
import com.mpos.sdk.PaymentControllerException;
import com.mpos.sdk.example.R;

public class ConfigDialog extends ReaderServiceDialog {
    public ConfigDialog(Context context) {
        super(context);
    }

    @Override
    protected void startServiceAction() throws PaymentControllerException {
        PaymentController.getInstance().readerDownloadAndSetConfig(true);
    }

    @Override
    public void onReaderConfigFinished(boolean success) {
        super.onReaderConfigFinished(success);
        Toast.makeText(getContext(), getContext().getString(success ? R.string.success : R.string.failed),Toast.LENGTH_LONG).show();
        dismiss();
    }
}
