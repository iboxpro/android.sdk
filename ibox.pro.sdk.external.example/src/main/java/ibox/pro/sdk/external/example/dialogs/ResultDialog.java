package ibox.pro.sdk.external.example.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import ibox.pro.sdk.external.PaymentResultContext;
import ibox.pro.sdk.external.entities.ScheduleItem;
import ibox.pro.sdk.external.entities.TransactionItem;
import ibox.pro.sdk.external.example.R;

public class ResultDialog extends Dialog {

	private TextView lblOperation, lblState, lblID, lblInvoice, lblAppcode, lblTerminal,
                lblIIN, lblPAN, lblLink,
                lblEMV, lblSignature;
	private Button btnAdjust;

	public ResultDialog(Context context, PaymentResultContext paymentResultContext, final boolean isReverse) {
		super(context);
		
		getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getWindow().getAttributes().gravity = Gravity.CENTER_VERTICAL;
        setContentView(R.layout.dialog_tr_details);

        initControls();        	

		final TransactionItem transactionItem = paymentResultContext.getTransactionItem();
		final ScheduleItem scheduleItem = paymentResultContext.getScheduleItem();
		final boolean isRegular = scheduleItem != null;
		String cardNumber = isRegular ? scheduleItem.getCard().getPanMasked() : paymentResultContext.getTransactionItem().getCard().getPanMasked();

        lblOperation.setText(isRegular ? "SCHEDULE" : transactionItem.getOperation());
        lblState.setText(isRegular ? "" : new StringBuilder(transactionItem.getStateDisplay()).append(" (").append(transactionItem.getSubStateDisplay()).append(")"));
        lblID.setText(isRegular ? String.valueOf(scheduleItem.getID()) : transactionItem.getID());
        lblInvoice.setText(isRegular ? "" : transactionItem.getInvoice());
        lblAppcode.setText(isRegular ? "" : transactionItem.getApprovalCode());
        lblTerminal.setText(paymentResultContext.getTerminalName());

        lblIIN.setText(isRegular ? scheduleItem.getCard().getIin() : transactionItem.getCard().getIin());
        lblPAN.setText(cardNumber.replace("*", " **** "));
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

        if (!isRegular) {
            TransactionItem.ExternalPayment externalPayment = paymentResultContext.getTransactionItem().getExternalPayment();
            if (externalPayment != null) {
                if (externalPayment.getType() == TransactionItem.ExternalPayment.Type.QR) {
                    lblLink.setText("QR: " + Arrays.toString(externalPayment.getQR().toArray()));
                } else if (externalPayment.getType() == TransactionItem.ExternalPayment.Type.LINK) {
                    lblLink.setText(externalPayment.getLink());
                }
            }
        }

        btnAdjust.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				dismiss();
				new AdjustDialog(getContext(), isRegular ? String.valueOf(scheduleItem.getID()) : transactionItem.getID(), isRegular, isReverse).show();
			}
		});
        
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
		btnAdjust 		= (Button)findViewById(R.id.tr_details_dlg_btn_adjust);
	}

}
