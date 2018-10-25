package ibox.pro.sdk.external.example.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import ibox.pro.sdk.external.PaymentContext;
import ibox.pro.sdk.external.PaymentController;
import ibox.pro.sdk.external.PaymentController.RegularEndType;
import ibox.pro.sdk.external.PaymentController.RegularRepeatType;
import ibox.pro.sdk.external.PaymentException;
import ibox.pro.sdk.external.PaymentResultContext;
import ibox.pro.sdk.external.RegularPaymentContext;
import ibox.pro.sdk.external.ReversePaymentContext;
import ibox.pro.sdk.external.entities.LinkedCard;
import ibox.pro.sdk.external.entities.Purchase;
import ibox.pro.sdk.external.example.BitmapUtils;
import ibox.pro.sdk.external.example.Consts;
import ibox.pro.sdk.external.example.MainActivity;
import ibox.pro.sdk.external.example.R;
import ibox.pro.sdk.external.example.dialogs.FiscalDialog;
import ibox.pro.sdk.external.example.dialogs.LinkedCardsDialog;
import ibox.pro.sdk.external.example.dialogs.PaymentDialog;

public class FragmentPayment extends Fragment {

	static final Purchase TEST_PRODUCT = Purchase.Build(
		"Тестовый продукт"	,
		111.256d,
			2d,
			Arrays.asList(new String [] { Purchase.TaxCode.VAT_18})
	);

	private SimpleDateFormat mDateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.ENGLISH);
	
	private ImageView imgPhoto, imgProductField_2;
	private EditText edtAmount, edtDescription, edtERN, edtPhone, edtEmail;
	private Button btnPay, btnFiscal, btnPayNFC, btnPayBackground, btnCreditVoucher;
	private byte [] photo, productPhoto;
	private LinkedCard selectedLinkedCard = null;
	
	private LinearLayout llProduct, llRegular;
	private CheckBox cbSuppressSignature, cbProduct, cbRegular, cbEndType, cbAuxData;
	private RadioGroup rgInput;
	private DatePicker pkrStart, pkrEnd;
	private EditText edtProductField_1, edtRepeatCount, edtHour, edtMinute, edtDates;
	private Spinner spnRegularType, spnQuarterly, spnMonth, spnDay, spnDayOfWeek;
	private TextView lblStart, lblEnd, lblQuarterly, lblMonth, lblDay, lblDayOfWeek, lblDates;	
	
	private AlertDialog dlgPhoto, dlgProductPhoto;
	private ArrayAdapter<Purchase> purchasesAdapter;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		purchasesAdapter = new ArrayAdapter<Purchase>(getContext(), android.R.layout.test_list_item, new ArrayList<Purchase>());
	}

	@Override
	public void onResume() {
		super.onResume();

		edtERN.setEnabled(PaymentController.getInstance().getReaderType() != null && PaymentController.getInstance().getReaderType().isTTK());
		rgInput.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			int oldCheckedIndex = -1;

			@Override
			public void onCheckedChanged(final RadioGroup group, int checkedId) {
				if (checkedId > 0 && ((RadioButton) group.findViewById(checkedId)).isChecked()) {
					if (group.indexOfChild(group.findViewById(checkedId)) == 4) {
						List<LinkedCard> linkedCards = ((MainActivity) getActivity()).LinkedCards;

						if (linkedCards != null && linkedCards.size() > 0) {
							LinkedCardsDialog linkedCardsDialog = new LinkedCardsDialog(getActivity(), new LinkedCardsDialog.Listener() {
								@Override
								public void onCardSelected(LinkedCard card) {
									oldCheckedIndex = 4;
									selectedLinkedCard = card;
									updateLinkedCardRBText();
								}
							});
							linkedCardsDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
								@Override
								public void onCancel(DialogInterface dialog) {
									rgInput.check(oldCheckedIndex >= 0 ? group.getChildAt(oldCheckedIndex).getId() : oldCheckedIndex);
									selectedLinkedCard = null;
									updateLinkedCardRBText();
								}
							});
							linkedCardsDialog.show();
						} else {
							selectedLinkedCard = null;
							Toast.makeText(getActivity(), R.string.linked_dlg_empty, Toast.LENGTH_LONG).show();
							rgInput.check(oldCheckedIndex);
							updateLinkedCardRBText();
						}
					} else {
						selectedLinkedCard = null;
						updateLinkedCardRBText();
						oldCheckedIndex = group.indexOfChild(group.findViewById(checkedId));
					}
				}
			}
		});
		cbAuxData.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
				if (checked)
					showPurchasesDialog();
				else
					purchasesAdapter.clear();
			}
		});
	}

	@Override
	public void onPause() {
		super.onPause();

		rgInput.setOnCheckedChangeListener(null);
		cbAuxData.setOnCheckedChangeListener(null);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.fragment_payment, container, false);

		initControls(view);
		updateLinkedCardRBText();

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(getString(R.string.payment_photo_dlg_title))
				.setMessage(getString(R.string.payment_photo_dlg_msg))
				.setPositiveButton(getString(R.string.payment_photo_dlg_btn_gallery), new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						getImageFromGallery(imgPhoto);
						dialog.dismiss();
					}
				})
				.setNegativeButton(getString(R.string.payment_photo_dlg_btn_camera), new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						getImageFromCamera(imgPhoto);
						dialog.dismiss();
					}
				});

		dlgPhoto = builder.create();

		builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(getString(R.string.payment_photo_dlg_title))
				.setMessage(getString(R.string.payment_photo_dlg_msg))
				.setPositiveButton(getString(R.string.payment_photo_dlg_btn_gallery), new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						getImageFromGallery(imgProductField_2);
						dialog.dismiss();
					}
				})
				.setNegativeButton(getString(R.string.payment_photo_dlg_btn_camera), new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						getImageFromCamera(imgProductField_2);
						dialog.dismiss();
					}
				});
		dlgProductPhoto = builder.create();

		imgPhoto.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dlgPhoto.show();
			}
		});

		btnPay.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (cbRegular.isChecked())
					doRegularPayment();
				else
					doSinglePayment(false);
			}
		});

		btnPayNFC.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				doSinglePayment(true);
			}
		});

		if (!Build.MANUFACTURER.equalsIgnoreCase("BBPOS"))
			btnPayNFC.setVisibility(View.GONE);

		btnFiscal.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				new FiscalDialog(getActivity()).show();
			}
		});

		btnPayBackground.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				AsyncTask<Void, Void, PaymentResultContext> task = new AsyncTask<Void, Void, PaymentResultContext>() {
					PaymentContext paymentContext = null;

					@Override
					protected void onPreExecute() {
						super.onPreExecute();

						paymentContext = new PaymentContext();

						paymentContext.setAmount(edtAmount.getText().length() > 0 ? Double.parseDouble(edtAmount.getText().toString()) : 0.0d);
						paymentContext.setDescription(edtDescription.getText().toString());
						paymentContext.setImage(photo);
						paymentContext.setCurrency(PaymentController.Currency.RUB);
						paymentContext.setExtID("TEST_APP");
						if (isPaymentWithProduct()) {
							setPaymentProductData(paymentContext);
						}
					}

					@Override
					protected PaymentResultContext doInBackground(Void... voids) {
						return PaymentController.getInstance().submitCash(getActivity(), paymentContext);
					}

					@Override
					protected void onPostExecute(PaymentResultContext paymentResultContext) {
						super.onPostExecute(paymentResultContext);

						if (paymentResultContext != null) {
							Toast.makeText(getActivity(), "Invoice: " + paymentResultContext.getTransactionItem().getInvoice(), Toast.LENGTH_LONG).show();
						}
					}

					;
				}.execute();
			}
		});

		btnCreditVoucher.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				doCreditVoucher();
			}
		});

		return view;
	}

	private void initControls(View view) {
		imgPhoto 		= (ImageView)view.findViewById(R.id.payment_img_photo);
		edtAmount 		= (EditText)view.findViewById(R.id.payment_edt_amount);
		edtDescription 	= (EditText)view.findViewById(R.id.payment_edt_description);
        edtERN       	= (EditText)view.findViewById(R.id.payment_edt_ern);
        edtPhone 		= (EditText)view.findViewById(R.id.payment_edt_phone);
        edtEmail 		= (EditText)view.findViewById(R.id.payment_edt_email);

		rgInput				= (RadioGroup)view.findViewById(R.id.payment_rg_input);
		cbSuppressSignature = (CheckBox)view.findViewById(R.id.payment_cb_suppress_signature);
		cbProduct			= (CheckBox)view.findViewById(R.id.payment_cb_product);
		cbAuxData			= (CheckBox)view.findViewById(R.id.payment_cb_auxdata);
		llProduct			= (LinearLayout)view.findViewById(R.id.payment_ll_product);
		edtProductField_1 	= (EditText)view.findViewById(R.id.payment_edt_product_field_1); 
		imgProductField_2 	= (ImageView)view.findViewById(R.id.payment_img_product_field_2); 
		
		llRegular 		= (LinearLayout)view.findViewById(R.id.payment_ll_regular);
		cbRegular 		= (CheckBox)view.findViewById(R.id.payment_cb_regular);
		cbEndType		= (CheckBox)view.findViewById(R.id.payment_cb_end_type);
		pkrStart 		= (DatePicker)view.findViewById(R.id.payment_pkr_start);
		pkrEnd 			= (DatePicker)view.findViewById(R.id.payment_pkr_end);
		edtRepeatCount 	= (EditText)view.findViewById(R.id.payment_edt_repeat_count);
		edtDates		= (EditText)view.findViewById(R.id.payment_edt_dates);
		edtHour 		= (EditText)view.findViewById(R.id.payment_edt_hour);
		edtMinute 		= (EditText)view.findViewById(R.id.payment_edt_minute);
				
		spnRegularType 	= (Spinner)view.findViewById(R.id.payment_spn_type);
		spnQuarterly	= (Spinner)view.findViewById(R.id.payment_spn_quarter_month);
		spnMonth		= (Spinner)view.findViewById(R.id.payment_spn_month);
		spnDay 			= (Spinner)view.findViewById(R.id.payment_spn_day);
		spnDayOfWeek 	= (Spinner)view.findViewById(R.id.payment_spn_day_of_week);
		
		lblStart 		= (TextView)view.findViewById(R.id.payment_lbl_start);
		lblEnd 			= (TextView)view.findViewById(R.id.payment_lbl_end_type);
		lblQuarterly	= (TextView)view.findViewById(R.id.payment_lbl_quarter_month);
		lblMonth 		= (TextView)view.findViewById(R.id.payment_lbl_month);
		lblDay 			= (TextView)view.findViewById(R.id.payment_lbl_day);
		lblDayOfWeek 	= (TextView)view.findViewById(R.id.payment_lbl_day_of_week);
		lblDates		= (TextView)view.findViewById(R.id.payment_lbl_dates);
		
		btnPay 			= (Button)view.findViewById(R.id.payment_btn_pay);
		btnPayNFC 			= (Button)view.findViewById(R.id.payment_btn_pay_nfc);
		btnFiscal		= (Button)view.findViewById(R.id.payment_btn_fiscal);
		btnPayBackground = (Button)view.findViewById(R.id.payment_btn_pay_background);
		btnCreditVoucher = (Button)view.findViewById(R.id.payment_btn_credit_voucher);
		
		imgProductField_2.setOnClickListener(new View.OnClickListener() {			
			@Override
			public void onClick(View v) {
				dlgProductPhoto.show();
			}
		});
		
		cbProduct.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton paramCompoundButton, boolean checked) {
				llProduct.setVisibility(checked ? View.VISIBLE : View.GONE);
				edtDescription.setEnabled(!checked);
				if (checked)
					edtDescription.setText("");
			}
		});
		
		cbRegular.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {			
			@Override
			public void onCheckedChanged(CompoundButton paramCompoundButton, boolean checked) {
				llRegular.setVisibility(checked ? View.VISIBLE : View.GONE);
			}
		});
		
		cbEndType.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {			
			@Override
			public void onCheckedChanged(CompoundButton paramCompoundButton, boolean endOnCount) {
				lblEnd.setText(endOnCount ? R.string.payment_lbl_repeat_count : R.string.payment_lbl_end);
				pkrEnd.setVisibility(endOnCount ? View.GONE : View.VISIBLE);
				edtRepeatCount.setVisibility(endOnCount ? View.VISIBLE : View.GONE);
			}
		});
		
		final ArrayAdapter<String> typesAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.regular_payment_types));
		typesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spnRegularType.setAdapter(typesAdapter);
		spnRegularType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {		
			@Override
			public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
				PaymentController.RegularRepeatType repeatType = PaymentController.RegularRepeatType.values()[position];
				
				cbEndType.setVisibility(View.GONE);
				lblStart.setVisibility(View.GONE);
				pkrStart.setVisibility(View.GONE);
				lblEnd.setVisibility(View.GONE);
				pkrEnd.setVisibility(View.GONE);
				edtRepeatCount.setVisibility(View.GONE);
				lblQuarterly.setVisibility(View.GONE);
				spnQuarterly.setVisibility(View.GONE);
				lblMonth.setVisibility(View.GONE);
				spnMonth.setVisibility(View.GONE);
				lblDay.setVisibility(View.GONE);
				spnDay.setVisibility(View.GONE);
				lblDayOfWeek.setVisibility(View.GONE);
				spnDayOfWeek.setVisibility(View.GONE);
				lblDates.setVisibility(View.GONE);
				edtDates.setVisibility(View.GONE);
				
				switch (repeatType) {
					case Never :
						lblStart.setVisibility(View.VISIBLE);
						pkrStart.setVisibility(View.VISIBLE);
						break;
					case Weekly :
						cbEndType.setVisibility(View.VISIBLE);
						lblStart.setVisibility(View.VISIBLE);
						pkrStart.setVisibility(View.VISIBLE);
						lblEnd.setVisibility(View.VISIBLE);
						pkrEnd.setVisibility(cbEndType.isChecked() ? View.GONE : View.VISIBLE);
						edtRepeatCount.setVisibility(cbEndType.isChecked() ? View.VISIBLE : View.GONE);
						lblDayOfWeek.setVisibility(View.VISIBLE);
						spnDayOfWeek.setVisibility(View.VISIBLE);
						break;
					case Monthly :
						cbEndType.setVisibility(View.VISIBLE);
						lblStart.setVisibility(View.VISIBLE);
						pkrStart.setVisibility(View.VISIBLE);
						lblEnd.setVisibility(View.VISIBLE);
						pkrEnd.setVisibility(cbEndType.isChecked() ? View.GONE : View.VISIBLE);
						edtRepeatCount.setVisibility(cbEndType.isChecked() ? View.VISIBLE : View.GONE);
						lblDay.setVisibility(View.VISIBLE);
						spnDay.setVisibility(View.VISIBLE);
						break;
					case Quarterly : 
						cbEndType.setVisibility(View.VISIBLE);
						lblStart.setVisibility(View.VISIBLE);
						pkrStart.setVisibility(View.VISIBLE);
						lblEnd.setVisibility(View.VISIBLE);
						pkrEnd.setVisibility(cbEndType.isChecked() ? View.GONE : View.VISIBLE);
						edtRepeatCount.setVisibility(cbEndType.isChecked() ? View.VISIBLE : View.GONE);
						lblQuarterly.setVisibility(View.VISIBLE);
						spnQuarterly.setVisibility(View.VISIBLE);
						lblDay.setVisibility(View.VISIBLE);
						spnDay.setVisibility(View.VISIBLE);
						break;
					case Annual : 
						cbEndType.setVisibility(View.VISIBLE);
						lblStart.setVisibility(View.VISIBLE);
						pkrStart.setVisibility(View.VISIBLE);
						lblEnd.setVisibility(View.VISIBLE);
						pkrEnd.setVisibility(cbEndType.isChecked() ? View.GONE : View.VISIBLE);
						edtRepeatCount.setVisibility(cbEndType.isChecked() ? View.VISIBLE : View.GONE);
						lblMonth.setVisibility(View.VISIBLE);
						spnMonth.setVisibility(View.VISIBLE);
						lblDay.setVisibility(View.VISIBLE);
						spnDay.setVisibility(View.VISIBLE);
						break;
					case ArbitraryDates :		
						lblDates.setVisibility(View.VISIBLE);
						edtDates.setVisibility(View.VISIBLE);
						break;
					default :
						break;
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> paramAdapterView) {
				
			}
		});
		
		String [] quarterMonth = new String [] {"1", "2", "3"};
		ArrayAdapter<String> quarterlyMonthAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, quarterMonth);
		quarterlyMonthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spnQuarterly.setAdapter(quarterlyMonthAdapter);
		
		DateFormatSymbols dateFormatSymbols = new DateFormatSymbols();
		ArrayAdapter<String> monthAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, dateFormatSymbols.getMonths());
		monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spnMonth.setAdapter(monthAdapter);
	
		String [] weekDays = new String [7];
		String [] localWeekDays = dateFormatSymbols.getWeekdays();
		int firstDay = Calendar.getInstance().getFirstDayOfWeek();
		for (int i = 0; i < weekDays.length; i++)
			weekDays[i] = localWeekDays[(firstDay + i) == 7 ? 7 : (firstDay + i) % 7];
						
		String [] days = new String [32];
		for (int i = 0; i < days.length; i++)
			days[i] = (i == days.length - 1) ? getString(R.string.month_last_day) : String.valueOf(i + 1); 
		ArrayAdapter<String> dayAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, days);
		dayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spnDay.setAdapter(dayAdapter);
	
		ArrayAdapter<String> dayOfWeekAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, weekDays);
		dayOfWeekAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spnDayOfWeek.setAdapter(dayOfWeekAdapter);
	}

	private void updateLinkedCardRBText() {
		((RadioButton) rgInput.getChildAt(4))
				.setText(String.format(getString(R.string.payment_rb_linked_card_format),
						selectedLinkedCard == null ? "" : selectedLinkedCard.getAlias()));
	}

	private void doSinglePayment(boolean NFCOnly) {
		final PaymentContext context = new PaymentContext();

		switch (rgInput.indexOfChild(rgInput.findViewById(rgInput.getCheckedRadioButtonId()))) {
			case 1:
				context.setMethod(PaymentController.PaymentMethod.CASH);
				break;
			case 2:
				context.setMethod(PaymentController.PaymentMethod.CREDIT);
				break;
			case 3:
				context.setMethod(PaymentController.PaymentMethod.OTHER);
				break;
			case 4:
				context.setMethod(PaymentController.PaymentMethod.LINKED_CARD);
				context.setLinkedCardID(selectedLinkedCard.getID());
				break;
			case 5:
				context.setMethod(PaymentController.PaymentMethod.OUTER_CARD);
				break;
			case 6:
				context.setMethod(PaymentController.PaymentMethod.PREPAID);
				break;
			default:
				context.setMethod(PaymentController.PaymentMethod.CARD);
				break;
		}
		context.setAmount(edtAmount.getText().length() > 0 ? Double.parseDouble(edtAmount.getText().toString()) : 0.0d);
		if (context.isCash())
			context.setAmountCashGot(context.getAmount() + 1d);
		context.setDescription(edtDescription.getText().toString());
		context.setImage(photo);
		context.setCurrency(PaymentController.Currency.RUB);
		context.setNFC(NFCOnly);
		context.setReceiptPhone(edtPhone.getText().toString());
		context.setReceiptEmail(edtEmail.getText().toString());
		context.setExtID("TEST_APP");
		context.setSuppressSignatureWaiting(cbSuppressSignature.isChecked());

		if (PaymentController.getInstance().getReaderType() != null && PaymentController.getInstance().getReaderType().isTTK())
		    context.setErn(Integer.parseInt(edtERN.getText().toString()));

		if (cbAuxData.isChecked())
			for (int i = 0; i < purchasesAdapter.getCount(); i++)
				context.putPurchase(purchasesAdapter.getItem(i));

		if (isPaymentWithProduct())
			setPaymentProductData(context);

		HashMap<PaymentController.PaymentMethod, Map<String, String>> acquirersByMethods = ((MainActivity) getActivity()).AcquirersByMethods;
		if (acquirersByMethods != null) {
			if (acquirersByMethods.containsKey(context.getMethod())) {
				final Map<String, String> acquirers = acquirersByMethods.get(context.getMethod());
				final Object [] acuirersArray =  acquirers.entrySet().toArray();
				if (acquirers != null && acquirers.size() > 0) {
					if (acquirers.size() == 1) {
						context.setAcquirerCode(((Map.Entry<String, String>) acuirersArray[0]).getKey());
						new PaymentDialog(getActivity(), context).show();
					} else {
						CharSequence [] items = new CharSequence[acquirers.size()];
						int i = 0;
						for (Object acquirer : acuirersArray)
							items [i++] = ((Map.Entry<String, String>) acquirer).getValue();

						AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
							.setSingleChoiceItems(items, 0, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialogInterface, int i) {
									context.setAcquirerCode(((Map.Entry<String, String>) acuirersArray[i]).getKey());
									dialogInterface.dismiss();
									new PaymentDialog(getActivity(), context).show();
								}
							});
						builder.create().show();
					}
				} else
					new PaymentDialog(getActivity(), context).show();
			} else
				new PaymentDialog(getActivity(), context).show();
		} else
			new PaymentDialog(getActivity(), context).show();
	}
	
	private void doRegularPayment() {
		RegularPaymentContext context = new RegularPaymentContext();
		
		context.setAmount(edtAmount.getText().toString().trim().length() > 0 ? Double.parseDouble(edtAmount.getText().toString()) : 0.0d);
		context.setCurrency(PaymentController.Currency.RUB);
		context.setDescription(edtDescription.getText().toString());
		context.setImage(photo);
		context.setExtID("TEST_APP");
		
		context.setReceiptPhone(edtPhone.getText().toString());
		context.setReceiptEmail(edtEmail.getText().toString());
		context.setRepeatType(PaymentController.RegularRepeatType.values()[spnRegularType.getSelectedItemPosition()]);
		
		Calendar startDate = Calendar.getInstance();
	    startDate.set(pkrStart.getYear(), pkrStart.getMonth(), pkrStart.getDayOfMonth());
		context.setStartDate(startDate.getTime());
		
		context.setEndType(cbEndType.isChecked() ? RegularEndType.BY_QUANTITY : RegularEndType.AT_DAY);
		
		Calendar endDate = Calendar.getInstance();
		endDate.set(pkrEnd.getYear(), pkrEnd.getMonth(), pkrEnd.getDayOfMonth());
		context.setEndDate(endDate.getTime());
		
		if (edtRepeatCount.getText().toString().trim().length() > 0)
			context.setRepeatCount(Integer.parseInt(edtRepeatCount.getText().toString()));
		context.setMonth(context.getRepeatType() == RegularRepeatType.Quarterly 
				? (spnQuarterly.getSelectedItemPosition() + 1) 
				: (spnMonth.getSelectedItemPosition() + 1));
		context.setDay(spnDay.getSelectedItemPosition() + 1);
		
		int firstDay = Calendar.getInstance().getFirstDayOfWeek();
		int selectedLocalDay = spnDayOfWeek.getSelectedItemPosition();
		
		context.setDayOfWeek((selectedLocalDay - 1 + firstDay) % 7);
		
		if (edtHour.getText().toString().trim().length() > 0)
			context.setHour(Integer.parseInt(edtHour.getText().toString()));
		
		if (edtMinute.getText().toString().trim().length() > 0)
			context.setMinute(Integer.parseInt(edtMinute.getText().toString()));
		
		if (edtDates.getText().toString().trim().length() > 0) {
			String [] dates = edtDates.getText().toString().split(";");
			ArrayList<Date> arbitraryDays = new ArrayList<Date>(dates.length);
			for (int i = 0; i < dates.length; i++) {
				try {
					arbitraryDays.add(mDateFormat.parse(dates[i].trim()));
				} catch (ParseException e) {
					
				}
			}
			context.setArbitraryDays(arbitraryDays);
		}
		
		new PaymentDialog(getActivity(), context).show();
	}

	private void doCreditVoucher() {
		final ReversePaymentContext context = new ReversePaymentContext();
		context.setReturnAmount(edtAmount.getText().length() > 0 ? Double.parseDouble(edtAmount.getText().toString()) : 0.0d);
		context.setNFC(false);
		context.setReceiptPhone(edtPhone.getText().toString());
		context.setReceiptEmail(edtEmail.getText().toString());
		context.setExtID("TEST_APP");
		context.setErn(Integer.parseInt(edtERN.getText().toString()));
		context.setSuppressSignatureWaiting(cbSuppressSignature.isChecked());
		context.setCurrency(PaymentController.Currency.RUB);
		context.setAction(PaymentController.ReverseAction.RETURN);

		if (cbAuxData.isChecked())
			for (int i = 0; i < purchasesAdapter.getCount(); i++)
				context.putPurchase(purchasesAdapter.getItem(i));

		final PaymentController.PaymentMethod creditVoucherMethod = PaymentController.PaymentMethod.CARD;
		HashMap<PaymentController.PaymentMethod, Map<String, String>> acquirersByMethods = ((MainActivity) getActivity()).AcquirersByMethods;
		if (acquirersByMethods != null) {
			if (acquirersByMethods.containsKey(creditVoucherMethod)) {
				final Map<String, String> acquirers = acquirersByMethods.get(creditVoucherMethod);
				final Object [] acuirersArray =  acquirers.entrySet().toArray();
				if (acquirers != null && acquirers.size() > 0) {
					if (acquirers.size() == 1) {
						context.setAcquirerCode(((Map.Entry<String, String>) acuirersArray[0]).getKey());
						new CreditVoucherDialog(getActivity(), context).show();
					} else {
						CharSequence [] items = new CharSequence[acquirers.size()];
						int i = 0;
						for (Object acquirer : acuirersArray)
							items [i++] = ((Map.Entry<String, String>) acquirer).getValue();

						AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
								.setSingleChoiceItems(items, 0, new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialogInterface, int i) {
										context.setAcquirerCode(((Map.Entry<String, String>) acuirersArray[i]).getKey());
										dialogInterface.dismiss();
										new CreditVoucherDialog(getActivity(), context).show();
									}
								});
						builder.create().show();
					}
				} else
					new CreditVoucherDialog(getActivity(), context).show();
			} else
				new CreditVoucherDialog(getActivity(), context).show();
		} else
			new CreditVoucherDialog(getActivity(), context).show();
	}

	private void setPaymentProductData(PaymentContext context) {
		context.setPaymentProductCode(getString(R.string.def_product_code));

		HashMap<String, String> paymentProductTextData = new HashMap<String, String>(1);
		paymentProductTextData.put(getString(R.string.def_product_field_1_code), edtProductField_1.getText().toString());
		context.setPaymentProductTextData(paymentProductTextData);

		HashMap<String, byte []> paymentProductImageData = new HashMap<String, byte[]>(1);
		paymentProductImageData.put(getString(R.string.def_product_field_2_code), productPhoto);
		context.setPaymentProductImageData(paymentProductImageData);
	}
	
	private boolean isPaymentWithProduct() {
		return cbProduct.isChecked();
	}

	private void showPurchasesDialog() {
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());
		dialogBuilder.setPositiveButton(R.string.cards_btn_add, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				showAddPurchaseDialog((AlertDialog) dialogInterface);
			}
		})
		.setNeutralButton(R.string.purchases_dlg_btn_update_amount, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				edtAmount.setText(getAuxPurchasesAmount().toString());
				edtAmount.setSelection(edtAmount.getText().length());
			}
		})
		.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				dialogInterface.dismiss();
			}
		})
		.setAdapter(purchasesAdapter, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(final DialogInterface parent, final int i) {
				new AlertDialog.Builder(getContext())
						.setMessage(String.format(getString(R.string.purchases_dlg_remove_format),  purchasesAdapter.getItem(i).getTitle()))
						.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialogInterface, int j) {
								dialogInterface.dismiss();
								purchasesAdapter.remove(purchasesAdapter.getItem(i));
								purchasesAdapter.notifyDataSetChanged();
								((AlertDialog) parent).show();
							}
						})
						.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialogInterface, int j) {
								dialogInterface.dismiss();
								((AlertDialog) parent).show();
							}
						})
						.create().show();
			}
		})
		.create().show();
	}

	private void showAddPurchaseDialog(final Dialog parent) {
		final EditText edtPurchase = (EditText) getLayoutInflater().inflate(R.layout.dialog_purchase, null);
		edtPurchase.setText(TEST_PRODUCT.toString());
		new AlertDialog.Builder(getContext())
				.setView(edtPurchase)
				.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						try {
							Purchase purchase = new Purchase(new JSONObject(edtPurchase.getText().toString()));
							if (purchase != null) {
								purchasesAdapter.add(purchase);
								dialogInterface.dismiss();
							} else
								Toast.makeText(getContext(), R.string.common_error, Toast.LENGTH_LONG).show();
						} catch (Exception e) {
							e.printStackTrace();
							Toast.makeText(getContext(), R.string.common_error, Toast.LENGTH_LONG).show();
						}
						parent.show();
					}
				})
				.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						dialogInterface.dismiss();
						parent.show();
					}
				}).create().show();
	}

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case Consts.RequestCodes.SELECT_PHOTO :
            case Consts.RequestCodes.PRODUCT_SELECT_PHOTO :
                if (resultCode == Activity.RESULT_OK) {
                	System.gc();
                    String uri = getRealPathFromUri(data.getData());
                    setPaymentContextImage(requestCode == Consts.RequestCodes.SELECT_PHOTO ? imgPhoto : imgProductField_2, uri);

                }
                break;

            case Consts.RequestCodes.PHOTO_CAPTURE :
            case Consts.RequestCodes.PRODUCT_PHOTO_CAPTURE :
                if (resultCode == Activity.RESULT_OK) {
                    if (new File(getCameraImagePath()).exists()) {
                    	System.gc();
                    	setPaymentContextImage(requestCode == Consts.RequestCodes.PHOTO_CAPTURE ? imgPhoto : imgProductField_2, getCameraImagePath());
                    }
                }
                break;

            default:
                break;
        }
    }

    private void getImageFromGallery(ImageView target) {
	    Intent intent = new Intent(Intent.ACTION_PICK);
	    intent.setType("image/*");
	    startActivityForResult(intent, target == imgPhoto ? Consts.RequestCodes.SELECT_PHOTO : Consts.RequestCodes.PRODUCT_SELECT_PHOTO);
	}

	private void getImageFromCamera(ImageView target) {
        if (android.os.Environment.getExternalStorageState().equalsIgnoreCase(android.os.Environment.MEDIA_MOUNTED)) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            File file = new File(getCameraImagePath());
            Uri outputFileUri = Uri.fromFile(file);

            intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
            startActivityForResult(intent, target == imgPhoto ? Consts.RequestCodes.PHOTO_CAPTURE : Consts.RequestCodes.PRODUCT_PHOTO_CAPTURE);
        } else {
            Toast.makeText(getActivity(), getString(R.string.sdcard_needed), Toast.LENGTH_LONG).show();
        }
    }
    
    private String getCameraImagePath() {
        return android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + "/image.tmp";
    }
	
    private String getRealPathFromUri(final Uri contentUri) {
        String[] projection = new String[] { android.provider.MediaStore.MediaColumns.DATA };
        ContentResolver cr = getActivity().getContentResolver();
        Cursor cursor = cr.query(contentUri, projection, null, null, null);
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            int index = cursor.getColumnIndex(android.provider.MediaStore.MediaColumns.DATA);
            return cursor.getString(index);
        }
        return null;
    }
    
    private void setPaymentContextImage(ImageView target, String path) {
        if (path == null) {
        	if (target == imgPhoto)
        		photo = null;
        	else
        		productPhoto = null;
        	
        	target.setImageResource(android.R.color.white);
        }
        else {
            new SetImageTask(target).execute(path);
        }
        
    }

    private BigDecimal getAuxPurchasesAmount() {
		PaymentController.Currency currency = PaymentController.Currency.RUB;
		BigDecimal result = BigDecimal.ZERO.setScale(currency.getE(), RoundingMode.HALF_UP);
		for (int i = 0; i < purchasesAdapter.getCount(); i++) {
			Purchase nextPurchase = purchasesAdapter.getItem(i);
			result = result.add(
					BigDecimal.valueOf(nextPurchase.getPrice()).setScale(currency.getE(), RoundingMode.HALF_UP)
							.multiply(BigDecimal.valueOf(nextPurchase.getQuantity()).setScale(3, RoundingMode.HALF_UP))
			).setScale(currency.getE(), RoundingMode.HALF_UP);
		}
		return result;
	}
    
    private class SetImageTask extends AsyncTask<String, Void, Void> {
    	private ImageView target;
    	private Bitmap bitmap = null;
    	private ProgressDialog pDialog;
    	
    	public SetImageTask(ImageView target) {
    		this.target = target;
    		pDialog = new ProgressDialog(getActivity());
    		pDialog.setCancelable(false);
    	}

        protected Void doInBackground(String... params) {
        	getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					pDialog.show();
				}
			});
        	
            String uri = params[0];
            try {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap = BitmapUtils.compressedBitmap(uri, Consts.Parameters.ImageWidth, Consts.Parameters.ImageHeight);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 60, stream);
                if (target == imgPhoto)
                	photo = stream.toByteArray();
                else
                	productPhoto = stream.toByteArray();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;

        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            
            if (bitmap != null)
            	target.setImageBitmap(bitmap);
            
            pDialog.dismiss();
        }   
    }

    private class CreditVoucherDialog extends PaymentDialog {
		private ReversePaymentContext reversePaymentContext;

		public CreditVoucherDialog(Activity context, ReversePaymentContext reversePaymentContext) {
			super(context);
			this.reversePaymentContext = reversePaymentContext;
		}

		@Override
		protected void action() throws PaymentException {
			PaymentController.getInstance().reversePayment(getContext(), reversePaymentContext);
		}

		@Override
		protected boolean usesReader() {
			return true;
		}

		@Override
		protected int getReadyStringID() {
			return PaymentController.getInstance().getReaderType().isMultiInputSupported()
					? R.string.reader_state_ready_multiinput
					: reversePaymentContext.getNFC() ? R.string.reader_state_ready_nfconly : R.string.reader_state_ready;
		}

		@Override
		public void onTransactionStarted(String transactionID) {
			startProgress();
			lblState.setText(String.format(getContext().getString(R.string.payment_dlg_started), transactionID == null ? "" : transactionID));
		}
	}
}
