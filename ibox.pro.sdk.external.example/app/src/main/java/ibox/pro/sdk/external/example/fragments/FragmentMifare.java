package ibox.pro.sdk.external.example.fragments;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import ibox.pro.sdk.external.PaymentController;
import ibox.pro.sdk.external.PaymentControllerListener;
import ibox.pro.sdk.external.PaymentException;
import ibox.pro.sdk.external.PaymentResultContext;
import ibox.pro.sdk.external.example.MainActivity;
import ibox.pro.sdk.external.example.R;
public class FragmentMifare extends Fragment  implements PaymentControllerListener {

    Button btnPoll, btnVerifyPin, btnWriteCard, btnReadCard, btnFinish, btnStatus, btnPenetrate, btnPowerOnNfc, btnPowerOffNfc
            , btnSendApdu, btnBeep, btnEnableSound, btnDisableSound, btnReadCardPan, btnReadCardPanHash
            , btnNewPoll,btnNewAuthenticate, btnNewRead, btnNewWrite, btnNewFinish, btnNewFastRead;

    TextView txtLog, txtApdu, txtNewData, txtNewKeyType, txtNewKeyValue, txtNewTimeout, txtNewBlock, txtNewStartBlock, txtNewEndBlock;

    RadioGroup rgMifareCardType;

    EditText txtBlockAddr, txtKeyValue, txtTimeout;

    public FragmentMifare() {
    }

    public void setControlsEnabled(boolean state) {
        btnPoll.setEnabled(state);
        btnVerifyPin.setEnabled(state);
        btnWriteCard.setEnabled(state);
        btnReadCard.setEnabled(state);
        btnFinish.setEnabled(state);
        btnPenetrate.setEnabled(state);
        btnStatus.setEnabled(state);
        btnPowerOnNfc.setEnabled(state);
        btnSendApdu.setEnabled(state);
        btnPowerOffNfc.setEnabled(state);
        btnBeep.setEnabled(state);
        btnDisableSound.setEnabled(state);
        btnEnableSound.setEnabled(state);
        btnReadCardPan.setEnabled(state);
        btnReadCardPanHash.setEnabled(state);

        btnNewPoll.setEnabled(state);
        btnNewAuthenticate.setEnabled(state);
        btnNewRead.setEnabled(state);
        btnNewWrite.setEnabled(state);
        btnNewFinish.setEnabled(state);
        btnNewFastRead.setEnabled(state);
    }

    public void initControls(View view) {

        txtLog = (TextView) view.findViewById(R.id.mifare_txt_log);
        txtLog.setMovementMethod(new ScrollingMovementMethod());

        btnPoll = (Button) view.findViewById(R.id.mifare_btn_poll);

        btnVerifyPin = (Button) view.findViewById(R.id.mifare_btn_verify_pin);
        btnWriteCard = (Button) view.findViewById(R.id.mifare_btn_write_card);
        btnReadCard = (Button) view.findViewById(R.id.mifare_btn_read_card);
        btnFinish = (Button) view.findViewById(R.id.mifare_btn_finish);
        btnStatus = (Button) view.findViewById(R.id.mifare_btn_status);
        btnPenetrate = (Button) view.findViewById(R.id.mifare_btn_penetrate);

        btnPowerOnNfc = (Button) view.findViewById(R.id.mifare_btn_poweronnfc);
        btnPowerOffNfc = (Button) view.findViewById(R.id.mifare_btn_poweroffnfc);
        btnSendApdu = (Button) view.findViewById(R.id.mifare_btn_send_apdu);

        btnBeep = (Button) view.findViewById(R.id.mifare_btn_beep);
        btnEnableSound = (Button) view.findViewById(R.id.mifare_btn_enable_sound);
        btnDisableSound = (Button) view.findViewById(R.id.mifare_btn_disable_sound);

        btnReadCardPan = (Button) view.findViewById(R.id.mifare_btn_read_card_pan);
        btnReadCardPanHash = (Button) view.findViewById(R.id.mifare_btn_read_card_panhash);

        btnNewPoll = (Button) view.findViewById(R.id.mifare_btn_new_poll);
        btnNewAuthenticate = (Button) view.findViewById(R.id.mifare_btn_new_authenticate);
        btnNewRead = (Button) view.findViewById(R.id.mifare_btn_new_read);
        btnNewWrite= (Button) view.findViewById(R.id.mifare_btn_new_write);
        btnNewFinish= (Button) view.findViewById(R.id.mifare_btn_new_finish);
        btnNewFastRead= (Button) view.findViewById(R.id.mifare_btn_new_fastread);

        btnNewPoll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int timeout = new Integer(txtTimeout.getText().toString());
                PaymentController.getInstance().pollOnMifareCard(timeout);
            }
        });

        btnNewAuthenticate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int timeout = new Integer(txtTimeout.getText().toString());
                PaymentController.MifareCardType cardType = rgMifareCardType.getCheckedRadioButtonId() == R.id.rb_mifare_card_type_ultralight ? PaymentController.MifareCardType.UlTRALIGHT : PaymentController.MifareCardType.CLASSIC;
                PaymentController.getInstance().authenticateMifareCard(cardType, txtNewKeyType.getText().toString(), txtNewBlock.getText().toString(), txtNewKeyValue.getText().toString(), timeout);
                Log("Authenticate" + cardType.name() + ", " + txtNewKeyType.getText().toString() + "," + txtNewKeyValue.getText().toString());
            }
        });

        btnNewRead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int timeout = new Integer(txtTimeout.getText().toString());
                PaymentController.MifareCardType cardType = rgMifareCardType.getCheckedRadioButtonId() == R.id.rb_mifare_card_type_ultralight ? PaymentController.MifareCardType.UlTRALIGHT : PaymentController.MifareCardType.CLASSIC;
                PaymentController.getInstance().readMifareCard(cardType, txtNewBlock.getText().toString(), timeout);
            }
        });

        btnNewWrite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int timeout = new Integer(txtTimeout.getText().toString());
                PaymentController.MifareCardType cardType = rgMifareCardType.getCheckedRadioButtonId() == R.id.rb_mifare_card_type_ultralight ? PaymentController.MifareCardType.UlTRALIGHT : PaymentController.MifareCardType.CLASSIC;
                PaymentController.getInstance().writeMifareCard(cardType,txtNewBlock.getText().toString(), txtNewData.getText().toString(), timeout);
            }
        });

        btnNewFinish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int timeout = new Integer(txtTimeout.getText().toString());
                PaymentController.getInstance().finishMifareCard(timeout);

            }
        });

        btnNewFastRead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int timeout = new Integer(txtTimeout.getText().toString());
                PaymentController.getInstance().fastReadMifareCardData(txtNewStartBlock.getText().toString(), txtNewEndBlock.getText().toString(), timeout);
            }
        });


        txtApdu = (EditText) view.findViewById(R.id.mifare_txt_apdu);

        txtBlockAddr = (EditText) view.findViewById(R.id.txtBlockAddr);
        txtKeyValue = (EditText) view.findViewById(R.id.txtKeyValue);
        txtTimeout = (EditText) view.findViewById(R.id.txtTimeout);

        txtNewData = (EditText) view.findViewById(R.id.txt_new_data);
        txtNewKeyType = (EditText) view.findViewById(R.id.txt_new_key_type);
        txtNewKeyValue = (EditText) view.findViewById(R.id.txt_new_key_value);
        txtNewTimeout = (EditText) view.findViewById(R.id.txt_new_timeout);
        txtNewBlock = (EditText) view.findViewById(R.id.txt_new_block);
        txtNewStartBlock = (EditText) view.findViewById(R.id.txt_new_start_block);
        txtNewEndBlock = (EditText) view.findViewById(R.id.txt_new_end_block);

        rgMifareCardType = (RadioGroup)view.findViewById(R.id.rg_mifare_card_type);

        btnBeep.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PaymentController.getInstance().readerBeep(3);
            }
        });

        btnPoll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PaymentController.getInstance().doMifareCard("01", 20);
            }
        });

        btnVerifyPin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PaymentController.getInstance().setMifareBlockaddr(txtBlockAddr.getText().toString());
                PaymentController.getInstance().setMifareKeyValue(txtKeyValue.getText().toString());
                PaymentController.getInstance().doMifareCard("02Key A", Integer.parseInt(txtTimeout.getText().toString()));

            }
        });

        btnWriteCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PaymentController.getInstance().setMifareBlockaddr("0A");
                PaymentController.getInstance().setMifareKeyValue("ffffffffffff");
                PaymentController.getInstance().doMifareCard("04", 20);
            }
        });

        btnReadCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PaymentController.getInstance().setMifareBlockaddr("0A");
                PaymentController.getInstance().doMifareCard("03", 20);
            }
        });

        btnFinish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PaymentController.getInstance().doMifareCard("0E", 20);
            }
        });

        btnStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String str = PaymentController.getInstance().getMifareStatusMsg();
                if (str == null) str = "null";
                Log(str);
            }
        });

        btnPenetrate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PaymentController.getInstance().setMifareKeyValue("90 6a 00 00 00");
                PaymentController.getInstance().setMifareLen(5);
                PaymentController.getInstance().doMifareCard("0F", 500);
            }
        });

        btnPowerOnNfc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PaymentController.getInstance().powerOnNFC(false, 500);
            }
        });

        btnPowerOffNfc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PaymentController.getInstance().powerOffNFC(500);
            }
        });

        btnSendApdu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PaymentController.getInstance().sendApduByNFC(txtApdu.getText().toString(), 500);
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
            @Override
            public void onClick(View v) {
                try {
                    PaymentController.getInstance().readCardPan(MainActivity.CURRENCY);
                } catch (PaymentException e) {
                    e.printStackTrace();
                }
            }
        });

        btnReadCardPanHash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    PaymentController.getInstance().readCardPanHash(MainActivity.CURRENCY);
                } catch (PaymentException e) {
                    e.printStackTrace();
                }
            }
        });

        setControlsEnabled(PaymentController.getInstance().isConnected());
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
    public void onSearchMifareCard(Hashtable<String, String> cardData) {
        Log("onSearchMifareCard");
        if (cardData != null)
            for (String key : cardData.keySet()) {
                Log(key + " : " + cardData.get(key));
            }
    }

    @Override
    public void onVerifyMifareCard(boolean flag) {
        Log("onVerifyMifareCard : " + flag);
    }

    @Override
    public void onWriteMifareCard(boolean flag) {
        Log("onWriteMifareCard : " + flag);
    }

    @Override
    public void onReadMifareCard(Hashtable<String, String> data) {
        Log("onReadMifareCard");
        if (data != null)
        for (String key : data.keySet()) {
            Log(key + " : " + data.get(key));
        }
    }

    @Override
    public void onOperateMifareCard(Hashtable<String, String> data) {
        Log("onOperateMifareCard");
        for (String key : data.keySet()) {
            Log(key + " : " + data.get(key));
        }
    }

    @Override
    public void onTransferMifareData(String data) {
        Log("onTransferMifareData : " + data);
    }

    @Override
    public void onFinishMifareCard(boolean flag) {
        Log("onFinishMifareCard : " + flag);
    }

    @Override
    public void onReturnPowerOnNFCResult(boolean result) {
        Log( "onReturnPowerOnNFCResult: " + result);
    }

    @Override
    public void onReturnNFCApduResult(boolean result, String apdu, int apduLen) {
        Log( "onReturnNFCApduResult: " + result + ", " + apdu + ", " + apduLen);
    }

    @Override
    public void onReturnPowerOffNFCResult(boolean result) {
        Log( "onReturnPowerOffNFCResult: " + result);

    }

    @Override
    public void onBatchReadMifareCardResult(String msg, Hashtable<String, List<String>> cardData) {
        Log("onBatchReadMifareCardResult");
        for (String key : cardData.keySet()) {
            Log(key);
            for (String s : cardData.get(key))
                Log(s);
        }
    }

    @Override
    public void onBatchWriteMifareCardResult(String msg, Hashtable<String, List<String>> cardData) {
        Log("onBatchWriteMifareCardResult");
        for (String key : cardData.keySet()) {
            Log(key);
            for (String s : cardData.get(key))
                Log(s);
        }
    }

    @Override
    public void onVerifyMifareULData(Hashtable<String, String> data) {
        Log("onVerifyMifareULData");
        for (String key : data.keySet()) {
            Log(key + " : " + data.get(key));
        }
    }

    @Override
    public void onGetMifareCardVersion(Hashtable<String, String> data) {
        Log("onGetMifareCardVersion");
        for (String key : data.keySet()) {
            Log(key + " : " + data.get(key));
        }
    }

    @Override
    public void onGetMifareReadData(Hashtable<String, String> data) {
        Log("onGetMifareReadData");
        for (String key : data.keySet()) {
            Log(key + " : " + data.get(key));
        }
    }

    @Override
    public void onGetMifareFastReadData(Hashtable<String, String> data) {
        Log("onGetMifareFastReadData");
        for (String key : data.keySet()) {
            Log(key + " : " + data.get(key));
        }
    }

    @Override
    public void onWriteMifareULData(String s) {
        Log("onWriteMifareULData:  " + s);
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
        View view = inflater.inflate(R.layout.fragment_mifare, container, false);
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
                Log.i("iboxSDK", "readerInfo: " + params);
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

    }

    @Override
    public void onReaderConfigUpdate(String s, Hashtable<String, Object> hashtable) {

    }
}
