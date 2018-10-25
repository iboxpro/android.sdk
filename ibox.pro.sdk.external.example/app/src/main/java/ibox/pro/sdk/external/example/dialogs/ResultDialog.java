package ibox.pro.sdk.external.example.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import ibox.pro.sdk.external.PaymentController;
import ibox.pro.sdk.external.PaymentResultContext;
import ibox.pro.sdk.external.entities.APITryGetPaymentStatusResult;
import ibox.pro.sdk.external.entities.ScheduleItem;
import ibox.pro.sdk.external.entities.TransactionItem;
import ibox.pro.sdk.external.example.MainActivity;
import ibox.pro.sdk.external.example.R;
import ibox.pro.sdk.external.example.Utils;

public class ResultDialog extends Dialog {

	private TextView lblOperation, lblState, lblID, lblInvoice, lblAppcode, lblTerminal,
                lblIIN, lblPAN, lblLink,
                lblEMV, lblSignature, lblFiscalStatus;
	private Button btnAdjust, btnFiscalStatus, btnInvoice;

	public ResultDialog(final Context context, final PaymentResultContext paymentResultContext, final boolean isReverse) {
		super(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
		
		getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getWindow().getAttributes().gravity = Gravity.CENTER_VERTICAL;
        setContentView(R.layout.dialog_tr_details);

        initControls();

        final TransactionItem transactionItem = paymentResultContext.getTransactionItem();
        final ScheduleItem scheduleItem = paymentResultContext.getScheduleItem();
        final boolean isRegular = scheduleItem != null;

        if (isRegular)
            update(scheduleItem);
        else
            update(transactionItem);
        lblTerminal.setText(paymentResultContext.getTerminalName());
        lblSignature.setText(String.valueOf(paymentResultContext.isRequiresSignature()));
        if (!isRegular && paymentResultContext.getEmvData() != null) {
            StringBuilder emvData = new StringBuilder();
            Iterator<Map.Entry<String, String>> iterator = paymentResultContext.getEmvData().entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, String> nextPair = iterator.next();
                emvData.append(nextPair.getKey()).append(" : ").append(nextPair.getValue());
                if (iterator.hasNext())
                    emvData.append("\n");
            }
            lblEMV.setText(emvData.toString());
        }

        btnAdjust.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				dismiss();
				new AdjustDialog(getContext(), isRegular ? String.valueOf(scheduleItem.getID()) : transactionItem.getID(), isRegular, isReverse).show();
			}
		});

        btnFiscalStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                new CheckFiscalStatusTask(ResultDialog.this).execute(transactionItem.getID());
            }
        });

        if (paymentResultContext.getTransactionItem() != null)
            btnInvoice.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String invoice = Utils.BuildInvoice(((MainActivity) context).Account, paymentResultContext.getTransactionItem());
                    Log.i(paymentResultContext.getTransactionItem().getID(), invoice);
                    TextView textView = new TextView(getContext());
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                    textView.setTypeface(Typeface.MONOSPACE);
                    textView.setText(invoice);
                    textView.setTextColor(Color.WHITE);
                    textView.setGravity(Gravity.CENTER);
                    new AlertDialog.Builder(getContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
                            .setView(textView)
                            .create().show();
                }
            });
        else
            btnInvoice.setVisibility(View.GONE);


        if (paymentResultContext.getTransactionItem() == null) {
            LinearLayout container = (LinearLayout) lblState.getParent();

            container.getChildAt(container.indexOfChild(lblState) - 1).setVisibility(View.GONE);
            lblState.setVisibility(View.GONE);
            container.getChildAt(container.indexOfChild(lblInvoice) - 1).setVisibility(View.GONE);
            lblInvoice.setVisibility(View.GONE);
            container.getChildAt(container.indexOfChild(lblAppcode) - 1).setVisibility(View.GONE);
            lblAppcode.setVisibility(View.GONE);
            container.getChildAt(container.indexOfChild(lblTerminal) - 1).setVisibility(View.GONE);
            lblTerminal.setVisibility(View.GONE);
            container.getChildAt(container.indexOfChild(lblLink) - 1).setVisibility(View.GONE);
            lblLink.setVisibility(View.GONE);
            container.getChildAt(container.indexOfChild(lblEMV) - 1).setVisibility(View.GONE);
            lblEMV.setVisibility(View.GONE);
            container.getChildAt(container.indexOfChild(lblFiscalStatus) - 1).setVisibility(View.GONE);
            lblFiscalStatus.setVisibility(View.GONE);
        }
	}
	
	private void initControls() {
        lblOperation	= (TextView)findViewById(R.id.tr_details_dlg_lbl_operation);
        lblState        = (TextView)findViewById(R.id.tr_details_dlg_lbl_state);
		lblID 			= (TextView)findViewById(R.id.tr_details_dlg_lbl_id);
		lblInvoice 		= (TextView)findViewById(R.id.tr_details_dlg_lbl_invoice);
        lblAppcode 		= (TextView)findViewById(R.id.tr_details_dlg_lbl_appcode);
        lblTerminal 	= (TextView)findViewById(R.id.tr_details_dlg_lbl_terminal);
        lblIIN  		= (TextView)findViewById(R.id.tr_details_dlg_lbl_iin);
		lblPAN 			= (TextView)findViewById(R.id.tr_details_dlg_lbl_pan);
        lblEMV          = (TextView)findViewById(R.id.tr_details_dlg_lbl_emv);
        lblLink         = (TextView)findViewById(R.id.tr_details_dlg_lbl_link);
		lblSignature 	= (TextView)findViewById(R.id.tr_details_dlg_lbl_signature);
        lblFiscalStatus	= (TextView)findViewById(R.id.tr_details_dlg_lbl_fiscal_status);
		btnAdjust 		= (Button)findViewById(R.id.tr_details_dlg_btn_adjust);
        btnFiscalStatus = (Button)findViewById(R.id.tr_details_dlg_btn_fiscal_status);
        btnInvoice      = (Button)findViewById(R.id.tr_details_dlg_btn_invoice);
	}

	private void update(TransactionItem transactionItem) {
        lblOperation.setText(transactionItem.getOperation());
        lblState.setText(new StringBuilder(transactionItem.getStateDisplay()).append(" (").append(transactionItem.getSubStateDisplay()).append(")"));
        lblID.setText(transactionItem.getID());
        lblInvoice.setText(transactionItem.getInvoice());
        lblAppcode.setText(transactionItem.getApprovalCode());

        lblIIN.setText(transactionItem.getCard().getIin());
        lblPAN.setText(transactionItem.getCard().getPanMasked().replace("*", " **** "));

        TransactionItem.ExternalPayment externalPayment = transactionItem.getExternalPayment();
        if (externalPayment != null) {
            if (externalPayment.getType() == TransactionItem.ExternalPayment.Type.QR) {
                lblLink.setText("QR: " + Arrays.toString(externalPayment.getQR().toArray()));
            } else if (externalPayment.getType() == TransactionItem.ExternalPayment.Type.LINK) {
                lblLink.setText(externalPayment.getLink());
            }
        }

        String fiscalStatus = "";
        if (transactionItem.getFiscalInfo() != null)
            try {
                fiscalStatus = transactionItem.getFiscalInfo().getJSON().toString(2);
            } catch (Exception e) { e.printStackTrace(); }


        lblFiscalStatus.setText(fiscalStatus);
    }

    private void update(ScheduleItem scheduleItem) {
        lblOperation.setText("SCHEDULE");
        lblState.setText("");
        lblID.setText(String.valueOf(scheduleItem.getID()));
        lblInvoice.setText("");
        lblAppcode.setText("");

        lblIIN.setText(scheduleItem.getCard().getIin());
        lblPAN.setText(scheduleItem.getCard().getPanMasked().replace("*", " **** "));
    }

	private static class CheckFiscalStatusTask extends AsyncTask<String, Void, APITryGetPaymentStatusResult> {
        private ResultDialog parent;
        private ProgressDialog progressDialog;

	    public CheckFiscalStatusTask(ResultDialog parent) {
            this.parent = parent;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(parent.getContext());
            progressDialog.show();
        }

        @Override
        protected APITryGetPaymentStatusResult doInBackground(String... strings) {
            return PaymentController.getInstance().tryGetFiscalInfo(parent.getContext().getApplicationContext(), strings[0]);
        }

        @Override
        protected void onPostExecute(APITryGetPaymentStatusResult result) {
            super.onPostExecute(result);
            progressDialog.dismiss();

            if (result != null && result.isValid() && result.getTransaction() != null) {
                Toast.makeText(parent.getContext(), R.string.success, Toast.LENGTH_LONG).show();
                parent.update(result.getTransaction());
            } else
                Toast.makeText(parent.getContext(), R.string.failed, Toast.LENGTH_LONG).show();

            if (!parent.isShowing())
                parent.show();
        }
    }
}
