package ibox.pro.sdk.external.example.fragments;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import ibox.pro.sdk.external.PaymentController;
import ibox.pro.sdk.external.entities.APIGetHistoryResult;
import ibox.pro.sdk.external.entities.TransactionItem;
import ibox.pro.sdk.external.example.R;
import ibox.pro.sdk.external.example.dialogs.ReversePaymentDialog;

public class FragmentHistory extends Fragment implements ReversePaymentDialog.OnPaymentCancelledListener {

    private EditText edtGetByID;
	private ListView lvContent;
    private boolean updateOnScroll = true;

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

        edtGetByID = (EditText)view.findViewById(R.id.history_edt_getById);
        edtGetByID.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 0) {
                    edtGetByID.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(edtGetByID.getWindowToken(), 0);
                    mLoadHistoryTask = new LoadHistoryTask();
                    mLoadHistoryTask.execute(mPage);
                }
            }
        });
        edtGetByID.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    new FindTransactionByIDTask().execute(edtGetByID.getText().toString());

                    InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(edtGetByID.getWindowToken(), InputMethodManager.RESULT_UNCHANGED_SHOWN);
                    return true;
                }
                return false;
            }
        });

		lvContent = (ListView)view.findViewById(R.id.history_lv_content);
		mAdapter = new HistoryAdapter(getActivity());
		
		lvContent.setOnScrollListener(new AbsListView.OnScrollListener() {
			
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				
			}
			
			@Override
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
				if (updateOnScroll && totalItemCount - visibleItemCount - firstVisibleItem == 0) {
                    if (!isFinished && !isRunning) {
                    	mLoadHistoryTask = new LoadHistoryTask();
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
		
	private void showTransactionDetails(final TransactionItem trItem) {
		View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_history_tr_details, null);
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setView(dialogView);

		AlertDialog dlgTrInfo = builder.create();

        ((EditText)dialogView.findViewById(R.id.history_tr_details_dlg_lbl_id)).setText(String.valueOf(trItem.getID()));
		((TextView)dialogView.findViewById(R.id.history_tr_details_dlg_lbl_date)).setText(String.valueOf(trItem.getDate()));
		((TextView)dialogView.findViewById(R.id.history_tr_details_dlg_lbl_title)).setText(String.valueOf(trItem.getDescription()));
		((TextView)dialogView.findViewById(R.id.history_tr_details_dlg_lbl_amount)).setText(getFormattedAmount(trItem.getAmount(), Locale.getDefault(), trItem.getFormat().getAmountFormat()));
		((TextView)dialogView.findViewById(R.id.history_tr_details_dlg_lbl_amount_eff)).setText(getFormattedAmount(trItem.getAmountEff(), Locale.getDefault(), trItem.getFormat().getAmountFormat()));
		((TextView)dialogView.findViewById(R.id.history_tr_details_dlg_lbl_paytype)).setText(trItem.getCard().getIin());
		((TextView)dialogView.findViewById(R.id.history_tr_details_dlg_lbl_pan)).setText(trItem.getCard().getPanMasked());
		((TextView)dialogView.findViewById(R.id.history_tr_details_dlg_lbl_state)).setText(new StringBuilder(trItem.getStateDisplay()).append(" (").append(trItem.getSubStateDisplay()).append(")"));
		((TextView)dialogView.findViewById(R.id.history_tr_details_dlg_lbl_invoice)).setText(trItem.getInvoice());
		((TextView)dialogView.findViewById(R.id.history_tr_details_dlg_lbl_geodata)).setText(String.valueOf(trItem.getLatitude()).concat(" , ").concat(String.valueOf(trItem.getLongitude())));
		((TextView)dialogView.findViewById(R.id.history_tr_details_dlg_lbl_signature_url)).setText(trItem.getSignatureUrl());
		((TextView)dialogView.findViewById(R.id.history_tr_details_dlg_lbl_photo_url)).setText(trItem.getPhotoUrl());

		((TextView)dialogView.findViewById(R.id.history_tr_details_dlg_lbl_amount)).setTypeface(Typeface.createFromAsset(getActivity().getAssets(), "Roboto-Regular.ttf"));

		int stateColor = getContext().getResources().getColor(android.R.color.white);
		if (trItem.getDisplayMode() != null) {
			if (trItem.getDisplayMode() == TransactionItem.DisplayMode.SUCCESS)
				stateColor = getResources().getColor(android.R.color.holo_green_dark);
			if (trItem.getDisplayMode() == TransactionItem.DisplayMode.REVERSE)
				stateColor = getResources().getColor(android.R.color.darker_gray);
			if (trItem.getDisplayMode() == TransactionItem.DisplayMode.DECLINED)
				stateColor = getResources().getColor(android.R.color.holo_red_light);
			if (trItem.getDisplayMode() == TransactionItem.DisplayMode.REVERSED) {
				stateColor = getResources().getColor(android.R.color.darker_gray);
			}
		}
		((TextView)dialogView.findViewById(R.id.history_tr_details_dlg_lbl_state)).setText(new StringBuilder(trItem.getStateDisplay()).append(" (").append(trItem.getSubStateDisplay()).append(")"));
		((TextView)dialogView.findViewById(R.id.history_tr_details_dlg_lbl_state)).setTextColor(stateColor);

		dialogView.findViewById(R.id.history_tr_details_dlg_btn_cancel).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				new ReversePaymentDialog(getActivity(), trItem.getID(), PaymentController.ReverseAction.CANCEL, FragmentHistory.this).show();
			}
		});
		dialogView.findViewById(R.id.history_tr_details_dlg_btn_return).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				new ReversePaymentDialog(getActivity(), trItem.getID(), PaymentController.ReverseAction.RETURN, FragmentHistory.this).show();
			}
		});

		dialogView.findViewById(R.id.history_tr_details_dlg_btn_cancel).setVisibility(trItem.canCancel() || trItem.canCancelPartial() ? View.VISIBLE : View.GONE);
		dialogView.findViewById(R.id.history_tr_details_dlg_btn_return).setVisibility(trItem.canReturn() || trItem.canReturnPartial() ? View.VISIBLE : View.GONE);

		dlgTrInfo.show();
	}

	@Override
	public void onPaymentCancelled() {
		mPage = 1;
        mAdapter.clear();
        mAdapter.notifyDataSetChanged();
		mLoadHistoryTask = new LoadHistoryTask();
		mLoadHistoryTask.execute(mPage);
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

			int stateColor = getContext().getResources().getColor(android.R.color.black);
			if (getItem(position).getDisplayMode() != null) {
				if (getItem(position).getDisplayMode() == TransactionItem.DisplayMode.SUCCESS)
					stateColor = getResources().getColor(android.R.color.holo_green_dark);
				if (getItem(position).getDisplayMode() == TransactionItem.DisplayMode.REVERSE)
					stateColor = getResources().getColor(android.R.color.darker_gray);
				if (getItem(position).getDisplayMode() == TransactionItem.DisplayMode.DECLINED)
					stateColor = getResources().getColor(android.R.color.holo_red_light);
				if (getItem(position).getDisplayMode() == TransactionItem.DisplayMode.REVERSED) {
					stateColor = getResources().getColor(android.R.color.darker_gray);
				}
			}
			holder.lblTitle.setTextColor(stateColor);
			holder.lblAmount.setTextColor(stateColor);
			
			holder.lblAmount.setText(getFormattedAmount(getItem(position).getAmountEff(), Locale.getDefault(), getItem(position).getFormat().getAmountFormat()));
		}
		
		private class ItemHolder {
			private TextView lblTitle, lblAmount;
		}
		
	}
	
	private class LoadHistoryTask extends AsyncTask<Integer, Void, APIGetHistoryResult> {
		
		private ProgressDialog pDialog;
		
		public LoadHistoryTask() {
			pDialog = new ProgressDialog(getActivity());
			pDialog.setCancelable(false);
		}
		
		@Override
		protected void onPreExecute() {
			isRunning = true;
			pDialog.show();
            updateOnScroll = true;
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

    private class FindTransactionByIDTask extends AsyncTask<String, Void, APIGetHistoryResult> {

        private ProgressDialog pDialog;

        public FindTransactionByIDTask() {
            pDialog = new ProgressDialog(getActivity());
            pDialog.setCancelable(false);
        }

        @Override
        protected void onPreExecute() {
            updateOnScroll = false;
            mPage = 1;
            mAdapter.clear();
            mAdapter.notifyDataSetChanged();
            isRunning = true;
            pDialog.show();
        }

        @Override
        protected APIGetHistoryResult doInBackground(String ... params) {
            return PaymentController.getInstance().getTransactionByID(getActivity(), params[0]);
        }

        @Override
        protected void onPostExecute(APIGetHistoryResult result) {
            try {
                if (result != null) {
                    if (result.isValid()) {
                        for (TransactionItem transaction : result.getTransactions())
                            mAdapter.add(transaction);

                        mAdapter.notifyDataSetChanged();

                    } else {
                        Toast.makeText(getActivity(), result.getErrorMessage(), Toast.LENGTH_LONG).show();
                    }
                } else
                    Toast.makeText(getActivity(), R.string.error_no_response, Toast.LENGTH_LONG).show();
            } finally {
                isRunning = false;
                isFinished = false;
                pDialog.dismiss();
            }
        }


    }

}
