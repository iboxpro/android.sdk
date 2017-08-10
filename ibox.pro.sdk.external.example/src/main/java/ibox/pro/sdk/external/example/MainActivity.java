package ibox.pro.sdk.external.example;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTabHost;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.Toast;

import ibox.pro.sdk.external.PaymentController;
import ibox.pro.sdk.external.PaymentController.ReaderType;
import ibox.pro.sdk.external.entities.APIAuthResult;
import ibox.pro.sdk.external.entities.APIResult;
import ibox.pro.sdk.external.example.fragments.FragmentHistory;
import ibox.pro.sdk.external.example.fragments.FragmentPayment;
import ibox.pro.sdk.external.example.fragments.FragmentSettings;

public class MainActivity extends FragmentActivity {

	public String BankName;
	public String ClientName;
	public String ClientLegalName;
	public String ClientPhone;
	public String ClientWeb;


	private FragmentTabHost mTabHost;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_main);
		mTabHost = (FragmentTabHost)findViewById(android.R.id.tabhost);
		setupTabHost();

		PaymentController.getInstance().onCreate(this, savedInstanceState);
		if (savedInstanceState == null) {
			String readerType = Utils.getString(this, Consts.SavedParams.READER_TYPE_KEY);
			String readerAddress = Utils.getString(this, Consts.SavedParams.READER_ADDRESS_KEY);

			if (readerType != null && readerType.length() > 0)
				PaymentController.getInstance().setReaderType(this, ReaderType.valueOf(readerType), readerAddress);
		}

		if (Build.MANUFACTURER.equalsIgnoreCase("BBPOS"))
			try {
				PaymentController.getInstance().setReaderType(this, ReaderType.M17, null);
			} catch (IllegalStateException e) {
				e.printStackTrace();
			}
		else if (PaymentController.getInstance().getReaderType() == null)
			try {
				PaymentController.getInstance().setReaderType(this, ReaderType.P16, null);
			} catch (IllegalStateException e) {
				e.printStackTrace();
			}

	 	PaymentController.getInstance().setSingleStepEMV(true);
		if (savedInstanceState == null)
			showLoginDialog();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		PaymentController.getInstance().onSaveInstanceState(outState);
		super.onSaveInstanceState(outState);
	}
	
	@Override
	protected void onDestroy() {
		PaymentController.getInstance().onDestroy();
		super.onDestroy();
	}
	
	private void setupTabHost() {
		mTabHost.setup(this, getSupportFragmentManager(),android.R.id.tabcontent);
		
		mTabHost.addTab(mTabHost.newTabSpec(getString(R.string.tab_payment)).setIndicator(getString(R.string.tab_payment)), 
				FragmentPayment.class, null);
		mTabHost.addTab(mTabHost.newTabSpec(getString(R.string.tab_history)).setIndicator(getString(R.string.tab_history)), 
				FragmentHistory.class, null);
		mTabHost.addTab(mTabHost.newTabSpec(getString(R.string.tab_settings)).setIndicator(getString(R.string.tab_settings)), 
				FragmentSettings.class, null);
	}

	private void showLoginDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.login_dlg_title))
			.setCancelable(false)
			.setPositiveButton(getString(R.string.login_dlg_btn_ok), new DialogInterface.OnClickListener() {				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String login = ((EditText)((AlertDialog)dialog).findViewById(R.id.login_dlg_edt_login)).getText().toString();
					String password = ((EditText)((AlertDialog)dialog).findViewById(R.id.login_dlg_edt_password)).getText().toString();
					PaymentController.getInstance().setCredentials(login, password);

					APIAuthResult result = PaymentController.getInstance().auth(MainActivity.this);

					if (result == null)
						Toast.makeText(MainActivity.this, "Connection lost", Toast.LENGTH_LONG).show();
					else if (!result.isValid())
						Toast.makeText(MainActivity.this, "Invalid credentials", Toast.LENGTH_LONG).show();
					else {
						BankName = result.getAccount().getBankName();
						ClientName = result.getAccount().getClientName();
						ClientLegalName = result.getAccount().getClientLegalName();
						ClientPhone = result.getAccount().getClientPhone();
						ClientWeb = result.getAccount().getClientWeb();
						dialog.dismiss();
					}
				}
			})
			.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					finish();
				}
			})
			.setView(LayoutInflater.from(this).inflate(R.layout.dialog_login, null))
			.setOnCancelListener(new DialogInterface.OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					finish();
				}
			})
			.create()
			.show();
	}
}
