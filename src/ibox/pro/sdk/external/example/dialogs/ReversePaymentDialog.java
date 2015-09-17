package ibox.pro.sdk.external.example.dialogs;

import android.app.Activity;

import ibox.pro.sdk.external.PaymentController;
import ibox.pro.sdk.external.PaymentException;
import ibox.pro.sdk.external.PaymentResultContext;
import ibox.pro.sdk.external.example.R;

public class ReversePaymentDialog extends PaymentDialog {

    private String transactionID;
    private PaymentController.ReverseAction action;
    private OnPaymentCancelledListener cancelledListener;

    public interface OnPaymentCancelledListener {
        void onPaymentCancelled();
    }

    public ReversePaymentDialog(Activity context, String transactionID, PaymentController.ReverseAction action, OnPaymentCancelledListener cancelledListener) {
        super(context, null);
        this.transactionID = transactionID;
        this.action = action;
        this.cancelledListener = cancelledListener;
    }

    @Override
    protected void action() {
        try {
            PaymentController.getInstance().reversePayment(getContext(), transactionID, action);
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
        super.onFinished(paymentResultContext);
        if (cancelledListener != null)
            cancelledListener.onPaymentCancelled();
    }
}
