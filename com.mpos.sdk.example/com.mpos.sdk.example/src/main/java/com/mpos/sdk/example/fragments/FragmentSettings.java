package com.mpos.sdk.example.fragments;

import android.app.AlertDialog;

import android.app.Dialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import com.mpos.sdk.PaymentController;
import com.mpos.sdk.PaymentController.ReaderType;
import com.mpos.sdk.PaymentControllerException;
import com.mpos.sdk.PaymentControllerListener;
import com.mpos.sdk.PaymentResultContext;
import com.mpos.sdk.SoftposActionCallback;
import com.mpos.sdk.RegistrationCallback;
import com.mpos.sdk.entities.SettlementResult;
import com.mpos.sdk.example.CommonAsyncTask;
import com.mpos.sdk.example.dialogs.ConfigDialog;
import com.mpos.sdk.example.Consts;
import com.mpos.sdk.example.R;
import com.mpos.sdk.example.UnattendedActivity;
import com.mpos.sdk.example.Utils;
import com.mpos.sdk.example.dialogs.AutoconfigDialog;
import com.mpos.sdk.example.dialogs.GetKeysHostsDialog;

public class FragmentSettings extends Fragment {

	private ListView lvReaders;
	private ReadersAdapter mAdapter;
	private Button btnConfig, btnKeys, btnSettlement;
	private Button btnSoftposReg, btnRemoveSoftposAccount;
	private SettlementTask settlementTask;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_settings, container, false);
		
		lvReaders = (ListView)view.findViewById(R.id.settings_lv_readers);

		ArrayList<ReaderType> allowedReaders = new ArrayList<>(Arrays.asList(PaymentController.ReaderType.values()));

		mAdapter = new ReadersAdapter(getActivity(), allowedReaders);
		lvReaders.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
				final ReaderType reader = PaymentController.ReaderType.values()[pos];
				if (reader.isWireless()) {
					final ArrayList<BluetoothDevice> bondedDevices = PaymentController.getInstance().getBluetoothDevices(getActivity());
					final boolean usbSupported = reader.isUsbSupported();
					String [] devices = new String [bondedDevices.size() + (usbSupported ? 1 : 0)];
					if (usbSupported)
						devices[0] = "USB";
					int i = usbSupported ? 1 : 0;
					for (BluetoothDevice device : bondedDevices)
						devices[i++] = (device.getName() != null && device.getName().length() > 0) ? device.getName() : device.getAddress();										
					
					new AlertDialog.Builder(getActivity())
            				.setCancelable(false)
            				.setTitle("Select device")
            				.setNegativeButton(getActivity().getString(R.string.cancel), new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									dialog.dismiss();
								}
							})
							.setSingleChoiceItems(devices, -1, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									try {
										String address = usbSupported
												? (which > 0 ? bondedDevices.get(which - 1).getAddress() : PaymentController.USB_MODE_KEY)
												: bondedDevices.get(which).getAddress();
										PaymentController.getInstance().setReaderType(getActivity(), reader, address);

										if (reader.equals(ReaderType.P17)) {
											Hashtable<String, Object> p = new Hashtable<>();
											p.put("NOTUP", true);
											PaymentController.getInstance().setCustomReaderParams(p);
										}
										Utils.setString(getActivity(), Consts.SavedParams.READER_TYPE_KEY, reader.name());
										Utils.setString(getActivity(), Consts.SavedParams.READER_ADDRESS_KEY, address);
									} catch (PaymentControllerException e) {
										Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
									}
									dialog.dismiss();
									mAdapter.notifyDataSetChanged();
								}
							})
							.create().show();
				} else if (reader.isTTK()) {
					final EditText edtAddress = new EditText(getContext());
					edtAddress.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
					edtAddress.setText("127.0.0.1:43888");
					edtAddress.setSelection(edtAddress.getText().length());
					new AlertDialog.Builder(getActivity())
							.setCancelable(false)
							.setTitle("Set address")
							.setNegativeButton(getActivity().getString(R.string.cancel), new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									dialog.dismiss();
								}
							})
							.setView(edtAddress)
							.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialogInterface, int i) {
									try {
										String address = edtAddress.getText().toString();
										PaymentController.getInstance().setReaderType(getActivity(), reader, address);
										Utils.setString(getActivity(), Consts.SavedParams.READER_TYPE_KEY, reader.name());
										Utils.setString(getActivity(), Consts.SavedParams.READER_ADDRESS_KEY, address);
									} catch (PaymentControllerException e) {
										Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
									}
									mAdapter.notifyDataSetChanged();
									dialogInterface.dismiss();
								}
							})
							.create().show();
				} else if (reader == ReaderType.SOFTPOS) {
					configureSoftpos();
				} else {
					try {
						PaymentController.getInstance().setReaderType(getActivity(), reader, null);
						Utils.setString(getActivity(), Consts.SavedParams.READER_TYPE_KEY, reader.name());
						Utils.setString(getActivity(), Consts.SavedParams.READER_ADDRESS_KEY, null);
					} catch (PaymentControllerException e) {
						Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
					}
					mAdapter.notifyDataSetChanged();
				}

				btnSoftposReg.setVisibility(reader == ReaderType.SOFTPOS ? View.VISIBLE : View.GONE);
				btnRemoveSoftposAccount.setVisibility(reader == ReaderType.SOFTPOS ? View.VISIBLE : View.GONE);
			}
		});

		if (!Build.MANUFACTURER.equalsIgnoreCase("BBPOS"))
			lvReaders.setAdapter(mAdapter);

		btnConfig = (Button)view.findViewById(R.id.settings_btn_config);
		btnConfig.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				PaymentController.ReaderType readerType = PaymentController.getInstance().getReaderType();
				if (readerType != null) {
					if (PaymentController.getInstance().getReaderType() == ReaderType.TTK) {
						try {
							PaymentController.getInstance().auth(getContext());
						} catch (PaymentControllerException e) {
							Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
						}
					}
					else if (readerType.isConfigSupported()) {
						new ConfigDialog(getActivity()).show();
					} else
						new AutoconfigDialog(getActivity()).show();
				} else
					Toast.makeText(getActivity(), R.string.settings_lbl_title, Toast.LENGTH_LONG).show();
			}
		});
		btnKeys = (Button)view.findViewById(R.id.settings_btn_keys);
		btnKeys.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (PaymentController.getInstance().getReaderType() != null)
					new GetKeysHostsDialog(getActivity()).show();
				else
					Toast.makeText(getActivity(), R.string.settings_lbl_title, Toast.LENGTH_LONG).show();
			}
		});

		btnSettlement = (Button)view.findViewById(R.id.settings_btn_settlement);
		btnSettlement.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				settlementTask = new SettlementTask(getContext());
				settlementTask.execute();
			}
		});


		btnSoftposReg = (Button)view.findViewById(R.id.settings_btn_softpos_reg);
		btnSoftposReg.setVisibility(PaymentController.getInstance().getReaderType() == ReaderType.SOFTPOS ? View.VISIBLE : View.GONE);
		btnSoftposReg.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (PaymentController.getInstance().getReaderType() == ReaderType.SOFTPOS) {
					final EditText edtOtp = new EditText(getContext());
					edtOtp.setHint("OTP");;
					edtOtp.setInputType(InputType.TYPE_CLASS_NUMBER);
					final CheckBox cbForce = new CheckBox(getContext());
					cbForce.setText(R.string.settings_cb_softpos_reg_force);
					cbForce.setTextColor(Color.WHITE);
					LinearLayout llRoot = new LinearLayout(getContext());
					llRoot.setOrientation(LinearLayout.VERTICAL);
					LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
					llRoot.addView(edtOtp, params);
					llRoot.addView(cbForce, params);
					new AlertDialog.Builder(getContext())
						.setTitle(R.string.settings_btn_softpos_reg)
						.setView(llRoot)
						.setPositiveButton(R.string.common_continue, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								try {
									PaymentController.getInstance().startSoftposRegistration(edtOtp.getText().toString(), cbForce.isChecked(), new RegistrationCallback() {
										@Override
										public void onFinished(String accessCode) {
											Toast.makeText(getActivity(), "SUCCESS " + accessCode, Toast.LENGTH_LONG).show();
										}

										@Override
										public void onFailed(String error) {
											Toast.makeText(getActivity(), "FAILED: " + error, Toast.LENGTH_LONG).show();
										}
									});
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						})
						.setNegativeButton(R.string.cancel, null)
						.create()
						.show();
				}
			}
		});

		btnRemoveSoftposAccount = (Button)view.findViewById(R.id.settings_btn_softpos_remove_acc);
		btnRemoveSoftposAccount.setVisibility(PaymentController.getInstance().getReaderType() == ReaderType.SOFTPOS ? View.VISIBLE : View.GONE);
		btnRemoveSoftposAccount.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (PaymentController.getInstance().getReaderType() == ReaderType.SOFTPOS) {
					try {
						PaymentController.getInstance().removeSoftposAccount(new SoftposActionCallback() {
							@Override
							public void onFinished(String data) {
								Toast.makeText(getActivity(), "SUCCESS " + data, Toast.LENGTH_LONG).show();
							}

							@Override
							public void onFailed(String error) {
								Toast.makeText(getActivity(), "FAILED: " + error, Toast.LENGTH_LONG).show();
							}
						});
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		});

		view.findViewById(R.id.settings_btn_unattended).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(getContext(), UnattendedActivity.class));
			}
		});
		return view;
	}

	@Override
	public void onStop() {
		super.onStop();
		if (settlementTask != null)
			settlementTask.cancel(true);
	}


	private String getReaderName(PaymentController.ReaderType reader) {
		return reader.getName();
	}

	private String getSoftposPackage(ResolveInfo resolveInfo) {
		return resolveInfo.activityInfo.applicationInfo.packageName;
	}

	private void configureSoftpos() {
		List<ResolveInfo> softposApps = getContext().getPackageManager()
			.queryIntentActivities(
				new Intent("softpos.ready"),
				PackageManager.MATCH_DEFAULT_ONLY
			);
		if (softposApps != null && softposApps.size() > 0) {
			final EditText edtAccessCode = new EditText(getContext());
			edtAccessCode.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
			edtAccessCode.setText("");
			edtAccessCode.setInputType(InputType.TYPE_CLASS_NUMBER);
			edtAccessCode.setSelection(edtAccessCode.getText().length());
			final Dialog dlgAccessCode = new AlertDialog.Builder(getActivity())
					.setCancelable(false)
					.setTitle("Set access code")
					.setNegativeButton(getActivity().getString(R.string.cancel), new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					})
					.setView(edtAccessCode)
					.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialogInterface, int i) {
							Utils.setString(getActivity(), Consts.SavedParams.READER_TYPE_KEY, ReaderType.SOFTPOS.name());
							try {
								PaymentController.getInstance().setReaderType(getActivity(), ReaderType.SOFTPOS, null);

								String accessCode = edtAccessCode.getText().toString();
								Utils.setString(getActivity(), Consts.SavedParams.READER_ACCESS_CODE, accessCode);
								Hashtable<String, Object> p = new Hashtable<>();
								p.put("AccessCode", accessCode);
								p.put("PackageName", Utils.getString(getActivity(), Consts.SavedParams.READER_SOFTPOS_PACKAGE));
								PaymentController.getInstance().setCustomReaderParams(p);

								mAdapter.notifyDataSetChanged();
								dialogInterface.dismiss();
							} catch (PaymentControllerException e) {
								Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
							}
						}
					})
					.create();

			if (softposApps.size() == 1) {
				Utils.setString(getActivity(),
					Consts.SavedParams.READER_SOFTPOS_PACKAGE,
					getSoftposPackage(softposApps.get(0))
				);
				dlgAccessCode.show();
			} else {
				AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
				alertDialog.setTitle("Select softpos app");

				final String[] appsTitles = new String[softposApps.size()];
				for (int i = 0; i < softposApps.size(); i++)
					appsTitles[i] = softposApps.get(i).activityInfo.applicationInfo.packageName;

				alertDialog
					.setSingleChoiceItems(appsTitles, -1, (dialog, which) -> {
						Utils.setString(getActivity(),
							Consts.SavedParams.READER_SOFTPOS_PACKAGE,
							getSoftposPackage(softposApps.get(which))
						);
						dialog.dismiss();
						dlgAccessCode.show();
					}).setNegativeButton("Cancel", (dialog, which) -> {
						dialog.dismiss();
					}).setCancelable(false)
					.show();
			}
		} else
			Toast.makeText(getActivity(), "Check Softpos is installed", Toast.LENGTH_LONG).show();
	}

	private class ReadersAdapter extends ArrayAdapter<PaymentController.ReaderType> {
		public ReadersAdapter(Context c, List<ReaderType> readers) {
			super(c, 0, readers);
		}
		
		private class ItemHolder {
			TextView lblName;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ItemHolder holder = null;
			
			if (convertView == null) {
				convertView = LayoutInflater.from(getContext()).inflate(R.layout.reader_item, parent, false);
				
				holder = new ItemHolder();
				holder.lblName = (TextView)convertView.findViewById(R.id.reader_item_lbl_name);
				
				convertView.setTag(holder);
			} else {
				holder = (ItemHolder)convertView.getTag();
			}
			
			holder.lblName.setText(getReaderName(getItem(position)));
			
			if (PaymentController.getInstance().getReaderType() != null && PaymentController.getInstance().getReaderType() == getItem(position)) {
				holder.lblName.setTextColor(Color.parseColor("#009900"));
			} else {
				holder.lblName.setTextColor(Color.parseColor("#000000"));
			}
			
			return convertView;
		}
	}


	private class SettlementTask extends CommonAsyncTask<Void, Void, SettlementResult> implements PaymentControllerListener {
		private final CountDownLatch readerReadyLatch = new CountDownLatch(1);
		private boolean readerReady;

		public SettlementTask(Context context) {
			super(context);
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			PaymentController.getInstance().setPaymentControllerListener(this);
			PaymentController.getInstance().enable();
		}

		@Override
		protected void onCancelled() {
			PaymentController.getInstance().setPaymentControllerListener(null);
			PaymentController.getInstance().disable();

			super.onCancelled();
		}

		@Override
		protected SettlementResult doInBackground(Void... voids) {
			try {
				readerReadyLatch.await();
				return readerReady ? PaymentController.getInstance().settlement() : null;
			} catch (PaymentControllerException e) {
				return new SettlementResult().setSuccess(false).setErrorMessage(e.getMessage());
			} catch (InterruptedException e) {
				return null;
			}
		}

		@Override
		protected void onPostExecute(SettlementResult result) {
			super.onPostExecute(result);
			PaymentController.getInstance().setPaymentControllerListener(null);
			PaymentController.getInstance().disable();

			if (result != null) {
				String message = result.isSuccess() ? getString(R.string.success) : (getString(R.string.failed) + " : " + result.getErrorMessage());
				Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
			} else
				Toast.makeText(getActivity(), R.string.failed, Toast.LENGTH_LONG).show();
		}

		@Override
		public void onReaderEvent(PaymentController.ReaderEvent event, Map<String, String> params) {
			if (event == PaymentController.ReaderEvent.CONNECTED || event == PaymentController.ReaderEvent.START_INIT) {
				//ignore
			} else if (event == PaymentController.ReaderEvent.INIT_SUCCESSFULLY) {
				readerReady = true;
				readerReadyLatch.countDown();
			} else
				readerReadyLatch.countDown();
		}

		//region ignored
		@Override
		public void onTransactionStarted(String transactionID) {

		}

		@Override
		public void onFinished(PaymentResultContext result) {

		}

		@Override
		public void onError(PaymentController.PaymentError error, String errorMessage, int extErrorCode) {

		}



		@Override
		public int onSelectApplication(List<String> apps) {
			return 0;
		}

		@Override
		public boolean onConfirmSchedule(List<Map.Entry<Date, Double>> steps, double totalAmount) {
			return false;
		}

		@Override
		public boolean onScheduleCreationFailed(PaymentController.PaymentError error, String errorMessage, int extErrorCode) {
			return false;
		}

		@Override
		public boolean onCancellationTimeout() {
			return false;
		}

		@Override
		public void onPinRequest() {

		}

		@Override
		public void onPinEntered() {

		}


		@Override
		public void onAutoConfigFinished(boolean success, String config, boolean isDefault) {

		}

		@Override
		public void onReaderConfigFinished(boolean success) {

		}

		@Override
		public void onBarcodeScanned(String barcode) {

		}

		@Override
		public boolean onBLCheck(String hashPan, String last4digits) {
			return false;
		}

		@Override
		public void onBatteryState(double percent) {

		}

		@Override
		public PaymentController.PaymentInputType onSelectInputType(List<PaymentController.PaymentInputType> allowedInputTypes) {
			return null;
		}

		@Override
		public void onSwitchedToCNP() {

		}

		@Override
		public void onInjectFinished(boolean success) {

		}

		@Override
		public void onReaderConfigUpdate(String s, Hashtable<String, Object> hashtable) {

		}


		//endregion
	}

}
