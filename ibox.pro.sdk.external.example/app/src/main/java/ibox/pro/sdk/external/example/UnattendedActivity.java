package ibox.pro.sdk.external.example;

import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import ibox.pro.sdk.external.PaymentContext;
import ibox.pro.sdk.external.PaymentController;
import ibox.pro.sdk.external.PaymentControllerListener;
import ibox.pro.sdk.external.PaymentResultContext;

public class UnattendedActivity extends PermissionsActivity implements PaymentControllerListener {
    private static final String LOGTAG = UnattendedActivity.class.getSimpleName();
    private static final String ACTION_ATTACHED = UsbManager.ACTION_USB_DEVICE_ATTACHED;
    private static final int READER_INIT_TIMEOUT = 30000;

    private boolean isReaderReady;
    private CountDownTimer initTimer;

    private EditText edtAmount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PaymentController.getInstance().onCreate(this, savedInstanceState);

        setContentView(R.layout.activity_unattended);
        edtAmount = findViewById(R.id.edtAmount);
        findViewById(R.id.btnPay).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pay();
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String readerAddress = Utils.getString(this, Consts.SavedParams.READER_ADDRESS_KEY);
        if (PaymentController.USB_MODE_KEY.equals(readerAddress)) {
            if (intent != null && ACTION_ATTACHED.equals(intent.getAction())) {
                log("DEVICE ATTACHED");
                if (!PaymentController.getInstance().isConnected()) {
                    isReaderReady = false;
                    reconnect();
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        PaymentController.getInstance().onDestroy();
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        PaymentController.getInstance().setPaymentControllerListener(this);
        PaymentController.getInstance().enable();
    }

    @Override
    protected void onStop() {
        PaymentController.getInstance().setPaymentControllerListener(null);
        PaymentController.getInstance().disable();
        super.onStop();
    }

    private void log(String text) {
        Log.d(LOGTAG, text);
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    private void reconnect() {
        PaymentController.getInstance().disable();
        PaymentController.getInstance().enable();
    }

    private void pay() {
        isReaderReady &= PaymentController.getInstance().isConnected();
        if (isReaderReady) {
            try {
                double amount = Double.parseDouble(edtAmount.getText().toString());
                PaymentContext paymentContext = new PaymentContext();
                paymentContext.setCurrency(PaymentController.Currency.RUB);
                paymentContext.setAmount(amount);
                PaymentController.getInstance().startPayment(this, paymentContext);
            } catch (Exception e) {
                log(e.toString());
            }
        } else
            log("Reader is not ready!");
    }

    //region PaymentControllerListener
    @Override
    public void onTransactionStarted(String transactionID) {
        log("onTransactionStarted: " + transactionID);
    }

    @Override
    public void onFinished(PaymentResultContext result) {
        edtAmount.setText(String.format(Locale.ENGLISH, "%.2f", result.getTransactionItem().getAmount() + 0.01));
        log("onFinished: " + result.getTransactionItem());
    }

    @Override
    public void onError(PaymentController.PaymentError error, String errorMessage, int extErrorCode) {
        log("onError: " + error + " | " + errorMessage + " | " + extErrorCode);
        pay();
    }

    @Override
    public void onReaderEvent(PaymentController.ReaderEvent event, Map<String, String> params) {
        log("onReaderEvent: " + event);
        switch (event) {
            case CONNECTED:
                if (initTimer != null)
                    initTimer.cancel();
                initTimer = new CountDownTimer(READER_INIT_TIMEOUT, 100) {
                    @Override
                    public void onTick(long millisUntilFinished) {

                    }

                    @Override
                    public void onFinish() {
                        if (!isReaderReady) {
                            log("reader init failed, reconnecting");
                            reconnect();
                        }
                    }
                };
                initTimer.start();
                break;
            case INIT_SUCCESSFULLY:
                isReaderReady = true;
                if (initTimer != null) {
                    initTimer.cancel();
                    initTimer = null;
                }
                pay();
                break;
            case CARD_TIMEOUT:
            case BAD_SWIPE:
            case PAYMENT_CANCELED:
                pay();
                break;
            case DISCONNECTED:
                String readerAddress = Utils.getString(this, Consts.SavedParams.READER_ADDRESS_KEY);
                if (!PaymentController.USB_MODE_KEY.equals(readerAddress)) //if USB connection is lost, it is better to try to reconnect at onNewIntent(), when device will be replugged
                    reconnect();
            case INIT_FAILED:
                isReaderReady = false;
                reconnect();
                break;
        }
    }

    @Override
    public int onSelectApplication(List<String> apps) {
        log("onSelectApplication: " + apps.toString());
        return 0;
    }

    @Override
    public boolean onConfirmSchedule(List<Map.Entry<Date, Double>> steps, double totalAmount) {
        return false;
    }

    @Override
    public boolean onScheduleCreationFailed(PaymentController.PaymentError error, String errorMessage, int extErrorCode) {
        return false;
    }

    @Override
    public boolean onCancellationTimeout() {
        log("onCancellationTimeout");
        return false;
    }

    @Override
    public void onPinRequest() {

    }

    @Override
    public void onPinEntered() {

    }

    @Override
    public void onAutoConfigUpdate(double percent) {

    }

    @Override
    public void onAutoConfigFinished(boolean success, String config, boolean isDefault) {

    }

    @Override
    public void onReaderConfigFinished(boolean success) {

    }

    @Override
    public void onBatteryState(double percent) {

    }

    @Override
    public PaymentController.PaymentInputType onSelectInputType(List<PaymentController.PaymentInputType> allowedInputTypes) {
        return null;
    }

    @Override
    public void onSwitchedToCNP() {
        log("onSwitchedToCNP");
    }

    @Override
    public void onReaderConfigUpdate(String s, Hashtable<String, Object> hashtable) {

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

    @Override
    public void onBatchReadMifareCardResult(String msg, Hashtable<String, List<String>> cardData) {

    }

    @Override
    public void onBatchWriteMifareCardResult(String msg, Hashtable<String, List<String>> cardData) {

    }

    @Override
    public void onVerifyMifareULData(Hashtable<String, String> data) {

    }

    @Override
    public void onGetMifareCardVersion(Hashtable<String, String> hashtable) {

    }

    @Override
    public void onGetMifareReadData(Hashtable<String, String> hashtable) {

    }

    @Override
    public void onGetMifareFastReadData(Hashtable<String, String> hashtable) {

    }

    @Override
    public void onWriteMifareULData(String s) {

    }
    //endregion
}
