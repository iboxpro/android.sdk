package com.mpos.sdk.example.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import androidx.annotation.NonNull;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.mpos.sdk.entities.LinkedCard;
import com.mpos.sdk.example.MainActivity;
import com.mpos.sdk.example.R;

public class LinkedCardsDialog extends Dialog {
    private ListView lvCards;
    private CardsAdapter cardsAdapter;
    private Listener listener;

    public interface Listener {
        void onCardSelected(LinkedCard card);
    }

    public LinkedCardsDialog(Context context, final Listener listener) {
        super(context);
        this.listener = listener;

        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getWindow().getAttributes().gravity = Gravity.CENTER_VERTICAL;
        setContentView(R.layout.dialog_linked_cards);

        lvCards = (ListView) findViewById(R.id.linked_dlg_lv_list);
        lvCards.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (listener != null)
                    listener.onCardSelected(((CardsAdapter)parent.getAdapter()).getItem(position));
                dismiss();
            }
        });
        cardsAdapter = new CardsAdapter();
        lvCards.setAdapter(cardsAdapter);
    }

    private class CardsAdapter extends ArrayAdapter<LinkedCard> {
        public CardsAdapter() {
            super(LinkedCardsDialog.this.getContext(), 0, ((MainActivity)((ContextWrapper)LinkedCardsDialog.this.getContext()).getBaseContext()).LinkedCards);
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

            holder.lblName.setText(getItem(position).getAlias());
            return convertView;
        }
    }
}
