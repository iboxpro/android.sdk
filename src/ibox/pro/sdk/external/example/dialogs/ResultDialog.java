package ibox.pro.sdk.external.example.dialogs;

import ibox.pro.sdk.external.example.R;

import android.app.Dialog;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

public class ResultDialog extends Dialog {

	private TextView lblID, lblInvoice, lblPAN, lblSignature;
	private Button btnAdjsut;

	public ResultDialog(Context context, final boolean isRegular, final String transactionID, String invoice, String cardNumber, boolean requiresSignature) {
		super(context);
		
		getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getWindow().getAttributes().gravity = Gravity.CENTER_VERTICAL;
        setContentView(R.layout.dialog_tr_details);

        initControls();        	
        
        lblID.setText(transactionID);
        lblInvoice.setText(invoice);
        lblPAN.setText(cardNumber.replace("*", " **** **** "));
        lblSignature.setText(String.valueOf(requiresSignature));
        
        btnAdjsut.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				dismiss();
				new AdjustDialog(getContext(), transactionID, isRegular).show();
			}
		});
        
	}
	
	private void initControls() {
		lblID 			= (TextView)findViewById(R.id.tr_details_dlg_lbl_id);
		lblInvoice 		= (TextView)findViewById(R.id.tr_details_dlg_lbl_invoice);
		lblPAN 			= (TextView)findViewById(R.id.tr_details_dlg_lbl_pan);
		lblSignature 	= (TextView)findViewById(R.id.tr_details_dlg_lbl_signature);
		btnAdjsut 		= (Button)findViewById(R.id.tr_details_dlg_btn_adjust);
	}

}
