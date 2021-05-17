package ibox.pro.sdk.external.example.fragments;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.InputFilter;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Hashtable;

import ibox.pro.sdk.external.PaymentController;
import ibox.pro.sdk.external.PaymentController.ReaderType;
import ibox.pro.sdk.external.PaymentControllerException;
import ibox.pro.sdk.external.entities.SettlementResult;
import ibox.pro.sdk.external.RegistrationCallback;
import ibox.pro.sdk.external.example.Consts;
import ibox.pro.sdk.external.example.R;
import ibox.pro.sdk.external.example.UnattendedActivity;
import ibox.pro.sdk.external.example.Utils;
import ibox.pro.sdk.external.example.dialogs.AutoconfigDialog;

public class FragmentSettings extends Fragment {
	private ListView lvReaders;
	private ReadersAdapter mAdapter;
	private Button btnAutoconfig, btnSettlement;
	private Button btnSoftposReg;

	private final String config = null;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_settings, container, false);
		
		lvReaders = (ListView)view.findViewById(R.id.settings_lv_readers);

		mAdapter = new ReadersAdapter(getActivity());
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
										PaymentController.getInstance().setReaderType(getActivity(), reader, address, config);

										dialog.dismiss();
										mAdapter.notifyDataSetChanged();
										Utils.setString(getActivity(), Consts.SavedParams.READER_TYPE_KEY, reader.name());
										Utils.setString(getActivity(), Consts.SavedParams.READER_ADDRESS_KEY, address);
									} catch (PaymentControllerException e) {
										Toast.makeText(getContext(), e.getMessage(),Toast.LENGTH_LONG).show();
									}
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
										PaymentController.getInstance().setReaderType(getActivity(), reader, address, config);
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
					final EditText edtAccessCode = new EditText(getContext());
					edtAccessCode.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
					edtAccessCode.setText("");
					edtAccessCode.setInputType(InputType.TYPE_CLASS_NUMBER);
					edtAccessCode.setSelection(edtAccessCode.getText().length());
					new AlertDialog.Builder(getActivity())
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
									try {
										PaymentController.getInstance().setReaderType(getActivity(), reader, null);

										String accessCode = edtAccessCode.getText().toString();
										Utils.setString(getActivity(), Consts.SavedParams.READER_ACCESS_CODE, accessCode);
										Hashtable<String, Object> p = new Hashtable<>();
										p.put("AccessCode", accessCode);
										PaymentController.getInstance().setCustomReaderParams(p);

										mAdapter.notifyDataSetChanged();
										dialogInterface.dismiss();
									} catch (PaymentControllerException e) {
										Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
									}
								}
							})
							.create().show();
				} else {
					try {
						PaymentController.getInstance().setReaderType(getActivity(), reader, null);
						Utils.setString(getActivity(), Consts.SavedParams.READER_TYPE_KEY, reader.name());
						Utils.setString(getActivity(), Consts.SavedParams.READER_ADDRESS_KEY, null);
						mAdapter.notifyDataSetChanged();
					} catch (PaymentControllerException e) {
						Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
					}
				}

				btnSoftposReg.setVisibility(reader == ReaderType.SOFTPOS ? View.VISIBLE : View.GONE);
			}
		});

		if (!Build.MANUFACTURER.equalsIgnoreCase("BBPOS"))
			lvReaders.setAdapter(mAdapter);

		btnAutoconfig = (Button)view.findViewById(R.id.settings_btn_autoconfig);
		btnAutoconfig.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (PaymentController.getInstance().getReaderType() == ReaderType.TTK) {
					try {
						PaymentController.getInstance().auth(getContext());
					} catch (PaymentControllerException e) {
						Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
					}
				} else if (PaymentController.getInstance().getReaderType() != null)
					new AutoconfigDialog(getActivity()).show();
				else
					Toast.makeText(getActivity(), R.string.settings_lbl_title, Toast.LENGTH_LONG).show();
			}
		});

		btnSettlement = (Button)view.findViewById(R.id.settings_btn_settlement);
		btnSettlement.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					SettlementResult result = PaymentController.getInstance().settlement();
					String message = result.isSuccess() ? getString(R.string.success) : (getString(R.string.failed) + " : " + result.getErrorMessage());
					Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
				} catch (PaymentControllerException e) {
					Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
				}
			}
		});

		btnSoftposReg = (Button)view.findViewById(R.id.settings_btn_softpos_reg);
		btnSoftposReg.setVisibility(PaymentController.getInstance().getReaderType() == ReaderType.SOFTPOS ? View.VISIBLE : View.GONE);
		btnSoftposReg.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (PaymentController.getInstance().getReaderType() == ReaderType.SOFTPOS) {
					try {
						PaymentController.getInstance().startSoftposRegistration(new RegistrationCallback() {
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

	private String getReaderName(PaymentController.ReaderType reader) {
		return reader.getName();
	}
	
	private class ReadersAdapter extends ArrayAdapter<PaymentController.ReaderType> {
		public ReadersAdapter(Context c) {
			super(c, 0, PaymentController.ReaderType.values());
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
			
			if (PaymentController.getInstance().getReaderType() != null && PaymentController.getInstance().getReaderType().ordinal() == position) {
				holder.lblName.setTextColor(Color.parseColor("#009900"));
			} else {
				holder.lblName.setTextColor(Color.parseColor("#000000"));
			}
			
			return convertView;
		}
	}
	
}
