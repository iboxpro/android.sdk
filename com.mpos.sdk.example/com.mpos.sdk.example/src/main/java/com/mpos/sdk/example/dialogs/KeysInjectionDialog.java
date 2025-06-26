package com.mpos.sdk.example.dialogs;

import android.content.Context;
import android.widget.Toast;

import com.mpos.sdk.PaymentController;
import com.mpos.sdk.PaymentControllerException;
import com.mpos.sdk.example.R;


public class KeysInjectionDialog extends ReaderServiceDialog {

    private final String host;

    public KeysInjectionDialog(Context context, String host) {
        super(context);
        this.host = host;
    }

    @Override
    protected void startServiceAction() throws PaymentControllerException {
        PaymentController.getInstance().readerInjectKeys(host);
    }

    @Override
    public void onInjectFinished(boolean success) {
        super.onInjectFinished(success);

        stopProgress();
        String result = success ? getContext().getString(R.string.success) : getContext().getString(R.string.failed);
        Toast.makeText(getContext(), result, Toast.LENGTH_LONG).show();
        dismiss();
    }
}
