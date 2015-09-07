package ibox.pro.sdk.external.example.fragments;

import ibox.pro.sdk.external.PaymentController;
import ibox.pro.sdk.external.entities.APIGetHistoryResult;
import ibox.pro.sdk.external.entities.TransactionItem;
import ibox.pro.sdk.external.example.R;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class FragmentHistory extends Fragment {

	private ListView lvContent;

	private HistoryAdapter mAdapter;
	private LoadHistoryTask mLoadHistoryTask;
	
	private int mPage = 1;
	private boolean isFinished, isRunning;
	private Typeface amountTypeface;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		amountTypeface = Typeface.createFromAsset(getActivity().getAssets(), "Roboto-Regular.ttf");
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_history, container, false);
				
		mPage = 1;
		
		lvContent = (ListView)view.findViewById(R.id.history_lv_content);
		mAdapter = new HistoryAdapter(getActivity());
		
		lvContent.setOnScrollListener(new AbsListView.OnScrollListener() {
			
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				
			}
			
			@Override
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
				if (totalItemCount - visibleItemCount - firstVisibleItem == 0) {
                    if (!isFinished && !isRunning) {
                    	mLoadHistoryTask = new LoadHistoryTask(getActivity());
                    	mLoadHistoryTask.execute(mPage);
                    }
                }
			}
		});
		
		lvContent.setAdapter(mAdapter);
		
		lvContent.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				showTransactionDetails(mAdapter.getItem(position));
			}
		});
				
		return view;
	}
		
	private void showTransactionDetails(TransactionItem trItem) {
		View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_history_tr_details, null);
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setView(dialogView);
		
		AlertDialog dlgTrInfo = builder.create();
		
		((TextView)dialogView.findViewById(R.id.history_tr_details_dlg_lbl_date)).setText(String.valueOf(trItem.getDate()));
		((TextView)dialogView.findViewById(R.id.history_tr_details_dlg_lbl_title)).setText(String.valueOf(trItem.getDescription()));
		((TextView)dialogView.findViewById(R.id.history_tr_details_dlg_lbl_amount)).setText(getFormattedAmount(trItem.getAmount(), Locale.getDefault(), trItem.getFormat().getAmountFormat()));
		((TextView)dialogView.findViewById(R.id.history_tr_details_dlg_lbl_paytype)).setText(trItem.getCard().getIin());
		((TextView)dialogView.findViewById(R.id.history_tr_details_dlg_lbl_pan)).setText(trItem.getCard().getPanMasked());
		((TextView)dialogView.findViewById(R.id.history_tr_details_dlg_lbl_state)).setText(trItem.getStateDisplay());
		((TextView)dialogView.findViewById(R.id.history_tr_details_dlg_lbl_invoice)).setText(trItem.getInvoice());
		((TextView)dialogView.findViewById(R.id.history_tr_details_dlg_lbl_geodata)).setText(String.valueOf(trItem.getLatitude()).concat(" , ").concat(String.valueOf(trItem.getLongitude())));
		((TextView)dialogView.findViewById(R.id.history_tr_details_dlg_lbl_signature_url)).setText(trItem.getSignatureUrl());
		((TextView)dialogView.findViewById(R.id.history_tr_details_dlg_lbl_photo_url)).setText(trItem.getPhotoUrl());		
		
		((TextView)dialogView.findViewById(R.id.history_tr_details_dlg_lbl_amount)).setTypeface(Typeface.createFromAsset(getActivity().getAssets(), "Roboto-Regular.ttf"));
		
		dlgTrInfo.show();
	}
	
	private String getFormattedAmount(Double amnt, Locale locale, String mask){
    	DecimalFormatSymbols decFormSym;
    	DecimalFormat decFormAWF;
    	decFormSym = new DecimalFormatSymbols(locale);
		decFormAWF = new DecimalFormat(mask, decFormSym);
		decFormAWF.setDecimalSeparatorAlwaysShown(true);
		decFormAWF.setMaximumFractionDigits(2);
		decFormAWF.setMinimumFractionDigits(2);
		return decFormAWF.format(amnt);
    }
			
	private class HistoryAdapter extends ArrayAdapter<TransactionItem> {
								
		public HistoryAdapter(Context context) {
			super(context, 0);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null)
				convertView = newView(position, parent);
			bindView (convertView, position);
			
			return convertView;
		}
		
		private View newView(int position, ViewGroup parent) {
			View result = LayoutInflater.from(getContext()).inflate(R.layout.item_history, parent, false);
			
			ItemHolder holder = new ItemHolder();
			holder.lblTitle = (TextView)result.findViewById(R.id.history_item_lbl_title);
			holder.lblAmount = (TextView)result.findViewById(R.id.history_item_lbl_amount);
			holder.lblAmount.setTypeface(amountTypeface);
			result.setTag(holder);
		
			return result;
		}
		
		private void bindView(View convertView, int position) {
			ItemHolder holder = (ItemHolder)convertView.getTag();
			
			if (getItem(position).getDescription().length() > 0) {
				holder.lblTitle.setText(getItem(position).getDescription());
				holder.lblTitle.setTextColor(Color.BLACK);
			} else {
				holder.lblTitle.setText(getString(R.string.history_item_no_description));
				holder.lblTitle.setTextColor(Color.LTGRAY);
			}		
			
			if (!getItem(position).isNotCanceled()) {
				holder.lblTitle.setPaintFlags(holder.lblTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
				holder.lblAmount.setPaintFlags(holder.lblTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
			} else { 
				holder.lblTitle.setPaintFlags(0);
				holder.lblAmount.setPaintFlags(0);
			}
			
			holder.lblAmount.setText(getFormattedAmount(getItem(position).getAmount(), Locale.getDefault(), getItem(position).getFormat().getAmountFormat()));
		}
		
		private class ItemHolder {
			private TextView lblTitle, lblAmount;
		}
		
	}
	
	private class LoadHistoryTask extends AsyncTask<Integer, Void, APIGetHistoryResult> {
		
		private ProgressDialog pDialog;
		
		public LoadHistoryTask(Context context) {
			pDialog = new ProgressDialog(getActivity());
			pDialog.setCancelable(false);
		}
		
		@Override
		protected void onPreExecute() {
			isRunning = true;
			pDialog.show();
		}
		
		@Override
		protected APIGetHistoryResult doInBackground(Integer ... params) {						
			return PaymentController.getInstance().getHistory(getActivity(), params[0]);
		}
		
		@Override
		protected void onPostExecute(APIGetHistoryResult result) {
			try {
				if (result != null) {
					if (result.isValid()) {
						if (result.getTransactions().isEmpty()) {
							isFinished = true;
							return;
						}
						
						for (TransactionItem transaction : result.getTransactions())
							mAdapter.add(transaction);					

						mPage ++;
						
						mAdapter.notifyDataSetChanged();
	
					} else {
						Toast.makeText(getActivity(), result.getErrorMessage(), Toast.LENGTH_LONG).show();
					}
				} else 
					Toast.makeText(getActivity(), R.string.error_no_response, Toast.LENGTH_LONG).show();
			} finally {
				isRunning = false;
				pDialog.dismiss();
			}
		}

	}

}
