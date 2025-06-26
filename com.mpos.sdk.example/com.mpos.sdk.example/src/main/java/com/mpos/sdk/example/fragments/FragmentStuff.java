package com.mpos.sdk.example.fragments;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.fragment.app.Fragment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.mpos.sdk.PaymentController;
import com.mpos.sdk.PaymentControllerException;
import com.mpos.sdk.PaymentControllerListener;
import com.mpos.sdk.PaymentResultContext;
import com.mpos.sdk.example.MainActivity;
import com.mpos.sdk.example.R;
import com.mpos.sdk.example.dialogs.ReaderServiceDialog;
import com.mpos.sdk.hardware.reader.Utils;

public class FragmentStuff extends Fragment  implements PaymentControllerListener {

    Button  btnBeep, btnEnableSound, btnDisableSound, btnReadCardPan, btnTest;
    TextView txtLog;

    public FragmentStuff() {
    }

    public void setControlsEnabled(boolean state) {
        btnBeep.setEnabled(state);
        btnDisableSound.setEnabled(state);
        btnEnableSound.setEnabled(state);
        btnReadCardPan.setEnabled(state);
        btnTest.setEnabled(state);

    }

    public void initControls(View view) {

        txtLog = (TextView) view.findViewById(R.id.stuff_txt_log);
        txtLog.setMovementMethod(new ScrollingMovementMethod());
        btnTest = (Button) view.findViewById(R.id.stuff_btn_test_mifare);

        btnBeep = (Button) view.findViewById(R.id.stuff_btn_beep);
        btnEnableSound = (Button) view.findViewById(R.id.stuff_btn_enable_sound);
        btnDisableSound = (Button) view.findViewById(R.id.stuff_btn_disable_sound);

        btnReadCardPan = (Button) view.findViewById(R.id.stuff_btn_read_card_pan);
        btnTest .setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Test();
            }
        });

        btnBeep.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PaymentController.getInstance().readerBeep(3);
            }
        });

        btnEnableSound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PaymentController.getInstance().setSoundEnabled(true);
            }
        });

        btnDisableSound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PaymentController.getInstance().setSoundEnabled(false);
            }
        });

        btnReadCardPan.setOnClickListener(new View.OnClickListener() {
            class PanDialog extends ReaderServiceDialog {
                final Handler mainThreadHanler = new Handler(Looper.getMainLooper());
                PanDialog() {
                    super(FragmentStuff.this.getContext(), false);
                }

                @Override
                protected void startProgress() {
                }

                @Override
                protected void onStart() {
                    super.onStart();
                    super.onReaderEvent(PaymentController.ReaderEvent.INIT_SUCCESSFULLY, null);
                    PaymentController.getInstance().setPaymentControllerListener(this);
                }

                @Override
                protected void onStop() {
                    super.onStop();
                    PaymentController.getInstance().setPaymentControllerListener(null);
                }

                @Override
                public void cancel() {
                    super.cancel();
                    PaymentController.getInstance().disable();

                    setControlsEnabled(false);
                    mainThreadHanler.postDelayed(() -> {
                        PaymentController.getInstance().setPaymentControllerListener(FragmentStuff.this);
                        PaymentController.getInstance().enable();
                    }, 1000);
                }

                @Override
                protected void startServiceAction() throws PaymentControllerException {
                    PaymentController.getInstance().readCardPan(MainActivity.CURRENCY);
                }


                @Override
                public void onReaderEvent(PaymentController.ReaderEvent event, Map<String, String> params) {
                    FragmentStuff.this.onReaderEvent(event, params);

                    if (event == PaymentController.ReaderEvent.SWIPE_CARD)
                        super.startProgress();

                    if (event == PaymentController.ReaderEvent.WAITING_FOR_CARD)
                        setText(R.string.reader_state_ready_swipeonly);
                    if (event == PaymentController.ReaderEvent.PAYMENT_CANCELED
                        || event == PaymentController.ReaderEvent.CARD_TIMEOUT
                        || event == PaymentController.ReaderEvent.CARD_INFO_RECEIVED)
                       dismiss();
                }
            }

            PanDialog dialog;

            @Override
            public void onClick(View v) {
                if (dialog == null)
                    dialog = new PanDialog();
                dialog.show();
            }
        });

        setControlsEnabled(PaymentController.getInstance().isConnected());
    }

    private void Test() {
        ProgressDialog progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                if (PaymentController.getInstance().startMifare()) {

                    Log("CardUID" + Utils.byteArrayToHexString(PaymentController.getInstance().getMifareUID()));
                    if (PaymentController.getInstance().authenticateMifare(
                            1,
                            new byte[]{(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF},
                            PaymentController.MifareKeyType.KEY_A
                    ))
                    {
                        Log("Authenticated!");
                        byte[] res =  PaymentController.getInstance().readMifareData(0);
                        Log("Read: " + Utils.byteArrayToHexString(res));
                        boolean writeResult = PaymentController.getInstance().writeMifareData(1,  Utils.hexStringToByteArray("DE AD BE EF DE AD BE EF DE AD BE EF DE AD BE EF"));
                        Log("Write result: "  + writeResult);

                    }
                }
            }
            catch (Exception ex) {}
            finally {
                PaymentController.getInstance().finishMifare();
            }

            handler.post(() -> {
                progressDialog.dismiss();
            });
        });
    }


    private void Log(final String text) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.i("MIFARE", text);
                String log = text + "\n" + txtLog.getText();
                txtLog.setText(log);
            }
        });
    }


    @Override
    public boolean onBLCheck(String hashPan, String last4digits) {
        return false;
    }

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stuff, container, false);
        initControls(view);
        return view;
    }


    @Override
    public void onTransactionStarted(String transactionID) {

    }

    @Override
    public void onFinished(PaymentResultContext result) {

    }

    @Override
    public void onError(PaymentController.PaymentError error, String errorMessage, int extErrorCode) {
        Toast.makeText(getContext(), String.format("%s (%s)", getString(R.string.common_error), String.valueOf(error)), Toast.LENGTH_LONG).show();
        setControlsEnabled(true);
    }

    @Override
    public void onReaderEvent(PaymentController.ReaderEvent event, Map<String, String> params) {
        switch (event) {
            case CONNECTED:
            case START_INIT:
                break;
            case DISCONNECTED:
                setControlsEnabled(false);
                Toast.makeText(getContext(), R.string.reader_state_disconnected, Toast.LENGTH_LONG).show();
                break;
            case INIT_SUCCESSFULLY:
                Log.i("mposSDK", "readerInfo: " + params);
                setControlsEnabled(true);
                break;
            case INIT_FAILED:
                setControlsEnabled(false);
                Toast.makeText(getContext(), R.string.reader_state_init_error, Toast.LENGTH_LONG).show();
                break;
            case CARD_INFO_RECEIVED:
                Toast.makeText(getContext(), "Card info: " + params, Toast.LENGTH_LONG).show();
                break;
            case PAYMENT_CANCELED:
                Toast.makeText(getContext(), "Cancelled", Toast.LENGTH_LONG).show();
                break;
            case CARD_TIMEOUT:
                Toast.makeText(getContext(), R.string.reader_card_timeout, Toast.LENGTH_LONG).show();
                break;
            case BAD_SWIPE:
                Toast.makeText(getContext(), R.string.reader_bad_swipe, Toast.LENGTH_LONG).show();
                break;
            default:
                break;
        }
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
    public void onReaderConfigUpdate(String config, Hashtable<String, Object> params) {

    }
}
