package ibox.pro.sdk.external.example.fragments;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
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
import ibox.pro.sdk.external.entities.SettlementResult;
import ibox.pro.sdk.external.example.Consts;
import ibox.pro.sdk.external.example.R;
import ibox.pro.sdk.external.example.Utils;
import ibox.pro.sdk.external.example.dialogs.AutoconfigDialog;

public class FragmentSettings extends Fragment {
	private ListView lvReaders;
	private ReadersAdapter mAdapter;
	private Button btnAutoconfig, btnSettlement;

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
									String address = usbSupported
										? (which > 0 ? bondedDevices.get(which - 1).getAddress() : PaymentController.USB_MODE_KEY)
										: bondedDevices.get(which).getAddress();
									PaymentController.getInstance().setReaderType(getActivity(), reader, address, config);

									dialog.dismiss();
									mAdapter.notifyDataSetChanged();
									Utils.setString(getActivity(), Consts.SavedParams.READER_TYPE_KEY, reader.name());
									Utils.setString(getActivity(), Consts.SavedParams.READER_ADDRESS_KEY, address);
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
									} catch (IllegalArgumentException e) {
										Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
									}
									mAdapter.notifyDataSetChanged();
									dialogInterface.dismiss();
								}
							})
							.create().show();
				} else {
					PaymentController.getInstance().setReaderType(getActivity(), reader, null);
					Utils.setString(getActivity(), Consts.SavedParams.READER_TYPE_KEY, reader.name());
					Utils.setString(getActivity(), Consts.SavedParams.READER_ADDRESS_KEY, null);
					mAdapter.notifyDataSetChanged();
				}
			}
		});

		if (!Build.MANUFACTURER.equalsIgnoreCase("BBPOS"))
			lvReaders.setAdapter(mAdapter);

		btnAutoconfig = (Button)view.findViewById(R.id.settings_btn_autoconfig);
		btnAutoconfig.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (PaymentController.getInstance().getReaderType() != null)
					new AutoconfigDialog(getActivity()).show();
				else
					Toast.makeText(getActivity(), R.string.settings_lbl_title, Toast.LENGTH_LONG).show();
			}
		});

		btnSettlement = (Button)view.findViewById(R.id.settings_btn_settlement);
		btnSettlement.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				SettlementResult result = PaymentController.getInstance().settlement();
				String message = result.isSuccess() ? "Успешно" : "Ошибка: " + result.getErrorMessage();
				Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
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
