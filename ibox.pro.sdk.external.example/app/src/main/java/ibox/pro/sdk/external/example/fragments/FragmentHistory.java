package ibox.pro.sdk.external.example.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
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
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import ibox.pro.sdk.external.PaymentController;
import ibox.pro.sdk.external.PaymentControllerException;
import ibox.pro.sdk.external.entities.APIGetHistoryResult;
import ibox.pro.sdk.external.entities.APITryGetPaymentStatusResult;
import ibox.pro.sdk.external.entities.TransactionItem;
import ibox.pro.sdk.external.example.CommonAsyncTask;
import ibox.pro.sdk.external.example.MainActivity;
import ibox.pro.sdk.external.example.R;
import ibox.pro.sdk.external.example.Utils;
import ibox.pro.sdk.external.example.dialogs.ResultDialog;
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
		final Dialog dlgTrInfo = new Dialog(getActivity(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
		dlgTrInfo.setContentView(dialogView);

		String fiscalStatus = "";
		if (trItem.getFiscalInfo() != null)
			try {
				fiscalStatus = trItem.getFiscalInfo().getJSON().toString(2);
			} catch (Exception e) { e.printStackTrace(); }

        ((EditText)dialogView.findViewById(R.id.history_tr_details_dlg_lbl_id)).setText(String.valueOf(trItem.getID()));
		((TextView)dialogView.findViewById(R.id.history_tr_details_dlg_lbl_date)).setText(String.valueOf(trItem.getDate()));
		((TextView)dialogView.findViewById(R.id.history_tr_details_dlg_lbl_title)).setText(String.valueOf(trItem.getDescription()));
		((TextView)dialogView.findViewById(R.id.history_tr_details_dlg_lbl_amount)).setText(getFormattedAmount(trItem.getAmount(), Locale.getDefault(), trItem.getFormat().getAmountFormat()));
		((TextView)dialogView.findViewById(R.id.history_tr_details_dlg_lbl_amount_eff)).setText(getFormattedAmount(trItem.getAmountEff(), Locale.getDefault(), trItem.getFormat().getAmountFormat()));
		((TextView)dialogView.findViewById(R.id.history_tr_details_dlg_lbl_paytype)).setText(trItem.getCard().getIin());
		((TextView)dialogView.findViewById(R.id.history_tr_details_dlg_lbl_pan)).setText(trItem.getCard().getPanMasked());
		((TextView)dialogView.findViewById(R.id.history_tr_details_dlg_lbl_state)).setText(new StringBuilder(trItem.getStateDisplay()).append(" (").append(trItem.getSubStateDisplay()).append(")"));
		((TextView)dialogView.findViewById(R.id.history_tr_details_dlg_lbl_fiscal_status)).setText(fiscalStatus);
		((TextView)dialogView.findViewById(R.id.history_tr_details_dlg_lbl_invoice)).setText(trItem.getInvoice());
		((TextView)dialogView.findViewById(R.id.history_tr_details_dlg_lbl_extid)).setText(trItem.getExtID());
		((TextView)dialogView.findViewById(R.id.history_tr_details_dlg_lbl_exttrandata)).setText(trItem.getExtTranData());
		((TextView)dialogView.findViewById(R.id.history_tr_details_dlg_lbl_geodata)).setText(String.valueOf(trItem.getLatitude()).concat(" , ").concat(String.valueOf(trItem.getLongitude())));
		((TextView)dialogView.findViewById(R.id.history_tr_details_dlg_lbl_signature_url)).setText(trItem.getSignatureUrl());
		((TextView)dialogView.findViewById(R.id.history_tr_details_dlg_lbl_photo_url)).setText(trItem.getPhotoUrl());

		((TextView)dialogView.findViewById(R.id.history_tr_details_dlg_lbl_amount)).setTypeface(Typeface.createFromAsset(getActivity().getAssets(), "Roboto-Regular.ttf"));

		TransactionItem.ExternalPayment externalPayment = trItem.getExternalPayment();
		if (externalPayment != null) {
			String text = "";
			if (externalPayment.getType() == TransactionItem.ExternalPayment.Type.QR)
				text = "QR: " + Arrays.toString(externalPayment.getQR().toArray());
			else if (externalPayment.getType() == TransactionItem.ExternalPayment.Type.LINK)
				text = externalPayment.getLink();
			((TextView) dialogView.findViewById(R.id.history_tr_details_dlg_lbl_link)).setText(text);
		}

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

		dialogView.findViewById(R.id.history_tr_details_dlg_btn_invoice).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				String invoice = Utils.BuildInvoice(((MainActivity) getActivity()).Account, trItem);
				Log.i(trItem.getID(), invoice);
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
		dialogView.findViewById(R.id.history_tr_details_dlg_btn_cancel).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				new ReversePaymentDialog(getActivity(), trItem, PaymentController.ReverseAction.CANCEL, FragmentHistory.this).show();
			}
		});
		dialogView.findViewById(R.id.history_tr_details_dlg_btn_return).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				new ReversePaymentDialog(getActivity(), trItem, PaymentController.ReverseAction.RETURN, FragmentHistory.this).show();
			}
		});

		dialogView.findViewById(R.id.history_tr_details_dlg_btn_cancel).setVisibility(trItem.canCancel() || trItem.canCancelPartial() || trItem.canCancelCNP() || trItem.canCancelCNPPartial() ? View.VISIBLE : View.GONE);
		dialogView.findViewById(R.id.history_tr_details_dlg_btn_return).setVisibility(trItem.canReturn() || trItem.canReturnPartial() || trItem.canReturnCNP() || trItem.canReturnCNPPartial() ? View.VISIBLE : View.GONE);

		TransactionItem.FiscalInfo fiscalInfo = trItem.getFiscalInfo();
		boolean fiscalizeRequired = fiscalInfo != null
				&& fiscalInfo.getFiscalStatus() != TransactionItem.FiscalInfo.FiscalStatus.CREATED
				&& fiscalInfo.getFiscalStatus() != TransactionItem.FiscalInfo.FiscalStatus.SUCCESS;
		dialogView.findViewById(R.id.histoty_tr_details_dlg_btn_fiscalize).setVisibility(fiscalizeRequired ? View.VISIBLE : View.GONE);
		dialogView.findViewById(R.id.histoty_tr_details_dlg_btn_fiscalize).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				new FiscalizeTask(dlgTrInfo).execute(trItem.getID());
			}
		});

		try {
			PaymentController.getInstance().initPaymentSession();
			dlgTrInfo.show();
		} catch (PaymentControllerException e) {
			Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
		}
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
		private ArrayList<TransactionItem> inProcessTransactions, finishedTran;

		public HistoryAdapter(Context context) {
			super(context, 0);
			inProcessTransactions = new ArrayList<TransactionItem>();
			finishedTran = new ArrayList<TransactionItem>();
		}

		@Override
		public int getCount() {
			return inProcessTransactions.size() + finishedTran.size();
		}

		@Override
		public void clear() {
			super.clear();
			inProcessTransactions.clear();
			finishedTran.clear();
		}

		@Override
		public TransactionItem getItem(int position) {
			return position < inProcessTransactions.size()
				? inProcessTransactions.get(position)
				: finishedTran.get(position - inProcessTransactions.size());
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
					String description = getItem(position).getDescription();
					if (inProcessTransactions.contains(getItem(position)))
						description += "\n" + getString(R.string.history_item_inprocess);
					holder.lblTitle.setText(description);
					holder.lblTitle.setTextColor(Color.BLACK);
				} else {
					String description = getString(R.string.history_item_no_description);
					if (inProcessTransactions.contains(getItem(position)))
						description += "\n" + getString(R.string.history_item_inprocess);
					holder.lblTitle.setText(description);
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
	
	private class LoadHistoryTask extends CommonAsyncTask<Integer, Void, APIGetHistoryResult> {
		public LoadHistoryTask() {
			super(getActivity());
		}
		
		@Override
		protected void onPreExecute() {
			isRunning = true;
            updateOnScroll = true;
            super.onPreExecute();
		}
		
		@Override
		protected APIGetHistoryResult doInBackground(Integer ... params) {						
			return PaymentController.getInstance().getHistory(getActivity(), params[0]);
		}
		
		@Override
		protected void onPostExecute(APIGetHistoryResult result) {
			super.onPostExecute(result);
			try {
				if (result != null) {
					if (result.isValid()) {
						if (result.getTransactions() == null || result.getTransactions().isEmpty()) {
							isFinished = true;
							return;
						}
						mAdapter.inProcessTransactions.clear();
						if (result.getInProcessTransactions() != null)
							mAdapter.inProcessTransactions.addAll(result.getInProcessTransactions());
						if (result.getTransactions() != null)
							mAdapter.finishedTran.addAll(result.getTransactions());
						mPage ++;
						
						mAdapter.notifyDataSetChanged();
					} else {
						Toast.makeText(getActivity(), result.getErrorMessage(), Toast.LENGTH_LONG).show();
					}
				} else 
					Toast.makeText(getActivity(), R.string.error_no_response, Toast.LENGTH_LONG).show();
			} finally {
				isRunning = false;
			}
		}

	}

    private class FindTransactionByIDTask extends CommonAsyncTask<String, Void, APIGetHistoryResult> {
        public FindTransactionByIDTask() {
            super(getActivity());
        }

        @Override
        protected void onPreExecute() {
            updateOnScroll = false;
            mPage = 1;
            mAdapter.clear();
            mAdapter.notifyDataSetChanged();
            isRunning = true;
            super.onPreExecute();
        }

        @Override
        protected APIGetHistoryResult doInBackground(String ... params) {
            return PaymentController.getInstance().getTransactionByID(getActivity(), params[0]);
        }

        @Override
        protected void onPostExecute(APIGetHistoryResult result) {
        	super.onPostExecute(result);

            try {
                if (result != null) {
                    if (result.isValid()) {
						if (result.getInProcessTransactions() != null)
							mAdapter.inProcessTransactions.addAll(result.getInProcessTransactions());
						if (result.getTransactions() != null)
							mAdapter.finishedTran.addAll(result.getTransactions());

                        mAdapter.notifyDataSetChanged();

                    } else {
                        Toast.makeText(getActivity(), result.getErrorMessage(), Toast.LENGTH_LONG).show();
                    }
                } else
                    Toast.makeText(getActivity(), R.string.error_no_response, Toast.LENGTH_LONG).show();
            } finally {
                isRunning = false;
                isFinished = false;
            }
        }
    }

	private class FiscalizeTask extends CommonAsyncTask<String, Void, APITryGetPaymentStatusResult>{
		private Dialog parent;

		public FiscalizeTask(Dialog parent) {
			super(parent.getContext());
			this.parent = parent;
		}

		@Override
		protected APITryGetPaymentStatusResult doInBackground(String... strings) {
			return PaymentController.getInstance().fiscalize(getContext().getApplicationContext(), strings[0]);
		}

		@Override
		protected void onPostExecute(APITryGetPaymentStatusResult result) {
			super.onPostExecute(result);

			if (result != null && result.isValid() && result.getTransaction() != null) {
				Toast.makeText(parent.getContext(), R.string.success, Toast.LENGTH_LONG).show();
				parent.dismiss();
				showTransactionDetails(result.getTransaction());
			} else
				Toast.makeText(parent.getContext(), R.string.failed, Toast.LENGTH_LONG).show();

			if (!parent.isShowing())
				parent.show();
		}
	}
}
