package ibox.pro.sdk.external.example.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Layout;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import ibox.pro.sdk.external.PaymentContext;
import ibox.pro.sdk.external.PaymentController;
import ibox.pro.sdk.external.PaymentController.PaymentError;
import ibox.pro.sdk.external.PaymentController.ReaderEvent;
import ibox.pro.sdk.external.PaymentControllerListener;
import ibox.pro.sdk.external.PaymentException;
import ibox.pro.sdk.external.PaymentResultContext;
import ibox.pro.sdk.external.example.MainActivity;
import ibox.pro.sdk.external.example.R;
import ibox.pro.sdk.external.example.Utils;

public class PaymentDialog extends Dialog implements PaymentControllerListener {
    private Activity mActivity;
    
    protected TextView lblState;
    private ImageView imgSpinner;
    private RotateAnimation spinRotation;
    private AlertDialog dlgSelectApp, dlgScheduleFailed, dlgCancellationTimeout;
    private RegularStepsDialog dlgSteps;
    
    private PaymentContext mPaymentContext;
    private int mSelectedAppIndex;
    private Boolean mStepsConfirmed, mStepsRetry, mDoReturn;

    protected PaymentDialog(Activity context) {
		super(context);
		mActivity = context;
		init();
	}

    public PaymentDialog(Activity context, PaymentContext paymentContext) {
        this(context);
		mPaymentContext = paymentContext;
    }

    private void init() {
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		getWindow().setContentView(R.layout.dialog_payment);
        getWindow().getAttributes().gravity = Gravity.CENTER_VERTICAL;
		setCanceledOnTouchOutside(false);

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
    }

    protected boolean usesReader() {
		return mPaymentContext.getMethod() == PaymentController.PaymentMethod.CARD;
	}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		if (usesReader())
			PaymentController.getInstance().enable();
    }

	@Override
	protected void onStart() {
		super.onStart();
		if (!usesReader())
			try {
				action();
			} catch (PaymentException e) {
				e.printStackTrace();
				onError(null, e.getMessage());
			}
	}

	protected void action() throws PaymentException {
		PaymentController.getInstance().startPayment(getContext(), mPaymentContext);
	}

	protected int getReadyStringID() {
		boolean nfcAllowed = true;

		return PaymentController.getInstance().getReaderType().isMultiInputSupported() && nfcAllowed
					? R.string.reader_state_ready_multiinput
					: mPaymentContext.getNFC() ? R.string.reader_state_ready_nfconly : R.string.reader_state_ready;
	}

	protected String getProgressString() {
        return getContext().getString(R.string.reader_state_inprogress);
    }

    protected void startProgress() {
    	imgSpinner.setVisibility(View.VISIBLE);
    	imgSpinner.startAnimation(spinRotation);
    	lblState.setText(getProgressString());
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
                case NFC_NOT_ALLOWED :
                    toastText = mActivity.getString(R.string.NFC_NOT_ALLOWED);
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
				case INVALID_INPUT_TYPE:
					toastText = mActivity.getString(R.string.error_invalid_input_type);
					break;
				case INVALID_AMOUNT:
					toastText = mActivity.getString(R.string.error_invalid_amount);
					break;
				case TTK_FAILED:
					toastText = String.format(mActivity.getString(R.string.error_standalone_format), errorMessage);
					break;
				case EXT_APP_FAILED:
					toastText = String.format(mActivity.getString(R.string.error_ext_app_format), errorMessage);
					break;
				case NFC_LIMIT_EXCEEDED:
					toastText = mActivity.getString(R.string.NFC_LIMIT_EXCEEDED);
					break;
                default :
                    toastText = mActivity.getString(R.string.EMV_ERROR);
                    break;
            }
		dismiss();
		Toast.makeText(mActivity, String.format("%s (%s)", toastText, (error == null ? "null" : error.toString())), Toast.LENGTH_LONG).show();
    }

	@Override
	public void onTransactionStarted(String transactionID) {
        if (mPaymentContext.getMethod() != PaymentController.PaymentMethod.CARD)
            startProgress();
		lblState.setText(String.format(getContext().getString(R.string.payment_dlg_started), transactionID == null ? "" : transactionID));
	}

	@Override
    public void onFinished(final PaymentResultContext paymentResultContext) {
		dismiss();
		Dialog resultDialog = paymentResultContext.getDeferredData() == null
				? new ResultDialog(mActivity, paymentResultContext, false)
				: new DeferredResultDialog(mActivity, paymentResultContext);
		resultDialog.show();
	}

	@Override
	public void onReaderEvent(ReaderEvent event) {
		Log.i("iboxSDK", "onReaderEvent: " + event.toString());
		switch (event) {
			case CONNECTED :
			case START_INIT :
				lblState.setText(R.string.reader_state_init);
				break;
			case INIT_SUCCESSFULLY:
				lblState.setText(R.string.progress);
				if (usesReader())
					try {
						action();
					} catch (PaymentException e) {
						e.printStackTrace();
						onError(null, e.getMessage());
					}
				break;
			case DISCONNECTED :
				stopProgress();
				lblState.setText(R.string.reader_state_disconnected);
				break;
			case SWIPE_CARD :
			case EMV_TRANSACTION_STARTED :
			case NFC_TRANSACTION_STARTED :
				startProgress();
				break;
			case WAITING_FOR_CARD :
				lblState.setText(getReadyStringID());
				break;
			case PAYMENT_CANCELED :
				Toast.makeText(mActivity, R.string.payment_dlg_canceled, Toast.LENGTH_LONG).show();
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
			case CARD_TIMEOUT :
				Toast.makeText(mActivity, R.string.reader_card_timeout, Toast.LENGTH_LONG).show();
				dismiss();
				break;
			case PIN_TIMEOUT:
				Toast.makeText(mActivity, R.string.reader_pin_timeout, Toast.LENGTH_LONG).show();
				dismiss();
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
                        synchronized (lock) {
							mSelectedAppIndex = which;
							lock.notifyAll();
                        }
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

		mActivity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				dlgSelectApp.dismiss();
				if (mSelectedAppIndex < 0)
					PaymentDialog.this.dismiss();
			}
		});

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
		lblState.setText(lblState.getText() + "\n" + getContext().getString(R.string.payment_toast_pin_request));
	}

	@Override
	public void onPinEntered() {
		lblState.setText(lblState.getText() + "\n" + getContext().getString(R.string.payment_toast_pin_entered));
	}

	@Override
	public void onBatteryState(double percent) {
		Toast.makeText(mActivity,
				String.format(getContext().getString(R.string.payment_toast_battery_format), percent),
				Toast.LENGTH_SHORT).show();
	}

	@Override
	public PaymentController.PaymentInputType onSelectInputType(List<PaymentController.PaymentInputType> allowedInputTypes) {
		class ResultWrapper {
			int result = -1;
		}

		final ResultWrapper resultWrapper = new ResultWrapper();
		final Object lock = new Object();
		final String[] inputTypesArray = new String [allowedInputTypes.size()];
		for (int i = 0; i < allowedInputTypes.size(); i++) {
			String inputType = null;
			String [] inputTypesStrings = mActivity.getResources().getStringArray(R.array.input_types);
			switch (allowedInputTypes.get(i)) {
				case CASH:
					inputType = inputTypesStrings[0];
					break;
				case SWIPE:
					inputType = inputTypesStrings[1];
					break;
				case CHIP:
					inputType = inputTypesStrings[2];
					break;
				case NFC:
					inputType = inputTypesStrings[3];
					break;
				case PREPAID:
					inputType = inputTypesStrings[4];
					break;
				case CREDIT:
					inputType = inputTypesStrings[5];
					break;
				default:
					inputType = "";
			}
			inputTypesArray[i] = inputType;
		}
		final AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
		builder.setCancelable(false)
				.setTitle("Select input type")
				.setNegativeButton(mActivity.getString(R.string.cancel), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						resultWrapper.result = -1;
					}
				})
				.setSingleChoiceItems(inputTypesArray, -1, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						synchronized (lock) {
							resultWrapper.result = which;
							lock.notifyAll();
						}
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

		mActivity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				dlgSelectApp.dismiss();
				if (resultWrapper.result < 0)
					PaymentDialog.this.dismiss();
			}
		});

		return resultWrapper.result >= 0 ? allowedInputTypes.get(resultWrapper.result) : null;
	}

	@Override
	public void onAutoConfigUpdate(double perecent) {

	}

	@Override
	public void onAutoConfigFinished(boolean success, String config, boolean isDefault) {

	}

	@Override
	public void onSwitchedToCNP() {

	}

	@Override
	public void onSearchMifareCard(Hashtable<String, String> cardData) {

	}

	@Override
	public void onVerifyMifareCard(boolean flag) {

	}

	@Override
	public void onWriteMifareCard(boolean flag) {

	}

	@Override
	public void onReadMifareCard(Hashtable<String, String> data) {

	}

	@Override
	public void onOperateMifareCard(Hashtable<String, String> data) {

	}

	@Override
	public void onTransferMifareData(String data) {

	}

	@Override
	public void onFinishMifareCard(boolean flag) {

	}

	@Override
	public void onReturnPowerOnNFCResult(boolean result) {

	}

	@Override
	public void onReturnNFCApduResult(boolean result, String apdu, int apduLen) {

	}

	@Override
	public void onReturnPowerOffNFCResult(boolean result) {

	}
}