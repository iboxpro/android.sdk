package ibox.pro.sdk.external.example.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import ibox.pro.sdk.external.PaymentController;
import ibox.pro.sdk.external.PaymentControllerListener;
import ibox.pro.sdk.external.PaymentResultContext;
import ibox.pro.sdk.external.example.R;

public class FragmentScanner extends Fragment implements PaymentControllerListener {
    private TextView lblScanResult;

    @Override
    public void onResume() {
        super.onResume();
        PaymentController.getInstance().setPaymentControllerListener(this);
        PaymentController.getInstance().enable();
    }

    @Override
    public void onPause() {
        super.onPause();
        PaymentController.getInstance().setPaymentControllerListener(null);
        PaymentController.getInstance().disable();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scanner, container, false);
        lblScanResult = (TextView) view.findViewById(R.id.scanner_lbl_result);
        return view;
    }

    @Override
    public void onBarcodeScanned(String barcode) {
        lblScanResult.setText(barcode == null ? "" : barcode);
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
    public void onTransactionStarted(String transactionID) {

    }

    @Override
    public void onFinished(PaymentResultContext result) {

    }

    @Override
    public void onError(PaymentController.PaymentError error, String errorMessage) {

    }

    @Override
    public void onReaderEvent(PaymentController.ReaderEvent event) {

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
    public void onAutoConfigUpdate(double percent) {

    }

    @Override
    public void onAutoConfigFinished(boolean success, String config, boolean isDefault) {

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

    }

    @Override
    public void onInjectFinished(boolean success) {

    }

    @Override
    public void onEmvConfigFinished(boolean success) {

    }

    @Override
    public void onCapkConfigFinished(boolean success) {

    }
}
