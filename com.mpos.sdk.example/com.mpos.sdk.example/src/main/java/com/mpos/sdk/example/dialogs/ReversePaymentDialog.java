package com.mpos.sdk.example.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.mpos.sdk.ReversePaymentContext;
import com.mpos.sdk.PaymentController;
import com.mpos.sdk.PaymentException;
import com.mpos.sdk.PaymentResultContext;
import com.mpos.sdk.entities.TransactionItem;
import com.mpos.sdk.example.R;

public class ReversePaymentDialog extends Dialog {

    private TransactionItem transaction;
    private Double amount;
    private String currency;
    private PaymentController.ReverseAction action;
    private OnPaymentCancelledListener cancelledListener;
    private JSONObject auxData;
    private Activity mActivity;
    private EditText edtAmount, edtERN, edtPhone, edtEmail, edtFiscalRoute;
    private CheckBox cbSuppressSignature, cbSkipFiscalization;
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
        setCanceledOnTouchOutside(false);

        edtAmount = (EditText)findViewById(R.id.reverse_dlg_edt_amount);
        edtERN = (EditText)findViewById(R.id.reverse_dlg_edt_ern);
        edtPhone = (EditText)findViewById(R.id.reverse_dlg_edt_phone);
        edtEmail = (EditText)findViewById(R.id.reverse_dlg_edt_email);
        edtFiscalRoute = (EditText)findViewById(R.id.reverse_dlg_edt_fiscalroute);
        cbSuppressSignature = (CheckBox)findViewById(R.id.reverse_dlg_cb_suppress_signature);
        cbSkipFiscalization = (CheckBox)findViewById(R.id.reverse_dlg_cb_skip_fiscalization);
        btnConfirm = (Button)findViewById(R.id.reverse_btn_confirm);

        edtAmount.setText(this.amount.toString());
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startReverse();
            }
        });
        edtERN.setEnabled(PaymentController.getInstance().getReaderType() != null && PaymentController.getInstance().getReaderType().isTTK());
    }

    private PaymentController.Currency getCurrency() {
        PaymentController.Currency curr = PaymentController.Currency.RUB;

        if ("RUB".equals(ReversePaymentDialog.this.currency))
            curr = PaymentController.Currency.RUB;

        if ("VND".equals(ReversePaymentDialog.this.currency))
            curr = PaymentController.Currency.VND;

        if ("EUR".equals(ReversePaymentDialog.this.currency))
            curr = PaymentController.Currency.EUR;

        if ("CAD".equals(ReversePaymentDialog.this.currency))
            curr = PaymentController.Currency.CAD;

        if ("KZT".equals(ReversePaymentDialog.this.currency))
            curr = PaymentController.Currency.KZT;

        return curr;
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
        private String tran2reverseID;
        private Double reverseAmount;
        private int readyStringID;
        private int chipRetries;
        private List<PaymentController.PaymentInputType> allowedInputTypes = new ArrayList<PaymentController.PaymentInputType>();

        public ProgressDialog(Double reverseAmount) {
            super(mActivity, null);
            this.reverseAmount = reverseAmount;

            if (transaction.getCancelReturnTypes() != null)
                allowedInputTypes.addAll(transaction.getCancelReturnTypes());
            else if (transaction.getInputType() != PaymentController.PaymentInputType.CHIP && transaction.getInputType() != PaymentController.PaymentInputType.NFC)
                allowedInputTypes.add(transaction.getInputType());
            else
                allowedInputTypes.add(PaymentController.PaymentInputType.SWIPE);


            configInputTypes();
        }

        @Override
        protected boolean usesReader() {
            boolean partial = BigDecimal.valueOf(reverseAmount).setScale(getCurrency().getE(), RoundingMode.HALF_UP).compareTo(BigDecimal.ZERO) != 0;
            boolean cnp = action == PaymentController.ReverseAction.RETURN
                    ? (transaction.canReturnCNPPartial() || (transaction.canReturnCNP() && !partial))
                    : (transaction.canCancelCNPPartial() || (transaction.canCancelCNP() && !partial));

            return (allowedInputTypes.contains(PaymentController.PaymentInputType.SWIPE)
                        || allowedInputTypes.contains(PaymentController.PaymentInputType.CHIP)
                        || allowedInputTypes.contains(PaymentController.PaymentInputType.NFC)
                        || allowedInputTypes.contains(PaymentController.PaymentInputType.MANUAL))
                    && !cnp;
        }

        private void configInputTypes() {
            boolean hasNonCardReverseInputTypes = allowedInputTypes.contains(PaymentController.PaymentInputType.CASH)
                    || allowedInputTypes.contains(PaymentController.PaymentInputType.PREPAID)
                    || allowedInputTypes.contains(PaymentController.PaymentInputType.CREDIT);
            if (!hasNonCardReverseInputTypes) {
                boolean nfcAllowed = PaymentController.getInstance().getNfcLimit() == null
                        || (BigDecimal.valueOf(PaymentController.getInstance().getNfcLimit(), getCurrency().getE()).setScale(getCurrency().getE(), RoundingMode.HALF_UP)
                            .compareTo(BigDecimal.valueOf(transaction.getAmount()).setScale(getCurrency().getE(), RoundingMode.HALF_UP)) > 0);

                if (allowedInputTypes.size() == 1) {
                    switch (allowedInputTypes.get(0)) {
                        case CHIP:
                            readyStringID = R.string.reader_state_ready_emvonly;
                            break;
                        case NFC:
                            if (nfcAllowed) {
                                readyStringID = R.string.reader_state_ready_nfconly;
                                break;
                            }
                        default:
                            readyStringID = R.string.reader_state_ready_swipeonly;
                    }
                } else if (allowedInputTypes.size() == 2) {
                    // swipe + emv == swipe + tap + emv
                    boolean acceptSwipe = allowedInputTypes.contains(PaymentController.PaymentInputType.SWIPE);
                    boolean acceptEMV = allowedInputTypes.contains(PaymentController.PaymentInputType.CHIP);
                    boolean acceptNFC = nfcAllowed
                            && PaymentController.getInstance().getReaderType().isNfcSupported()
                            && allowedInputTypes.contains(PaymentController.PaymentInputType.NFC);
                    if (acceptSwipe && acceptEMV && acceptNFC)
                        readyStringID = R.string.reader_state_ready_multiinput;
                    else if (acceptSwipe && acceptEMV)
                        readyStringID = R.string.reader_state_ready;
                    else if (acceptEMV && acceptNFC)
                        readyStringID = R.string.reader_state_ready_emv_or_tap;
                    else if (acceptEMV)
                        readyStringID = R.string.reader_state_ready_emvonly;
                    else if (acceptSwipe)
                        readyStringID = R.string.reader_state_ready_nfconly;
                    else
                        readyStringID = R.string.reader_state_ready_swipeonly;
                } else if (allowedInputTypes.size() == 3) {
                    if (nfcAllowed)
                        readyStringID = R.string.reader_state_ready_multiinput;
                    else
                        readyStringID = R.string.reader_state_ready;
                }
            }
        }

        @Override
        protected void action() throws PaymentException {
            ReversePaymentContext reversePaymentContext = new ReversePaymentContext();
            reversePaymentContext.setTransactionID(transaction.getID());
            reversePaymentContext.setAction(action);
            reversePaymentContext.setReturnAmount(reverseAmount);
            reversePaymentContext.setCurrency(getCurrency());
            reversePaymentContext.setAuxData(auxData);
            reversePaymentContext.setReceiptPhone(edtPhone.getText().toString());
            reversePaymentContext.setReceiptEmail(edtEmail.getText().toString());
            reversePaymentContext.setFiscalRouteProfile(edtFiscalRoute.getText().toString());
            reversePaymentContext.setExtID("TEST_APP");
            //reversePaymentContext.setExtTranData("TEST_APP");
            reversePaymentContext.setSuppressSignatureWaiting(cbSuppressSignature.isChecked());
            reversePaymentContext.setSkipFiscalization(cbSkipFiscalization.isChecked());
            if (PaymentController.getInstance().getReaderType() != null && PaymentController.getInstance().getReaderType().isTTK())
                reversePaymentContext.setErn(Integer.parseInt(edtERN.getText().toString()));
            PaymentController.getInstance().reversePayment(getContext(), reversePaymentContext);
        }

        @Override
        public void onTransactionStarted(String transactionID) {
            tran2reverseID = transactionID;
            startProgress();
        }

        @Override
        public PaymentController.PaymentInputType onSelectInputType(List<PaymentController.PaymentInputType> allowedInputTypes) {
            PaymentController.PaymentInputType result = super.onSelectInputType(allowedInputTypes);
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
        public void onReaderEvent(PaymentController.ReaderEvent event, Map<String, String> params) {
            if (event == PaymentController.ReaderEvent.EMV_TRANSACTION_STARTED || event == PaymentController.ReaderEvent.NFC_TRANSACTION_STARTED)
                chipRetries++;

            super.onReaderEvent(event, params);
        }

        @Override
        protected int getReadyStringID() {
            if (chipRetries >= PaymentController.MAX_EMV_RETRIES)
                if (!allowedInputTypes.contains(PaymentController.PaymentInputType.SWIPE)) {
                    allowedInputTypes.add(PaymentController.PaymentInputType.SWIPE);
                    configInputTypes();
                }
            return readyStringID;
        }

        @Override
        protected String getProgressString() {
            return String.format(getContext().getString(R.string.payment_dlg_reverse_started), tran2reverseID);
        }

        @Override
        public void onFinished(PaymentResultContext paymentResultContext) {
            chipRetries = 0;
            dismiss();
            new ResultDialog(mActivity, paymentResultContext, true).show();
            if (cancelledListener != null)
                cancelledListener.onPaymentCancelled();
        }
    }

}
