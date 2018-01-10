package ibox.pro.sdk.external.example.dialogs;

import ibox.pro.sdk.external.entities.APIResult;
import ibox.pro.sdk.external.PaymentController;
import ibox.pro.sdk.external.ui.SignatureView;
import ibox.pro.sdk.external.example.R;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class AdjustDialog extends Dialog {

	private final String transactionID;
	
	private SignatureView swSignature;
	private EditText edtPhone, edtEmail;
	private Button btnSend;

	public AdjustDialog(Context context, final String transactionID, final boolean isRegular, final boolean isReverse) {
		super(context);
		this.transactionID = transactionID;
		
		getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getWindow().getAttributes().gravity = Gravity.CENTER_VERTICAL;
        setTitle(R.string.adjust_dlg_title);
        setContentView(R.layout.dialog_adjust);

        initControls();
        
        if (isRegular) {
        	edtEmail.setVisibility(View.GONE);
        	findViewById(R.id.adjust_dlg_lbl_email).setVisibility(View.GONE);
        	edtPhone.setVisibility(View.GONE);
        	findViewById(R.id.adjust_dlg_lbl_phone).setVisibility(View.GONE);
        }
        
        btnSend.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String email = edtEmail.getText().toString();
				String phone = edtPhone.getText().toString();
				if (email.trim().length() > 0 || phone.trim().length() > 0 || !swSignature.isEmpty()) {
					if (!PaymentController.getInstance().isPaymentInProgress())
						new AdjustTask(email, phone).execute(isRegular, isReverse);
				} else
					Toast.makeText(getContext(), R.string.adjust_dlg_error, Toast.LENGTH_LONG).show();

			}
		});        
	}
	
	private void initControls() {
		swSignature = (SignatureView)findViewById(R.id.adjust_dlg_sw_signature);
		edtEmail = (EditText)findViewById(R.id.adjust_dlg_edt_email);
		edtPhone = (EditText)findViewById(R.id.adjust_dlg_edt_phone);
		btnSend = (Button)findViewById(R.id.adjust_dlg_btn_send);
	}
	
	private class AdjustTask extends AsyncTask<Boolean, Void, APIResult> {
		private ProgressDialog progressDialog = new ProgressDialog(getContext());
		private final String email, phone;

		public AdjustTask(String email, String phone) {
			this.email = email;
			this.phone = phone;
		}

		@Override
		protected void onPreExecute() {
			progressDialog.setMessage(getContext().getString(R.string.progress));
			progressDialog.setCancelable(false);
			progressDialog.show();
		}
		
		@Override
		protected void onCancelled() {
			progressDialog.dismiss();
		}
		
		@Override
		protected APIResult doInBackground(Boolean... params) {
			boolean isRegular = params[0];
			boolean isReverse = params[1];

			if (isRegular)
				return PaymentController.getInstance().adjust(
						getContext(),
						Integer.parseInt(transactionID), 
						swSignature.isEmpty() ? null : swSignature.getBitmapByteArray()
						);
			else if (isReverse)
				return PaymentController.getInstance().adjustReverse(
						getContext(),
						transactionID,
						phone,
						email,
						swSignature.isEmpty() ? null : swSignature.getBitmapByteArray()
						);

			else
				return PaymentController.getInstance().adjust(
						getContext(), 
						transactionID, 
						phone,
						email,
						swSignature.isEmpty() ? null : swSignature.getBitmapByteArray()
						);
		}
		
		@Override
		protected void onPostExecute(APIResult result) {
			progressDialog.dismiss();
			
			if (result != null) {
				if (result.isValid()) {
					dismiss();
				} else {
					Toast.makeText(getContext(), result.getErrorMessage(), Toast.LENGTH_LONG).show();
				}
			} else {
				Toast.makeText(getContext(), R.string.error_no_response, Toast.LENGTH_LONG).show();
			}			
		}
	}
	
}
