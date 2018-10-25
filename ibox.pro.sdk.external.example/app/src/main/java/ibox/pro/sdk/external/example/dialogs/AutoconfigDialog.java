package ibox.pro.sdk.external.example.dialogs;

import android.content.Context;
import android.widget.Toast;

import ibox.pro.sdk.external.PaymentController;
import ibox.pro.sdk.external.example.R;

public class AutoconfigDialog extends ReaderServiceDialog {
    public AutoconfigDialog(Context context) {
        super(context);
    }

    @Override
    protected void startServiceAction() throws IllegalStateException {
        PaymentController.getInstance().startAutoConfig();
    }

    @Override
    public void onAutoConfigUpdate(double percent) {
        super.onAutoConfigUpdate(percent);
        setProgressText(String.format("%.0f%%", percent));
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
