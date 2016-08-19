package ibox.pro.sdk.external.example.dialogs;

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

import ibox.pro.sdk.external.PaymentController;
import ibox.pro.sdk.external.entities.APIResult;
import ibox.pro.sdk.external.example.R;

public class FiscalDialog extends Dialog {

    private EditText edtTransaction, edtPrinter, edtDoc, edtCVC, edtShift;
    private Button btnFiscal;

    public FiscalDialog(Context context) {
        super(context);

        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getWindow().getAttributes().gravity = Gravity.CENTER_VERTICAL;
        setContentView(R.layout.dialog_fiscal);

        initControls();

        btnFiscal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendFiscalData();
            }
        });
    }

    private void initControls() {
        edtTransaction      = (EditText)findViewById(R.id.fiscal_dlg_edt_trid);
        edtPrinter          = (EditText)findViewById(R.id.fiscal_dlg_edt_printer);
        edtDoc              = (EditText)findViewById(R.id.fiscal_dlg_edt_doc);
        edtCVC              = (EditText)findViewById(R.id.fiscal_dlg_edt_cvc);
        edtShift            = (EditText)findViewById(R.id.fiscal_dlg_edt_shift);
        btnFiscal           = (Button)findViewById(R.id.fiscal_btn_confirm);
    }

    private void sendFiscalData() {
        new AsyncTask<Void, Void, APIResult>() {

            private String transactionID;
            private int printerID, docID, CVC, shift;
            private ProgressDialog progressDialog;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();

                transactionID = edtTransaction.getText().toString();
                try {
                    printerID   = Integer.parseInt(edtPrinter.getText().toString());
                    docID       = Integer.parseInt(edtDoc.getText().toString());
                    CVC         = Integer.parseInt(edtCVC.getText().toString());
                    shift       = Integer.parseInt(edtShift.getText().toString());
                } catch (NumberFormatException e) {
                    cancel(true);
                    Toast.makeText(getContext(), getContext().getString(R.string.common_error), Toast.LENGTH_LONG).show();
                    return;
                }
                progressDialog = new ProgressDialog(getContext());
                progressDialog.setMessage(getContext().getString(R.string.progress));
                progressDialog.setCancelable(false);
                progressDialog.show();
            }

            @Override
            protected APIResult doInBackground(Void... params) {
                return PaymentController.getInstance().submitFiscal(getContext(),
                        transactionID,
                        printerID, docID, CVC, shift);
            }

            @Override
            protected void onPostExecute(APIResult result) {
                super.onPostExecute(result);
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
        }.execute();
    }

}
