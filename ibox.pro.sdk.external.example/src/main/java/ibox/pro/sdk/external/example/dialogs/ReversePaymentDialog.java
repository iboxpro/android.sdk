package ibox.pro.sdk.external.example.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

import ibox.pro.sdk.external.PaymentController;
import ibox.pro.sdk.external.PaymentException;
import ibox.pro.sdk.external.PaymentResultContext;
import ibox.pro.sdk.external.example.R;

public class ReversePaymentDialog extends Dialog {

    private String transactionID;
    private Double amount;
    private String currency;
    private PaymentController.ReverseAction action;
    private OnPaymentCancelledListener cancelledListener;

    private Activity mActivity;
    private EditText edtAmount;
    private Button btnConfirm;


    public ReversePaymentDialog(Activity context, String transactionID, Double amount, String currency, PaymentController.ReverseAction action, OnPaymentCancelledListener cancelledListener) {
        super(context);
        mActivity = context;
        this.transactionID = transactionID;
        this.action = action;
        this.cancelledListener = cancelledListener;
        this.amount = amount;
        this.currency = currency;
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

        public ProgressDialog(Double reverseAmount) {
            super(mActivity, null);
            this.reverseAmount = reverseAmount;
        }

        @Override
        protected void action() {
            try {

                PaymentController.Currency curr = PaymentController.Currency.RUB;

                if ("RUB".equals(ReversePaymentDialog.this.currency))
                    curr = PaymentController.Currency.RUB;

                if ("VND".equals(ReversePaymentDialog.this.currency))
                    curr = PaymentController.Currency.VND;

                PaymentController.getInstance().reversePayment(getContext(), transactionID, action, reverseAmount, curr);

            } catch (PaymentException e) {
                onError(null, e.getMessage());
            }
        }

        @Override
        protected int getReadyStringID() {
            return R.string.reader_state_ready_swipeonly;
        }

        @Override
        public void onFinished(PaymentResultContext paymentResultContext) {
            dismiss();
            new ResultDialog(mActivity, paymentResultContext, false).show();
            if (cancelledListener != null)
                cancelledListener.onPaymentCancelled();
        }
    }

}
