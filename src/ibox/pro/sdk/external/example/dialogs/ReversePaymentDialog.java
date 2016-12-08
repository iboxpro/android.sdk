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
    private PaymentController.ReverseAction action;
    private OnPaymentCancelledListener cancelledListener;

    private Activity mActivity;
    private EditText edtAmount;
    private Button btnConfirm;

    public ReversePaymentDialog(Activity context, String transactionID, PaymentController.ReverseAction action, OnPaymentCancelledListener cancelledListener) {
        super(context);
        mActivity = context;
        this.transactionID = transactionID;
        this.action = action;
        this.cancelledListener = cancelledListener;
        init();
    }

    private void init() {
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_reverse);
        getWindow().getAttributes().gravity = Gravity.CENTER_VERTICAL;

        edtAmount = (EditText)findViewById(R.id.reverse_dlg_edt_amount);
        btnConfirm = (Button)findViewById(R.id.reverse_btn_confirm);

        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startReverse();
            }
        });
    }

    private void startReverse() {
        double reverseAmount = Double.parseDouble(edtAmount.getText().toString().replace(',', '.'));
        new ProgressDialog(reverseAmount).show();
        dismiss();
    }

    public interface OnPaymentCancelledListener {
        void onPaymentCancelled();
    }

    private class ProgressDialog extends PaymentDialog {

        private double reverseAmount;

        public ProgressDialog(double reverseAmount) {
            super(mActivity, null);
            this.reverseAmount = reverseAmount;
        }

        @Override
        protected void action() {
            try {
                PaymentController.getInstance().reversePayment(getContext(), transactionID, action, reverseAmount, PaymentController.Currency.RUB);
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

        @Override
        public void onAutoConfigUpdate(double perecent) {

        }

        @Override
        public void onAutoConfigFinished(boolean success, String config, boolean isDefault) {

        }
    }

}
