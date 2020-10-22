package ibox.pro.sdk.external.example.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import ibox.pro.sdk.external.PaymentController;
import ibox.pro.sdk.external.PaymentException;
import ibox.pro.sdk.external.PaymentResultContext;
import ibox.pro.sdk.external.ProcessingException;
import ibox.pro.sdk.external.example.CommonAsyncTask;
import ibox.pro.sdk.external.example.R;

public class DeferredResultDialog extends Dialog {
    private TextView lblTranID, lblHashPan, lblData;
    private Button btnSubmit;

    public DeferredResultDialog(@NonNull Context context, final PaymentResultContext resultContext) {
        super(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);

        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getWindow().getAttributes().gravity = Gravity.CENTER_VERTICAL;
        setContentView(R.layout.dialog_deferred_result);

        initControls();

        lblTranID.setText(resultContext.getTranId());
        lblHashPan.setText(resultContext.getCardHash());
        lblData.setText(resultContext.getDeferredData());

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new SubmitTask(getContext()).execute(resultContext);
            }
        });
    }

    private void initControls() {
        lblTranID  = findViewById(R.id.deferred_result_lbl_id);
        lblHashPan = findViewById(R.id.deferred_result_lbl_hashpan);
        lblData    = findViewById(R.id.defered_result_lbl_data);
        btnSubmit  = findViewById(R.id.defered_result_btn_submit);
    }

    private static class SubmitTask extends CommonAsyncTask<PaymentResultContext, Void, PaymentResultContext> {
        public SubmitTask(Context context) {
            super(context);
        }

        @Override
        protected PaymentResultContext doInBackground(PaymentResultContext... paymentResultContexts) {
            String error = null;
            try {
                return PaymentController.getInstance().submitDeferred(getContext(), paymentResultContexts[0].getDeferredData());
            } catch (PaymentException | ProcessingException e) {
                error = e.getMessage();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (error != null) {
                final String fError = error;
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getContext(), fError, Toast.LENGTH_LONG).show();
                    }
                });
            }
            return null;
        }

        @Override
        protected void onPostExecute(PaymentResultContext resultContext) {
            super.onPostExecute(resultContext);
            if (resultContext != null) {
                if (resultContext.getTransactionItem() != null)
                    new ResultDialog(getContext(), resultContext, false).show();
                else if (resultContext.getAttachedCard() != null)
                    Toast.makeText(getContext(), "Card '" + resultContext.getAttachedCard().getAlias() + "' was attached", Toast.LENGTH_LONG).show();
                else {
                    Toast.makeText(getContext(), R.string.history_item_inprocess, Toast.LENGTH_LONG).show();
                }
            }
        }
    }
}
