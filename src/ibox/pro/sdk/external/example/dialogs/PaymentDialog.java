package ibox.pro.sdk.external.example.dialogs;

import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

import ibox.pro.sdk.external.PaymentContext;
import ibox.pro.sdk.external.PaymentController;
import ibox.pro.sdk.external.PaymentController.PaymentError;
import ibox.pro.sdk.external.PaymentControllerListener;
import ibox.pro.sdk.external.PaymentController.ReaderEvent;
import ibox.pro.sdk.external.PaymentException;
import ibox.pro.sdk.external.RegularPaymentContext;
import ibox.pro.sdk.external.example.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class PaymentDialog extends Dialog implements PaymentControllerListener {
	
    private Activity mActivity;
    
    private TextView lblState;
    private ImageView imgSpinner;
    private RotateAnimation spinRotation;
    private AlertDialog dlgSelectApp;
    private RegularStepsDialog dlgSteps;
    
    private PaymentContext mPaymentContext;
    private int mSelectedAppIndex = -1;
    private int mStepsConfirmed = -1;
    private Boolean mStepsRetry = null;
    
    public PaymentDialog(Activity context, PaymentContext paymentContext) {
        super(context);
        mPaymentContext = paymentContext;
        init(context);
        mActivity = context;
    }

    private void init(Context context) {
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_payment);
        getWindow().getAttributes().gravity = Gravity.CENTER_VERTICAL;

        lblState = (TextView) findViewById(R.id.payment_dlg_lbl_state);
        imgSpinner = (ImageView) findViewById(R.id.payment_dlg_spinner);
        
        spinRotation = new RotateAnimation(
        	      0f,
        	      360f,
        	      Animation.RELATIVE_TO_SELF,
        	      0.5f,
        	      Animation.RELATIVE_TO_SELF,
        	      0.5f);
        spinRotation.setDuration(1200);
        spinRotation.setInterpolator(new LinearInterpolator());
        spinRotation.setRepeatMode(Animation.RESTART);
        spinRotation.setRepeatCount(Animation.INFINITE);
        
        PaymentController.getInstance().setPaymentControllerListener(this);
        PaymentController.getInstance().enable();
                    
        try {
			PaymentController.getInstance().startPayment(getContext(), mPaymentContext);
		} catch (PaymentException e) {
			onError(null, e.getMessage());
		}
    }
    
    private void startProgress() {
    	imgSpinner.setVisibility(View.VISIBLE);
    	imgSpinner.startAnimation(spinRotation);
    	lblState.setText(R.string.reader_state_inprogress);
    }
    
    private void stopProgress() {
    	imgSpinner.clearAnimation();
    	imgSpinner.setVisibility(View.GONE);
    }
    
    public PaymentContext getPaymentContext() {
        return mPaymentContext;
    }

    public void setPaymentContext(PaymentContext context) {
        mPaymentContext = context;
    }
    
    @Override
    public void dismiss() {
    	PaymentController.getInstance().disable();
    	stopProgress();
    	super.dismiss();
    }
    
    @Override
    public void onError(final PaymentController.PaymentError error, final String errorMessage) {
    	stopProgress();
    	String toastText = "";
    	switch (error) {
	    	case SERVER_ERROR :
	    		toastText = errorMessage;
	    		break;
	    	case CONNECTION_ERROR :
	    		toastText = mActivity.getString(R.string.error_no_response);
	    		break;
	    	case EMV_NOT_ALLOWED :
	    		toastText = mActivity.getString(R.string.EMV_NOT_ALLOWED);
				break;
	    	case EMV_CANCEL :
		    	toastText = mActivity.getString(R.string.EMV_TRANSACTION_CANCELED);
				break;
	    	case EMV_DECLINED :
	    		toastText = mActivity.getString(R.string.EMV_TRANSACTION_DECLINED);
	    		break;
	    	case EMV_TERMINATED :
	    		toastText = mActivity.getString(R.string.EMV_TRANSACTION_TERMINATED);
	    		break;
	    	case EMV_CARD_ERROR :
	    		toastText = mActivity.getString(R.string.EMV_CARD_ERROR);
	    		break;
	    	case EMV_DEVICE_ERROR :
	    		toastText = mActivity.getString(R.string.EMV_READER_ERROR);
	    		break;    	
	    	case EMV_CARD_BLOCKED :
	    		toastText = mActivity.getString(R.string.EMV_CARD_BLOCKED);
	    		break;
	    	case EMV_CARD_NOT_SUPPORTED :
	    		toastText = mActivity.getString(R.string.EMV_CARD_NOT_SUPPORTED);
	    		break;
	    	default :
	    		toastText = mActivity.getString(R.string.EMV_ERROR);
	    		break;    	
    	}     	
		Toast.makeText(getContext(), toastText, Toast.LENGTH_LONG).show();
		if (error == PaymentController.PaymentError.EMV_TERMINATED)
			dismiss();
    }
    
    @Override
    public void onFinished(String transactionID, String invoice, String cardNumber, boolean requiresSignature) {
    	dismiss();
    	boolean isRegular = mPaymentContext instanceof RegularPaymentContext;
    	new ResultDialog(mActivity, isRegular, transactionID, invoice, cardNumber, requiresSignature).show();
    }
    
    @Override
    public void onReaderEvent(ReaderEvent event) {
    	switch (event) {
	    	case CONNECTED :
	    	case START_INIT :
	    		lblState.setText(R.string.reader_state_init);
	    		break;
	    	case DISCONNECTED :
	    		stopProgress();
	    		lblState.setText(R.string.reader_state_disconnected);
	    		break;
	    	case SWIPE_CARD :
	    	case TRANSACTION_STARTED :
	    		startProgress();
	    		break;	    	
	    	case WAITING_FOR_CARD :
	    		lblState.setText(R.string.reader_state_ready);
	    		break;
	    	case PAYMENT_CANCELED :
	    		dismiss();
	    		break;
	    	case INIT_FAILED : 
	    		stopProgress();
	    		lblState.setText(R.string.reader_state_init_error);
	    		break;
	    	case EJECT_CARD :
	    		stopProgress();
	    		lblState.setText(R.string.reader_state_eject);
	    		break;
	    	default :
	    		break;
    	}
    }
    
    @Override
    public int onSelectApplication(List<String> apps) {
        final String[] array = apps.toArray(new String[apps.size()]);

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
            	AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
            		builder.setCancelable(false)
            				.setTitle("Select application")
            				.setNegativeButton(mActivity.getString(R.string.cancel), new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									mSelectedAppIndex = -1;
								}
							})
							.setSingleChoiceItems(array, -1, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									mSelectedAppIndex = which;
								}
							});
            	dlgSelectApp = builder.create();
            	dlgSelectApp.show();
            }
        });

        int appIndex = 0;
        Date startDate = new Date();
        while (true) {
            if (mSelectedAppIndex >= 0) {
                appIndex = mSelectedAppIndex;
                mSelectedAppIndex = -1;
                break;
            }
            Date now = new Date();
            if ((now.getTime() - startDate.getTime()) > (30 * 1000)) {
            	mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (dlgSelectApp != null) 
                        	dlgSelectApp.dismiss();
                    }
                });
                break;
            }

        }

        return appIndex;
    }
      
    @Override
    public boolean onConfirmSchedule(final List<Entry<Date, Double>> steps, final double totalAmount) {    	
    	mActivity.runOnUiThread(new Runnable() {			
			@Override
			public void run() {
				dlgSteps = new RegularStepsDialog(mActivity, steps, totalAmount);
				dlgSteps.setConfirmListener(new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface paramDialogInterface, int paramInt) {
						mStepsConfirmed = 1;
					}
				});
				dlgSteps.setOnCancelListener(new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface paramDialogInterface) {
						mStepsConfirmed = 0;
					}
				});
				dlgSteps.show();
			}
		});
    	
    	int stepsConfirmed = -1;
    	Date startDate = new Date();
    	while (true) {
            if (mStepsConfirmed >= 0) {
            	stepsConfirmed = mStepsConfirmed;
                mStepsConfirmed = -1;
                break;
            }
            Date now = new Date();
            if ((now.getTime() - startDate.getTime()) > (30 * 1000)) {
            	mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (dlgSteps != null) 
                        	dlgSteps.dismiss();
                    }
                });
                break;
            }

        }
    	
    	if (stepsConfirmed <= 0)
    		dismiss();
    	
    	return stepsConfirmed > 0 ? true : false;
    }

	@Override
	public boolean onScheduleCreationFailed(final PaymentError error, final String errorMsg) {
		try {
			mActivity.runOnUiThread(new Runnable() {			
				@Override
				public void run() {
					AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
					String message = String.format(getContext().getString(R.string.error_schedule_creation), error == PaymentError.CONNECTION_ERROR ? getContext().getString(R.string.error_no_response) : String.valueOf(errorMsg));
					builder.setMessage(message);
					builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							mStepsRetry = false;
							dialog.dismiss();
							PaymentDialog.this.dismiss();
						}
					});
					builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							mStepsRetry = true;
							dialog.dismiss();
						}
					});
					builder.setCancelable(false);
					builder.show();
				}
			});
			
			while (mStepsRetry == null)
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			
			boolean result = mStepsRetry.booleanValue();
			return result;
		} finally {
			mStepsRetry = null;
		}
	}
    
    
    
}