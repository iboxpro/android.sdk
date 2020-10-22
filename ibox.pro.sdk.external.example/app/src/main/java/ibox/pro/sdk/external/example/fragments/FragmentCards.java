package ibox.pro.sdk.external.example.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import ibox.pro.sdk.external.AttachCardContext;
import ibox.pro.sdk.external.PaymentController;
import ibox.pro.sdk.external.PaymentException;
import ibox.pro.sdk.external.PaymentResultContext;
import ibox.pro.sdk.external.entities.APIReadLinkedCardsResult;
import ibox.pro.sdk.external.entities.APIResult;
import ibox.pro.sdk.external.entities.LinkedCard;
import ibox.pro.sdk.external.example.CommonAsyncTask;
import ibox.pro.sdk.external.example.MainActivity;
import ibox.pro.sdk.external.example.R;
import ibox.pro.sdk.external.example.dialogs.DeferredResultDialog;
import ibox.pro.sdk.external.example.dialogs.PaymentDialog;

public class FragmentCards extends Fragment {
    private ListView lvCards;
    private Button btnAdd, btnAddDeferred, btnRefresh, btnBalance;
    private CardsAdapter cardsAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cards, container, false);

        initControls(view);
        if (cardsAdapter == null)
            cardsAdapter = new CardsAdapter();
        lvCards.setAdapter(cardsAdapter);
        lvCards.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final LinkedCard selectedCard = cardsAdapter.getItem(position);
                new AlertDialog.Builder(getActivity())
                        .setTitle(String.format(getString(R.string.cards_dlg_remove_format), selectedCard.getAlias()))
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                remove(selectedCard.getID());
                            }
                        })
                        .setNegativeButton(R.string.cancel, null)
                    .show();
                return true;
            }
        });
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                add();
            }
        });
        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refresh();
            }
        });
        btnBalance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkBalance();
            }
        });
        btnAddDeferred.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addDeferred();
            }
        });


        refresh();
        return view;
    }

    private void initControls(View view) {
        lvCards = (ListView) view.findViewById(R.id.cards_lv_cards);
        btnAdd = (Button) view.findViewById(R.id.cards_btn_add);
        btnAddDeferred = (Button) view.findViewById(R.id.cards_btn_add_deferred);
        btnRefresh = (Button) view.findViewById(R.id.cards_btn_refresh);
        btnBalance = (Button) view.findViewById(R.id.cards_btn_balance);
    }

    private void add() {
       new AttachCardDialog().show();
    }

    private void remove(int cardID) {
        new RemoveTask().execute(cardID);
    }

    private void addDeferred() {
        new AttachCardDialog().setDeferred(true).show();
    }

    private void refresh() {
        new RefreshTask().execute();
    }

    private void checkBalance() {
        new CheckBalanceDialog().show();
    }

    private class CardsAdapter extends ArrayAdapter<LinkedCard> {
        public CardsAdapter() {
            super(getActivity(), 0, ((MainActivity) getActivity()).LinkedCards);
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

    private class RefreshTask extends CommonAsyncTask<Void, Void, APIReadLinkedCardsResult> {

        public RefreshTask() {
            super(getActivity());
        }


        @Override
        protected APIReadLinkedCardsResult doInBackground(Void ... params) {
            return PaymentController.getInstance().getLinkedCards(getActivity());
        }

        @Override
        protected void onPostExecute(APIReadLinkedCardsResult result) {
            super.onPostExecute(result);

            if (result != null) {
                if (result.isValid()) {
                    MainActivity activity = (MainActivity) getActivity();
                    activity.LinkedCards.clear();
                    if (result.getLinkedCards() != null)
                        activity.LinkedCards.addAll(result.getLinkedCards());
                    cardsAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(getActivity(), result.getErrorMessage(), Toast.LENGTH_LONG).show();
                }
            } else
                Toast.makeText(getActivity(), R.string.error_no_response, Toast.LENGTH_LONG).show();

        }
    }

    private class RemoveTask extends CommonAsyncTask<Integer, Void, APIResult> {

        public RemoveTask() {
            super(getActivity());
        }

        @Override
        protected APIResult doInBackground(Integer ... params) {
            return PaymentController.getInstance().removeLinkedCard(getContext(), params[0]);
        }

        @Override
        protected void onPostExecute(APIResult result) {
            super.onPostExecute(result);

            if (result != null) {
                if (result.isValid()) {
                    refresh();
                } else {
                    Toast.makeText(getActivity(), result.getErrorMessage(), Toast.LENGTH_LONG).show();
                }
            } else
                Toast.makeText(getActivity(), R.string.error_no_response, Toast.LENGTH_LONG).show();
        }
    }

    public class AttachCardDialog extends PaymentDialog {
        private boolean deferred;

        public AttachCardDialog() {
            super(getActivity(), null);
        }

        public AttachCardDialog setDeferred(boolean deferred) {
            this.deferred = deferred;
            return this;
        }

        @Override
        protected boolean usesReader() {
            return true;
        }

        @Override
        protected void action() throws PaymentException {
            AttachCardContext attachCardContext = new AttachCardContext()
                    .setDeferred(deferred)
                    .setCurrency(MainActivity.CURRENCY);
            PaymentController.getInstance().addLinkedCard(getContext(), attachCardContext);
        }

        @Override
        public void onTransactionStarted(String transactionID) {
            startProgress();
        }

        @Override
        protected String getProgressString() {
            return getString(R.string.progress);
        }

        @Override
        protected int getReadyStringID() {
            return R.string.reader_state_ready_multiinput;
        }

        @Override
        public void onFinished(PaymentResultContext paymentResultContext) {
            if (deferred)
                new DeferredResultDialog(getContext(), paymentResultContext).show();
            else
                refresh();
            dismiss();
        }
    }

    public class CheckBalanceDialog extends PaymentDialog {
        public CheckBalanceDialog() {
            super(getActivity(), null);
        }

        @Override
        protected boolean usesReader() {
            return true;
        }

        @Override
        protected void action() throws PaymentException {
            PaymentController.getInstance().balanceInquiry(getContext(), MainActivity.CURRENCY);
        }

        @Override
        public void onTransactionStarted(String transactionID) {
            startProgress();
        }

        @Override
        protected String getProgressString() {
            return getString(R.string.progress);
        }

        @Override
        protected int getReadyStringID() {
            return R.string.reader_state_ready_swipeonly;
        }

        @Override
        public void onFinished(PaymentResultContext paymentResultContext) {
            Toast.makeText(getContext(), String.format(
                    getString(R.string.cards_dlg_balance_result_format),
                    paymentResultContext.getTransactionItem().getBalance()
            ), Toast.LENGTH_LONG).show();
            dismiss();
        }
    }
}
