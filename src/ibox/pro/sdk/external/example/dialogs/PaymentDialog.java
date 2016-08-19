package ibox.pro.sdk.external.example.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

import ibox.pro.sdk.external.PaymentContext;
import ibox.pro.sdk.external.PaymentController;
import ibox.pro.sdk.external.PaymentController.PaymentError;
import ibox.pro.sdk.external.PaymentController.ReaderEvent;
import ibox.pro.sdk.external.PaymentControllerListener;
import ibox.pro.sdk.external.PaymentException;
import ibox.pro.sdk.external.PaymentResultContext;
import ibox.pro.sdk.external.RegularPaymentContext;
import ibox.pro.sdk.external.example.R;

public class PaymentDialog extends Dialog implements PaymentControllerListener {
	
    private Activity mActivity;
    
    private TextView lblState;
    private ImageView imgSpinner;
    private RotateAnimation spinRotation;
    private AlertDialog dlgSelectApp, dlgScheduleFailed, dlgCancellationTimeout;
    private RegularStepsDialog dlgSteps;
    
    private PaymentContext mPaymentContext;
    private int mSelectedAppIndex;
    private Boolean mStepsConfirmed, mStepsRetry, mDoReturn;

    public PaymentDialog(Activity context, PaymentContext paymentContext) {
        super(context);
        mPaymentContext = paymentContext;
        mActivity = context;
        init();
    }

    private void init() {
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
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        action();
    }

    protected void action() {
		try {
			PaymentController.getInstance().startPayment(getContext(), mPaymentContext);
		} catch (PaymentException e) {
			onError(null, e.getMessage());
		}
	}

	protected int getReadyStringID() {
		return mPaymentContext instanceof RegularPaymentContext ? R.string.reader_state_ready_swipeonly : R.string.reader_state_ready;
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
        if (error == null)
            toastText = String.valueOf(errorMessage);
        else
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
                case NO_SUCH_TRANSACTION :
                    toastText = mActivity.getString(R.string.error_no_such_transaction);
                    break;
				case TRANSACTION_NULL_OR_EMPTY :
					toastText = mActivity.getString(R.string.error_tr_null_or_empty);
					break;
                default :
                    toastText = mActivity.getString(R.string.EMV_ERROR);
                    break;
            }
		Toast.makeText(getContext(), String.format("%s (%s)", toastText, (error == null ? "null" : error.toString())), Toast.LENGTH_LONG).show();
		if (error == null || error == PaymentController.PaymentError.EMV_TERMINATED || error == PaymentError.NO_SUCH_TRANSACTION)
			dismiss();
    }

	@Override
	public void onTransactionStarted(String transactionID) {
		lblState.setText(String.format(getContext().getString(R.string.payment_dlg_started), transactionID));
	}

	@Override
    public void onFinished(PaymentResultContext paymentResultContext) {
    	dismiss();
    	new ResultDialog(mActivity, paymentResultContext, false).show();
    }
    
    @Override
    public void onReaderEvent(ReaderEvent event) {
		Log.i("iboxSDK", "onReaderEvent: " + event.toString());
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
	    		lblState.setText(getReadyStringID());
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
			case BAD_SWIPE :
				Toast.makeText(mActivity, R.string.reader_bad_swipe, Toast.LENGTH_LONG).show();
				break;
			case LOW_BATTERY :
				Toast.makeText(mActivity, R.string.reader_low_battery, Toast.LENGTH_LONG).show();
				break;
	    	default :
	    		break;
    	}
    }
    
    @Override
    public int onSelectApplication(List<String> apps) {
        mSelectedAppIndex = -1;

        final Object lock = new Object();
        final String[] array = apps.toArray(new String[apps.size()]);
        final AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
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

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
            	dlgSelectApp = builder.create();
                dlgSelectApp.setOnDismissListener(new OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        synchronized (lock) {
                            lock.notifyAll();
                        }
                    }
                });
                dlgSelectApp.show();
            }
        });

        synchronized (lock) {
            try {
                lock.wait(30000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (mSelectedAppIndex < 0) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    dlgSelectApp.dismiss();
                    PaymentDialog.this.dismiss();
                }
            });
        }

        return Math.max(mSelectedAppIndex, 0);
    }
      
    @Override
    public boolean onConfirmSchedule(final List<Entry<Date, Double>> steps, final double totalAmount) {
        mStepsConfirmed = null;

		final Object lock = new Object();
    	mActivity.runOnUiThread(new Runnable() {			
			@Override
			public void run() {
				dlgSteps = new RegularStepsDialog(mActivity, steps, totalAmount);
				dlgSteps.setConfirmListener(new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        mStepsConfirmed = true;
					}
				});
				dlgSteps.setOnCancelListener(new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface paramDialogInterface) {
                        mStepsConfirmed = false;
					}
				});
				dlgSteps.setOnDismissListener(new OnDismissListener() {
					@Override
					public void onDismiss(DialogInterface dialogInterface) {
						synchronized (lock) {
							lock.notifyAll();
						}
					}
				});
				dlgSteps.show();
			}
		});
		synchronized (lock) {
			try {
				lock.wait(30000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		if (mStepsConfirmed == null || mStepsConfirmed == false) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    dlgSteps.dismiss();
                    PaymentDialog.this.dismiss();
                }
            });
			return false;
		}
    	
    	return mStepsConfirmed.booleanValue();
    }

	@Override
	public boolean onScheduleCreationFailed(final PaymentError error, final String errorMsg) {
		mStepsRetry = null;

		final Object lock = new Object();
		final AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
		String message = String.format(getContext().getString(R.string.error_schedule_creation), error == PaymentError.CONNECTION_ERROR ? getContext().getString(R.string.error_no_response) : String.valueOf(errorMsg));
		builder.setMessage(message);
		builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				mStepsRetry = false;
				dialog.dismiss();
				synchronized (lock) {
					lock.notifyAll();
				}
			}
		});
		builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				mStepsRetry = true;
				dialog.dismiss();
				synchronized (lock) {
					lock.notifyAll();
				}
			}
		});
		builder.setCancelable(false);

		mActivity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				dlgScheduleFailed = builder.create();
				dlgScheduleFailed.show();
			}
		});
		synchronized (lock) {
			try {
				lock.wait(30000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		if (mStepsRetry == null) {
			mActivity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					dlgScheduleFailed.dismiss();
					dismiss();
				}
			});
			return false;
		}

		return mStepsRetry.booleanValue();
	}

	@Override
	public boolean onCancellationTimeout() {
		mDoReturn = null;

		final Object lock = new Object();
		final AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
		builder.setMessage(getContext().getString(R.string.error_cancellation_timeout));
		builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				mDoReturn = false;
				dialog.dismiss();
				synchronized (lock) {
					lock.notifyAll();
				}
			}
		});
		builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				mDoReturn = true;
				dialog.dismiss();
				synchronized (lock) {
					lock.notifyAll();
				}
			}
		});
		builder.setCancelable(false);

		mActivity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				dlgCancellationTimeout = builder.create();
				dlgCancellationTimeout.show();
			}
		});
		synchronized (lock) {
			try {
				lock.wait(30000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		if (mDoReturn == null || !mDoReturn) {
			mActivity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					dlgCancellationTimeout.dismiss();
                    PaymentDialog.this.dismiss();
				}
			});

			return false;
		}

		return mDoReturn.booleanValue();
	}

	@Override
	public void onPinRequest() {
		Toast.makeText(mActivity, R.string.payment_toast_pin_request, Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onPinEntered() {
		Toast.makeText(mActivity, R.string.payment_toast_pin_entered, Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onAutoConfigUpdate(double perecent) {

	}

	@Override
	public void onAutoConfigFinished(boolean success, String config, boolean isDefault) {

	}

	@Override
	public void onBatteryState(double percent) {
		Toast.makeText(mActivity,
				String.format(getContext().getString(R.string.payment_toast_battery_format), percent),
				Toast.LENGTH_SHORT).show();
	}

}