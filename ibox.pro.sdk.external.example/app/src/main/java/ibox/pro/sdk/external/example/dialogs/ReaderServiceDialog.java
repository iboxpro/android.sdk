package ibox.pro.sdk.external.example.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import ibox.pro.sdk.external.PaymentController;
import ibox.pro.sdk.external.PaymentControllerListener;
import ibox.pro.sdk.external.PaymentResultContext;
import ibox.pro.sdk.external.example.R;

public abstract class ReaderServiceDialog extends Dialog implements PaymentControllerListener {

    private TextView lblState;
    private ImageView imgSpinner;
    private RotateAnimation spinRotation;

    public ReaderServiceDialog(Context context) {
        super(context);
        init();
    }

    private void init() {
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_payment);
        getWindow().getAttributes().gravity = Gravity.CENTER_VERTICAL;

        lblState = (TextView) findViewById(R.id.payment_dlg_lbl_state);
        imgSpinner = (ImageView) findViewById(R.id.payment_dlg_spinner);

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

        PaymentController.getInstance().setPaymentControllerListener(this);
        PaymentController.getInstance().enable();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        PaymentController.getInstance().setPaymentControllerListener(null);
        PaymentController.getInstance().disable();
    }

    @Override
    public void dismiss() {
        stopProgress();
        super.dismiss();
    }

    protected abstract void startServiceAction() throws IllegalStateException;

    protected void startProgress() {
        imgSpinner.setVisibility(View.VISIBLE);
        imgSpinner.startAnimation(spinRotation);
    }

    protected void setProgressText(String text) {
        lblState.setText(text);
    }

    protected void stopProgress() {
        imgSpinner.clearAnimation();
        imgSpinner.setVisibility(View.GONE);
    }

    @Override
    public void onReaderEvent(PaymentController.ReaderEvent event) {
        switch (event) {
            case CONNECTED:
            case START_INIT:
                lblState.setText(R.string.reader_state_init);
                break;
            case DISCONNECTED:
                stopProgress();
                lblState.setText(R.string.reader_state_disconnected);
                break;
            case INIT_SUCCESSFULLY:
                lblState.setText(R.string.progress);
                startProgress();
                try {
                    startServiceAction();
                } catch (IllegalStateException e) {
                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                    dismiss();
                }
                break;
            case INIT_FAILED:
                stopProgress();
                lblState.setText(R.string.reader_state_init_error);
                break;
            default:
                break;
        }
    }

    @Override
    public void onAutoConfigUpdate(double perecent) {

    }

    @Override
    public void onAutoConfigFinished(boolean success, String config, boolean isDefault) {

    }

    @Override
    public void onTransactionStarted(String transactionID) {

    }

    @Override
    public void onFinished(PaymentResultContext result) {

    }

    @Override
    public void onError(PaymentController.PaymentError error, String errorMessage) {

    }

    @Override
    public int onSelectApplication(List<String> apps) {
        return 0;
    }

    @Override
    public boolean onConfirmSchedule(List<Map.Entry<Date, Double>> steps, double totalAmount) {
        return false;
    }

    @Override
    public boolean onScheduleCreationFailed(PaymentController.PaymentError error, String errorMessage) {
        return false;
    }

    @Override
    public boolean onCancellationTimeout() {
        return false;
    }

    @Override
    public void onPinRequest() {

    }

    @Override
    public void onPinEntered() {

    }

    @Override
    public void onBatteryState(double percent) {
        Toast.makeText(getContext(),
                String.format(getContext().getString(R.string.payment_toast_battery_format), percent),
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public PaymentController.PaymentInputType onSelectInputType(List<PaymentController.PaymentInputType> allowedInputTypes) {
        return null;
    }

    @Override
    public void onSwitchedToCNP() {

    }

    @Override
    public void onSearchMifareCard(Hashtable<String, String> cardData) {

    }

    @Override
    public void onVerifyMifareCard(boolean flag) {

    }

    @Override
    public void onWriteMifareCard(boolean flag) {

    }

    @Override
    public void onReadMifareCard(Hashtable<String, String> data) {

    }

    @Override
    public void onOperateMifareCard(Hashtable<String, String> data) {

    }

    @Override
    public void onTransferMifareData(String data) {

    }

    @Override
    public void onFinishMifareCard(boolean flag) {

    }

    @Override
    public void onReturnPowerOnNFCResult(boolean result) {

    }

    @Override
    public void onReturnNFCApduResult(boolean result, String apdu, int apduLen) {

    }

    @Override
    public void onReturnPowerOffNFCResult(boolean result) {

    }
}
