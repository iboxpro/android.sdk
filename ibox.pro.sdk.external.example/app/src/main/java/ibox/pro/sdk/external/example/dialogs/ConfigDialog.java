package ibox.pro.sdk.external.example.dialogs;

import android.content.Context;
import android.widget.Toast;

import ibox.pro.sdk.external.PaymentController;
import ibox.pro.sdk.external.example.R;

public class ConfigDialog extends ReaderServiceDialog {
    private final String config;
    public ConfigDialog(Context context, String config) {
        super(context);
        this.config = config;
    }

    @Override
    protected void startServiceAction() throws IllegalStateException {
        PaymentController.getInstance().readerSetConfig(config);
    }

    @Override
    public void onReaderConfigFinished(boolean success) {
        super.onReaderConfigFinished(success);
        Toast.makeText(getContext(), getContext().getString(success ? R.string.success : R.string.failed),Toast.LENGTH_LONG).show();
        dismiss();
    }
}
