package com.mpos.sdk.example.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import com.mpos.sdk.PaymentController;
import com.mpos.sdk.PaymentControllerException;
import com.mpos.sdk.PaymentControllerListener;
import com.mpos.sdk.PaymentResultContext;
import com.mpos.sdk.example.R;

public class FragmentPrinter extends Fragment implements PaymentControllerListener {

    private EditText edtPrint;
    private View btnPrint;

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
        View view = inflater.inflate(R.layout.fragment_printer, container, false);
        edtPrint = (EditText) view.findViewById(R.id.printer_edt_print);
        btnPrint = view.findViewById(R.id.printer_btn_print);
        btnPrint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                printText(edtPrint.getText().toString());
            }
        });
        btnPrint.setEnabled(PaymentController.getInstance().isConnected());
        return view;
    }

    private void printText(String text) {
        new PrintTextTask(getContext()).execute(text);
    }

    private static class PrintTextTask extends AsyncTask<String, Void, PaymentController.PrintResult> {
        private Context context;
        private ProgressDialog progressDialog;

        public PrintTextTask(Context context) {
            this.context = context;
            progressDialog = new ProgressDialog(context);
            progressDialog.setCancelable(false);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog.show();
        }

        @Override
        protected PaymentController.PrintResult doInBackground(String... strings) {
            try {
                return PaymentController.getInstance().printText(strings[0], Layout.Alignment.ALIGN_NORMAL);
            } catch (PaymentControllerException e) {
                return PaymentController.PrintResult.PRINTER_ERROR;
            }
        }

        @Override
        protected void onPostExecute(PaymentController.PrintResult printResult) {
            super.onPostExecute(printResult);
            progressDialog.dismiss();
            Toast.makeText(context, String.valueOf(printResult), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onReaderEvent(PaymentController.ReaderEvent event, Map<String, String> params) {
        switch (event) {
            case CONNECTED:
            case START_INIT:
                break;
            case DISCONNECTED:
                btnPrint.setEnabled(false);
                Toast.makeText(getContext(), R.string.reader_state_disconnected, Toast.LENGTH_LONG).show();
                break;
            case INIT_SUCCESSFULLY:
                Log.i("mposSDK", "readerInfo: " + params);
                btnPrint.setEnabled(true);
                break;
            case INIT_FAILED:
                btnPrint.setEnabled(false);
                Toast.makeText(getContext(), R.string.reader_state_init_error, Toast.LENGTH_LONG).show();
                break;
            default:
                break;
        }
    }

    @Override
    public void onTransactionStarted(String transactionID) {

    }

    @Override
    public void onFinished(PaymentResultContext result) {

    }

    @Override
    public void onError(PaymentController.PaymentError error, String errorMessage, int extErrorCode) {

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
    public boolean onScheduleCreationFailed(PaymentController.PaymentError error, String errorMessage, int extErrorCode) {
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
    public void onBarcodeScanned(String barcode) {

    }


    @Override
    public void onReaderConfigFinished(boolean success) {

    }

    @Override
    public boolean onBLCheck(String hashPan, String last4digits) {
        return false;
    }

    @Override
    public void onReaderConfigUpdate(String config, Hashtable<String, Object> params) {

    }
}
