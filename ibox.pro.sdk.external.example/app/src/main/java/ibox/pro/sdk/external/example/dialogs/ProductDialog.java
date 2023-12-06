package ibox.pro.sdk.external.example.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import androidx.annotation.NonNull;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ibox.pro.sdk.external.PaymentController;
import ibox.pro.sdk.external.entities.APIPrepareResult;
import ibox.pro.sdk.external.entities.PaymentProductItem;
import ibox.pro.sdk.external.entities.PaymentProductItemField;
import ibox.pro.sdk.external.entities.PreparedField;
import ibox.pro.sdk.external.example.CommonAsyncTask;
import ibox.pro.sdk.external.example.R;
import ibox.pro.sdk.external.example.utils.WTReParser;

public class ProductDialog extends Dialog implements View.OnClickListener, AdapterView.OnItemSelectedListener {
    private List<PaymentProductItem> mProducts;
    private PaymentProductItem mCurrentProduct;
    private Double mPreparedAmount = null;

    private TextView lblSpecs;
    private LinearLayout llFields;
    private Button btnPrepare;

    private Listener listener;

    public ProductDialog(@NonNull Context context, List<PaymentProductItem> products) {
        super(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        mProducts = products;
        init();
    }

    private void init() {
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getWindow().getAttributes().gravity = Gravity.CENTER_VERTICAL;
        setContentView(R.layout.dialog_product);
        Spinner spnProducts = findViewById(R.id.product_spn_products);
        lblSpecs = findViewById(R.id.product_lbl_specs);
        llFields = findViewById(R.id.product_ll_fields);

        findViewById(R.id.product_btn_ok).setOnClickListener(this);
        findViewById(R.id.product_btn_cancel).setOnClickListener(this);
        btnPrepare = findViewById(R.id.product_btn_prepare);
        btnPrepare.setOnClickListener(this);

        ArrayList<String> titles = new ArrayList<>(mProducts.size());
        for (PaymentProductItem product : mProducts)
            titles.add(product.getTitle());

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, titles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnProducts.setAdapter(adapter);
        spnProducts.setOnItemSelectedListener(this);
        spnProducts.setSelection(0);
    }

    private void clear() {
        mPreparedAmount = null;
        mCurrentProduct = null;
        btnPrepare.setEnabled(false);
        llFields.removeAllViews();
        lblSpecs.setText("");
    }

    private boolean isValid() {
        for (int i = 0; i < llFields.getChildCount(); i++) {
            View nextView = llFields.getChildAt(i);
            if (nextView instanceof ProductFieldView)
                if (!((ProductFieldView) nextView).isValid())
                    return false;
        }
        return true;
    }

    private String getValidReport() {
        for (int i = 0; i < llFields.getChildCount(); i++) {
            View nextView = llFields.getChildAt(i);
            if (nextView instanceof ProductFieldView)
                if (!((ProductFieldView) nextView).isValid())
                    return String.format(
                            getContext().getString(R.string.payment_toast_error_field_format),
                            ((ProductFieldView) nextView).getField().getTitle()
                        );
        }
        return "OK";
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setImage(String fieldCode, String path) {
        View imgFieldView = llFields.findViewWithTag(fieldCode);
        if (imgFieldView != null)
            ((ProductImageFieldView) imgFieldView).setImageFromPath(path);
    }

    public PaymentProductItem getSelectedProduct() {
        return mCurrentProduct;
    }

    public String getSelectedProductCode() {
        return  mCurrentProduct == null ? null : mCurrentProduct.getCode();
    }

    public String getTextFieldValue(String code) {
        View txtFieldView = llFields.findViewWithTag(code);
        if (txtFieldView != null)
            return ((ProductTextFieldView) txtFieldView).getValue();
        return null;
    }

    public HashMap<String, String> getTextValues() {
        if (mCurrentProduct != null) {
            HashMap<String, String> result = new HashMap<>();
            for (int i = 0; i < llFields.getChildCount(); i++) {
                View nextView = llFields.getChildAt(i);
                if (nextView instanceof ProductTextFieldView) {
                    ProductTextFieldView productTextFieldView = (ProductTextFieldView) nextView;
                    result.put(productTextFieldView.getField().getCode(), productTextFieldView.getValue());
                }
            }
            return result;
        }
        return null;
    }

    public HashMap<String, String> getValuesForPrepare() {
        if (mCurrentProduct != null) {
            HashMap<String, String> result = new HashMap<>();
            for (int i = 0; i < llFields.getChildCount(); i++) {
                View nextView = llFields.getChildAt(i);
                if (nextView instanceof ProductTextFieldView) {
                    ProductTextFieldView productTextFieldView = (ProductTextFieldView) nextView;
                    if (productTextFieldView.getField().isPreparable())
                        result.put(productTextFieldView.getField().getCode(), productTextFieldView.getValue());
                }
            }
            return result;
        }
        return null;
    }

    public HashMap<String, String> getImageValues() {
        if (mCurrentProduct != null) {
            HashMap<String, String> result = new HashMap<>();
            for (int i = 0; i < llFields.getChildCount(); i++) {
                View nextView = llFields.getChildAt(i);
                if (nextView instanceof ProductImageFieldView) {
                    ProductImageFieldView productImageFieldView = (ProductImageFieldView) nextView;
                    result.put(productImageFieldView.getField().getCode(), productImageFieldView.getImageFileName());
                }
            }
            return result;
        }
        return null;
    }

    public Double getPreparedAmount() {
        return mPreparedAmount;
    }

    @Override
    public void onClick(View view) {
        if (view instanceof ImageView) {
            if (listener != null)
                listener.onRequestSetImage(String.valueOf(view.getTag()));
        } else {
            int id = view.getId();
            if (id ==R.id.product_btn_ok && isValid())
                    dismiss();
            else if (id == R.id.product_btn_cancel)
                cancel();
            else if (id == R.id.product_btn_prepare)
                new PrepareProductTask(mCurrentProduct, getValuesForPrepare()).execute();
        }
    }

    @Override
    public void cancel() {
        clear();
        super.cancel();
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
        if (mCurrentProduct != mProducts.get(position)) {
            mCurrentProduct = mProducts.get(position);
            mPreparedAmount = null;
            boolean preparable = mCurrentProduct.isPreparable();
            btnPrepare.setEnabled(preparable);

            llFields.removeAllViews();
            lblSpecs.setText(String.format("APPLY : %1$s\nPREPARABLE : %2$s\nRECURRENT MODE : %3$s",
                    String.valueOf(mCurrentProduct.getApply()),
                    String.valueOf(mCurrentProduct.isPreparable()),
                    String.valueOf(mCurrentProduct.getRecurrentMode()))
            );

            for (PaymentProductItemField field : mCurrentProduct.getFields()) {
                if (!preparable || mCurrentProduct.prepareOptional() || field.isPreparable()) {
                    View vwField = null;
                    switch (field.getType()) {
                        case TEXT:
                            vwField = new ProductTextFieldView(getContext(), field);
                            break;
                        case IMAGE:
                            if (field.isUserVisible())
                                vwField = new ProductImageFieldView(getContext(), field);
                            break;
                    }
                    if (vwField != null) {
                        vwField.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                        vwField.setTag(field.getCode());
                        llFields.addView(vwField);
                    }
                }
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        clear();
    }

    private abstract class ProductFieldView extends LinearLayout {
        private PaymentProductItemField field;

        public ProductFieldView(Context context, @NonNull PaymentProductItemField field) {
            super(context);
            this.field = field;
        }

        public PaymentProductItemField getField() {
            return field;
        }

        abstract boolean isValid();
    }

    private class ProductTextFieldView extends ProductFieldView {
        private TextView lblHint;
        private EditText edtText;

        public ProductTextFieldView(Context context, @NonNull PaymentProductItemField field) {
            super(context, field);
            init();
        }

        private void init() {
            inflate(getContext(), R.layout.view_product_text_field, this);
            lblHint = findViewById(R.id.view_product_text_field_lbl_hint);
            edtText = findViewById(R.id.view_product_text_field_edt_text);

            lblHint.setText(getField().getTitle());
            if (getField().isNumeric())
                edtText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            if (getField().getTextMask() != null && getField().getTextMask().trim().length() > 0)
                edtText.addTextChangedListener(new MaskWatcher(getField().getTextMask()));
            edtText.setText(getField().getDefaultValue());

            if (getField().isTextMultiline())
                edtText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);

            if (!getField().isUserVisible()) {
                lblHint.setEnabled(false);
                edtText.setEnabled(false);
            }
        }

        @Override
        boolean isValid() {
            String regex = getField().getTextRegExp();
            String value = edtText.getText().toString();
            boolean required = getField().isRequired();

            return (regex == null || regex.trim().length() == 0)
                    ? (value.length() > 0 || !required)
                    : (value.length() > 0 ? value.matches(regex) : !required);
        }

        String getValue() {
            return edtText.getText().toString();
        }

        class MaskWatcher implements TextWatcher {
            private WTReParser maskParser;
            private boolean mChanging = false;
            private String mLastAcceptedValue = "";

            public MaskWatcher (String mask) {
                try {
                    maskParser = new WTReParser(mask);
                } catch (Exception e) {
                    maskParser = null;
                    edtText.setEnabled(false);
                    e.printStackTrace();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (mChanging || maskParser == null)
                    return;
                mChanging = true;
                try {
                    String str = maskParser.reformatString(s.toString());
                    if (str!=null) {
                        edtText.setText(str);
                        mLastAcceptedValue = str;
                    }
                    else {
                        edtText.setText(mLastAcceptedValue);
                    }
                    edtText.setSelection(edtText.getText().length());
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                    //Log.e("Error!", ex.toString());
                }
                mChanging = false;
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

        }
    }

    private class ProductImageFieldView extends ProductFieldView {
        private TextView lblHint;
        private ImageView imgImage;

        private String mImageFileName;

        public ProductImageFieldView(Context context, @NonNull PaymentProductItemField field) {
            super(context, field);
            init();
        }

        private void init() {
            inflate(getContext(), R.layout.view_product_image_field, this);
            lblHint = findViewById(R.id.view_product_image_field_lbl_hint);
            imgImage = findViewById(R.id.view_product_image_field_img);
            imgImage.setOnClickListener(ProductDialog.this);
            imgImage.setTag(getField().getCode());

            lblHint.setText(getField().getTitle());
        }

        private boolean isImageAttached() {
            return mImageFileName != null && (new File(mImageFileName).length() > 0);
        }

        void setImageFromPath(String path) {
            mImageFileName = path;
            Bitmap result = BitmapFactory.decodeFile(mImageFileName);
            imgImage.setImageBitmap(ThumbnailUtils.extractThumbnail(
                    result,
                    imgImage.getWidth(), imgImage.getHeight(),
                    ThumbnailUtils.OPTIONS_RECYCLE_INPUT
            ));
        }

        @Override
        boolean isValid() {
            return !getField().isRequired() || isImageAttached();
        }

        public String getImageFileName() {
            return mImageFileName;
        }
    }

    private class PrepareProductTask extends CommonAsyncTask<Void, Void, APIPrepareResult> {
        private PaymentProductItem paymentProduct;
        private HashMap<String,String> fields;

        PrepareProductTask(@NonNull PaymentProductItem paymentProduct, @NonNull HashMap<String,String> fields) {
            super(ProductDialog.this.getContext());
            this.paymentProduct = paymentProduct;
            this.fields = fields;
        }

        @Override
        protected APIPrepareResult doInBackground(Void... voids) {
            return PaymentController.getInstance().prepareProduct(getContext(), paymentProduct.getCode(), fields);
        }

        @Override
        protected void onPostExecute(APIPrepareResult result) {
            super.onPostExecute(result);

            if (result != null) {
                if (result.isValid()) {
                    if (result.getFields() != null) {
                        for (PreparedField preparedField : result.getFields()) {
                            if (preparedField.isPaymentAmount()) {
                                try {
                                    mPreparedAmount = Double.parseDouble(preparedField.getValue());
                                } catch (NumberFormatException e) {
                                    e.printStackTrace();
                                    Toast.makeText(getContext(), R.string.common_error, Toast.LENGTH_LONG).show();
                                }
                            } else {
                                PaymentProductItemField field = null;
                                for (PaymentProductItemField productField : paymentProduct.getFields()) {
                                    if (productField.getCode().equals(preparedField.getCode())) {
                                        field = productField;
                                        break;
                                    }
                                }

                                if (field != null && field.getType() == PaymentProductItemField.Type.TEXT) {
                                    if (llFields.findViewWithTag(field.getCode()) != null) {
                                        ProductTextFieldView vwField = llFields.findViewWithTag(field.getCode());
                                        vwField.edtText.setText(preparedField.getValue());
                                    } else {
                                        ProductTextFieldView vwField = new ProductTextFieldView(getContext(), field);
                                        vwField.edtText.setText(preparedField.getValue());
                                        vwField.edtText.setEnabled(mCurrentProduct.isPreparableEditable() && field.isUserVisible());
                                        vwField.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                                        vwField.setTag(field.getCode());
                                        llFields.addView(vwField);
                                    }
                                }
                            }
                        }
                    }
                } else
                    Toast.makeText(getContext(), result.getErrorMessage(), Toast.LENGTH_LONG).show();
            } else
                Toast.makeText(getContext(), R.string.error_no_response, Toast.LENGTH_LONG).show();
        }
    }

    public interface Listener {
        void onRequestSetImage(String fieldCode);
    }
}
