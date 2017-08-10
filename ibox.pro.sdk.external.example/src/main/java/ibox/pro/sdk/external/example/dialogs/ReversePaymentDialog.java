package ibox.pro.sdk.external.example.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import ibox.pro.sdk.external.PaymentController;
import ibox.pro.sdk.external.PaymentException;
import ibox.pro.sdk.external.PaymentResultContext;
import ibox.pro.sdk.external.entities.TransactionItem;
import ibox.pro.sdk.external.example.R;

public class ReversePaymentDialog extends Dialog {

    private TransactionItem transaction;
    private Double amount;
    private String currency;
    private PaymentController.ReverseAction action;
    private OnPaymentCancelledListener cancelledListener;
    private JSONObject auxData;
    private Activity mActivity;
    private EditText edtAmount;
    private Button btnConfirm;


    public ReversePaymentDialog(Activity context, TransactionItem transaction, PaymentController.ReverseAction action, OnPaymentCancelledListener cancelledListener) {
        super(context);
        mActivity = context;
        this.transaction = transaction;
        this.action = action;
        this.cancelledListener = cancelledListener;
        this.amount = transaction.getAmountEff();
        this.currency = transaction.getCurrencyId();
        this.auxData = transaction.getAuxData();
        init();
    }

    private void init() {
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_reverse);
        getWindow().getAttributes().gravity = Gravity.CENTER_VERTICAL;

        edtAmount = (EditText)findViewById(R.id.reverse_dlg_edt_amount);
        btnConfirm = (Button)findViewById(R.id.reverse_btn_confirm);

        edtAmount.setText(this.amount.toString());

        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startReverse();
            }
        });
    }

    private void startReverse() {
        String amt = edtAmount.getText().toString().replace(',', '.');
        Double reverseAmount = null;
        try {
            reverseAmount = Double.parseDouble(amt);
        }
        catch (Exception ex) {
            reverseAmount = null;
        }
        new ProgressDialog(reverseAmount).show();
        dismiss();
    }

    public interface OnPaymentCancelledListener {
        void onPaymentCancelled();
    }

    private class ProgressDialog extends PaymentDialog {

        private Double reverseAmount;
        private int readyStringID;
        private int chipRetries;
        private List<TransactionItem.InputType> allowedInputTypes = new ArrayList<TransactionItem.InputType>();

        public ProgressDialog(Double reverseAmount) {
            super(mActivity, null);
            this.reverseAmount = reverseAmount;

            if (transaction.getCancelReturnTypes() != null)
                allowedInputTypes.addAll(transaction.getCancelReturnTypes());
            else if (transaction.getInputType() != TransactionItem.InputType.CHIP && transaction.getInputType() != TransactionItem.InputType.NFC)
                allowedInputTypes.add(transaction.getInputType());
            else
                allowedInputTypes.add(TransactionItem.InputType.SWIPE);


            configInputTypes();
        }

        private void configInputTypes() {
            boolean hasNonCardReverseInputTypes = allowedInputTypes.contains(TransactionItem.InputType.CASH) || allowedInputTypes.contains(TransactionItem.InputType.PREPAID);
            if (!hasNonCardReverseInputTypes) {
                if (allowedInputTypes.size() == 1) {
                    switch (allowedInputTypes.get(0)) {
                        case CHIP:
                            readyStringID = R.string.reader_state_ready_emvonly;
                            break;
                        case NFC:
                            readyStringID = R.string.reader_state_ready_nfconly;
                            break;
                        default:
                            readyStringID = R.string.reader_state_ready_swipeonly;
                    }
                } else if (allowedInputTypes.size() == 2) {
                    // swipe + emv == swipe + tap + emv
                    if (PaymentController.getInstance().getReaderType().isNfcSupported())
                        readyStringID = R.string.reader_state_ready_emv_or_tap;
                    else
                        readyStringID = R.string.reader_state_ready_emvonly;
                } else if (allowedInputTypes.size() == 3) {
                    if (PaymentController.getInstance().getReaderType().isMultiInputSupported())
                        readyStringID = R.string.reader_state_ready_multiinput;
                }
            }
        }

        @Override
        protected void action() {
            try {
                PaymentController.Currency curr = PaymentController.Currency.RUB;

                if ("RUB".equals(ReversePaymentDialog.this.currency))
                    curr = PaymentController.Currency.RUB;

                if ("VND".equals(ReversePaymentDialog.this.currency))
                    curr = PaymentController.Currency.VND;

                PaymentController.getInstance().reversePayment(getContext(), transaction.getID(), action, reverseAmount, curr, auxData);
            } catch (PaymentException e) {
                onError(null, e.getMessage());
            }
        }

        @Override
        public TransactionItem.InputType onSelectInputType(List<TransactionItem.InputType> allowedInputTypes) {
            TransactionItem.InputType result = super.onSelectInputType(allowedInputTypes);
            if (result != null)
                switch (result) {
                    case CHIP:
                        readyStringID = R.string.reader_state_ready_emvonly;
                        break;
                    case NFC:
                        readyStringID = R.string.reader_state_ready_nfconly;
                        break;
                    default:
                        readyStringID = R.string.reader_state_ready_swipeonly;
                }
            return result;
        }

        @Override
        public void onReaderEvent(PaymentController.ReaderEvent event) {
            if (event == PaymentController.ReaderEvent.EMV_TRANSACTION_STARTED || event == PaymentController.ReaderEvent.NFC_TRANSACTION_STARTED)
                chipRetries++;

            super.onReaderEvent(event);
        }

        @Override
        protected int getReadyStringID() {
            if (chipRetries >= PaymentController.MAX_EMV_RETRIES)
                if (!allowedInputTypes.contains(TransactionItem.InputType.SWIPE)) {
                    allowedInputTypes.add(TransactionItem.InputType.SWIPE);
                    configInputTypes();
                }
            return readyStringID;
        }

        @Override
        public void onFinished(PaymentResultContext paymentResultContext) {
            chipRetries = 0;
            dismiss();
            new ResultDialog(mActivity, paymentResultContext, false).show();
            if (cancelledListener != null)
                cancelledListener.onPaymentCancelled();
        }
    }

}
