package ibox.pro.sdk.external.example.dialogs;

import ibox.pro.sdk.external.example.R;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class RegularStepsDialog extends Dialog {

	private List<Map.Entry<Date, Double>> mSteps;
	private double mTotal;
	
	private ListView lvSteps;
	private TextView lblTotal;
	private Button btnConfirm;
	
	private DialogInterface.OnClickListener mConfirmListener;
	
	public RegularStepsDialog(Context context, List<Map.Entry<Date, Double>> steps, double total) {
		super(context);
		mSteps = steps;
		mTotal = total;
		init();	
	}
	
	private void init() {
		setTitle(R.string.steps_dlg_lbl_title);
        setContentView(R.layout.dialog_regular_steps);
        getWindow().getAttributes().gravity = Gravity.CENTER_VERTICAL;
        
        lvSteps = (ListView)findViewById(R.id.steps_dlg_lv_steps);
        lblTotal = (TextView)findViewById(R.id.steps_dlg_lbl_total);
        btnConfirm = (Button)findViewById(R.id.steps_dlg_btn_confirm);
        
        lvSteps.setAdapter(new StepsAdapter(mSteps));
        lblTotal.setText(String.format("%.2f", mTotal));
        btnConfirm.setOnClickListener(new View.OnClickListener() {			
			@Override
			public void onClick(View paramView) {
				if (mConfirmListener != null)
					mConfirmListener.onClick(RegularStepsDialog.this, 0);
				dismiss();
			}
		});
	}
	
	public void setConfirmListener(DialogInterface.OnClickListener confirmListener) {
		mConfirmListener = confirmListener;
	}
	
	private class StepsAdapter extends ArrayAdapter<Map.Entry<Date, Double>> {

		public StepsAdapter(List<Entry<Date, Double>> objects) {
			super(RegularStepsDialog.this.getContext(), 0, objects);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_step, parent, false);
			
			TextView lblDate = (TextView)convertView.findViewById(R.id.item_step_lbl_date);
			TextView lblAmount = (TextView)convertView.findViewById(R.id.item_step_lbl_amount);
			
			lblDate.setText(DateFormat.format("dd.MM.yyyy", getItem(position).getKey()));
			lblAmount.setText(String.format("%.2f", getItem(position).getValue()));
			
			return convertView;
		}
		
		
	}
	
}
