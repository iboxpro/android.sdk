package ibox.pro.sdk.external.example.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import ibox.pro.sdk.external.PaymentContext;
import ibox.pro.sdk.external.PaymentController;
import ibox.pro.sdk.external.PaymentController.RegularEndType;
import ibox.pro.sdk.external.PaymentController.RegularRepeatType;
import ibox.pro.sdk.external.RegularPaymentContext;
import ibox.pro.sdk.external.example.BitmapUtils;
import ibox.pro.sdk.external.example.Consts;
import ibox.pro.sdk.external.example.R;
import ibox.pro.sdk.external.example.dialogs.FiscalDialog;
import ibox.pro.sdk.external.example.dialogs.PaymentDialog;

public class FragmentPayment extends Fragment {

	private SimpleDateFormat mDateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.ENGLISH);
	
	private ImageView imgPhoto, imgProductField_2;
	private EditText edtAmount, edtDescription;
	private Button btnPay, btnFiscal;
	private byte [] photo, productPhoto;
	
	private LinearLayout llProduct, llRegular;
	private CheckBox cbCash, cbProduct, cbRegular, cbEndType;
	private DatePicker pkrStart, pkrEnd;
	private EditText edtProductField_1, edtPhone, edtEmail, edtRepeatCount, edtHour, edtMinute, edtDates;
	private Spinner spnRegularType, spnQuarterly, spnMonth, spnDay, spnDayOfWeek;
	private TextView lblStart, lblEnd, lblQuarterly, lblMonth, lblDay, lblDayOfWeek, lblDates;	
	
	private AlertDialog dlgPhoto, dlgProductPhoto;
		
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_payment, container, false);
		
		initControls(view);
	
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
					doSinglePayment();
			}
		});

		btnFiscal.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				new FiscalDialog(getActivity()).show();
			}
		});

		return view;
	}
		
	private void initControls(View view) {
		imgPhoto 		= (ImageView)view.findViewById(R.id.payment_img_photo);
		edtAmount 		= (EditText)view.findViewById(R.id.payment_edt_amount);
		edtDescription 	= (EditText)view.findViewById(R.id.payment_edt_description);

		cbCash				= (CheckBox)view.findViewById(R.id.payment_cb_cash);
		cbProduct			= (CheckBox)view.findViewById(R.id.payment_cb_product);
		llProduct			= (LinearLayout)view.findViewById(R.id.payment_ll_product);
		edtProductField_1 	= (EditText)view.findViewById(R.id.payment_edt_product_field_1); 
		imgProductField_2 	= (ImageView)view.findViewById(R.id.payment_img_product_field_2); 
		
		llRegular 		= (LinearLayout)view.findViewById(R.id.payment_ll_regular);
		edtPhone 		= (EditText)view.findViewById(R.id.payment_edt_phone);
		edtEmail 		= (EditText)view.findViewById(R.id.payment_edt_email);
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
		btnFiscal		= (Button)view.findViewById(R.id.payment_btn_fiscal);
		
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
	
	private void doSinglePayment() {
		PaymentContext context = new PaymentContext();

		context.setCash(cbCash.isChecked());
		context.setAmount(edtAmount.getText().length() > 0 ? Double.parseDouble(edtAmount.getText().toString()) : 0.0d);
		context.setDescription(edtDescription.getText().toString());
		context.setImage(photo);
		context.setCurrency(PaymentController.Currency.RUB);
		
		if (isPaymentWithProduct()) {
			setPaymentProductData(context);
		}
		
		new PaymentDialog(getActivity(), context).show();
	}
	
	private void doRegularPayment() {
		RegularPaymentContext context = new RegularPaymentContext();
		
		context.setAmount(edtAmount.getText().toString().trim().length() > 0 ? Double.parseDouble(edtAmount.getText().toString()) : 0.0d);
		context.setDescription(edtDescription.getText().toString());
		context.setImage(photo);
		
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
    
}
