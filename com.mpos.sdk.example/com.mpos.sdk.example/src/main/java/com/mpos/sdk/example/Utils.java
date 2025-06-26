package com.mpos.sdk.example;

import android.app.Activity;
import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mpos.sdk.entities.Account;
import com.mpos.sdk.entities.PaymentProductItemField;
import com.mpos.sdk.entities.Purchase;
import com.mpos.sdk.entities.Tax;
import com.mpos.sdk.entities.TaxContribution;
import com.mpos.sdk.entities.TransactionItem;

public class Utils {
    public static String getString(Activity activity, String key) {
        return activity.getSharedPreferences(activity.getApplicationContext().getPackageName(), Context.MODE_PRIVATE).getString(key, "");
    }

    public static void setString(Activity activity,String key, String value) {
        activity.getSharedPreferences(activity.getApplicationContext().getPackageName(), Context.MODE_PRIVATE).edit().putString(key, value).commit();
    }

    private static final int S_DECIMALS = MainActivity.CURRENCY.getE();
    private static final int Q_DECIMALS = 3;
    private static final HashMap<String, BigDecimal> TAX_RATES = new HashMap<String, BigDecimal>();
    static {
        TAX_RATES.put(Purchase.TaxCode.VAT_NA, BigDecimal.ZERO.setScale(S_DECIMALS, RoundingMode.HALF_UP));
        TAX_RATES.put(Purchase.TaxCode.VAT_0, BigDecimal.ZERO.setScale(S_DECIMALS, RoundingMode.HALF_UP));
        TAX_RATES.put(Purchase.TaxCode.VAT_10, BigDecimal.valueOf(0.1d).setScale(S_DECIMALS, RoundingMode.HALF_UP));
        TAX_RATES.put(Purchase.TaxCode.VAT_18, BigDecimal.valueOf(0.18d).setScale(S_DECIMALS, RoundingMode.HALF_UP));
        TAX_RATES.put(Purchase.TaxCode.VAT_20, BigDecimal.valueOf(0.2d).setScale(S_DECIMALS, RoundingMode.HALF_UP));
        TAX_RATES.put(Purchase.TaxCode.VAT_110, BigDecimal.valueOf(0.1d).setScale(S_DECIMALS, RoundingMode.HALF_UP));
        TAX_RATES.put(Purchase.TaxCode.VAT_120, BigDecimal.valueOf(0.2d).setScale(S_DECIMALS, RoundingMode.HALF_UP));
    }

    private static HashMap<String, BigDecimal> CalculateTaxes(TransactionItem.TaxMode taxMode, BigDecimal total, List<String> appliedTaxes) {
        HashMap<String, BigDecimal> result = new HashMap<String, BigDecimal>();
        if (appliedTaxes != null && appliedTaxes.size() > 0 && taxMode != null) {
            BigDecimal taxAmount = total.setScale(S_DECIMALS, RoundingMode.HALF_UP);
            for (String taxCode : appliedTaxes) {
                if (taxMode == TransactionItem.TaxMode.FOR_EACH) {
                    if (!taxCode.equals(Purchase.TaxCode.VAT_NA) && !taxCode.equals(Purchase.TaxCode.VAT_0)) {
                        if (!TAX_RATES.containsKey(taxCode))
                            throw new IllegalArgumentException("invalid tax code :" + taxCode);
                        BigDecimal taxRate = TAX_RATES.get(taxCode);
                        taxAmount = taxAmount.subtract(total.divide(taxRate.add(BigDecimal.ONE), RoundingMode.HALF_UP).setScale(S_DECIMALS, RoundingMode.HALF_UP));
                    }
                }

                result.put(taxCode, result.containsKey(taxCode) ? (result.get(taxCode).add(taxAmount)) : taxAmount);
            }
        } else
            result.put(Purchase.TaxCode.VAT_NA, result.containsKey(Purchase.TaxCode.VAT_NA) ? (result.get(Purchase.TaxCode.VAT_NA).add(total)) : total);

        if (result.size() == 0)
            throw new IllegalArgumentException("failed to calculate taxes");
        return result;
    }

    private static List<String> SplitString(String str, int size) {
        List<String> result = new ArrayList<String>();
        for (int i = 0; i < str.length(); i += size)
            result.add(str.substring(i, i + Math.min(size, str.length() - i)));
        return result;
    }

    private static void AppendKeyValue(StringBuilder builder, int lineWidth, String key, String value) {
        boolean multiline = key.length() + 1 + value.length() > lineWidth;
        if (multiline) {
            for (String keyChunk : SplitString(key, lineWidth))
                builder.append(keyChunk).append("\n");
            for (String valueChunk : SplitString(value, lineWidth))
                builder.append(valueChunk.length() == lineWidth ? valueChunk : String.format("%1$-" + valueChunk.length() + "s", valueChunk)).append("\n");
        } else
            builder.append(String.format("%1$-" + (key.length() + 1) + "s%2$" + (lineWidth - (key.length() + 1)) + "s", key, value)).append("\n");
    }

    public static List<Purchase> GetPurchasesFromJson(String json) {
        try {
            List<Purchase> result = new ArrayList<Purchase>();
            JSONObject o = new JSONObject(json);
            JSONArray purchases = o.getJSONArray("Purchases");
            if (purchases != null)
                for (int i = 0; i < purchases.length(); i++)
                    result.add(new Purchase(purchases.getJSONObject(i)));
            return result;
        } catch (JSONException e) {
            return null;
        }
    }

    public static final int TAPE_WIDTH = 37;
    public static String BuildInvoice(Account account, TransactionItem transaction) {
        int lineWidth = TAPE_WIDTH;
        char [] ca_divider = new char[lineWidth];
        Arrays.fill(ca_divider, '-');
        String divider = new String(ca_divider) + "\n";
        StringBuilder invoiceBuilder = new StringBuilder()
            .append("-----------------ЧЕК-----------------").append("\n");

        AppendKeyValue(invoiceBuilder, lineWidth, transaction.getState() == 500 ? "ВОЗВРАТ ПРИХОДА" : "ПРИХОД", " ");
        AppendKeyValue(invoiceBuilder, lineWidth, "КАССИР", account.getName());
        AppendKeyValue(invoiceBuilder, lineWidth, "ДАТА И ВРЕМЯ", new SimpleDateFormat("dd.MM.yy HH:mm:ss").format(transaction.getDate()));
        AppendKeyValue(invoiceBuilder, lineWidth, "НОМЕР ЧЕКА", transaction.getInvoice());

        boolean paidByCard = transaction.getCard() != null && !transaction.getCard().isCash() && !transaction.getCard().isCredit() && !transaction.getCard().isPrepaid();
        if (paidByCard)
            AppendKeyValue(invoiceBuilder, lineWidth, "КОД ПОДТВЕРЖДЕНИЯ", transaction.getApprovalCode());

        BigDecimal tranAmount = BigDecimal.valueOf(transaction.getAmount()).setScale(S_DECIMALS, RoundingMode.HALF_UP);
        BigDecimal invoiceTotal = BigDecimal.ZERO.setScale(S_DECIMALS, RoundingMode.HALF_UP);
        BigDecimal cashGot = BigDecimal.valueOf(transaction.getAmountCashGot()).setScale(S_DECIMALS, RoundingMode.HALF_UP);
        TransactionItem.TaxMode taxMode = transaction.getTaxMode();
        List<Purchase> purchases = transaction.getPurchases();
        List<TransactionItem.Product> products = transaction.getProducts();

        HashMap taxCalcs = new HashMap<String, BigDecimal>();
        taxCalcs.put(Purchase.TaxCode.VAT_NA, BigDecimal.ZERO.setScale(S_DECIMALS, RoundingMode.HALF_UP));
        taxCalcs.put(Purchase.TaxCode.VAT_0, BigDecimal.ZERO.setScale(S_DECIMALS, RoundingMode.HALF_UP));
        taxCalcs.put(Purchase.TaxCode.VAT_10, BigDecimal.ZERO.setScale(S_DECIMALS, RoundingMode.HALF_UP));
        taxCalcs.put(Purchase.TaxCode.VAT_18, BigDecimal.ZERO.setScale(S_DECIMALS, RoundingMode.HALF_UP));
        taxCalcs.put(Purchase.TaxCode.VAT_20, BigDecimal.ZERO.setScale(S_DECIMALS, RoundingMode.HALF_UP));
        taxCalcs.put(Purchase.TaxCode.VAT_110, BigDecimal.ZERO.setScale(S_DECIMALS, RoundingMode.HALF_UP));
        taxCalcs.put(Purchase.TaxCode.VAT_120, BigDecimal.ZERO.setScale(S_DECIMALS, RoundingMode.HALF_UP));

        boolean hasPurchases = purchases != null && purchases.size() > 0;
        boolean hasProducts = products != null && products.size() > 0;

        if ((hasPurchases || hasProducts) && transaction.getDescription() != null && transaction.getDescription().trim().length() > 0)
            invoiceBuilder.append(transaction.getDescription()).append("\n");
        invoiceBuilder.append(divider);

        if (hasPurchases) {
            for (Purchase purchase : purchases) {
                BigDecimal price = BigDecimal.valueOf(purchase.getPrice()).setScale(S_DECIMALS, RoundingMode.HALF_UP);
                BigDecimal quantity = BigDecimal.valueOf(purchase.getQuantity()).setScale(Q_DECIMALS, RoundingMode.HALF_UP);
                BigDecimal calcTotal = price.multiply(quantity).setScale(S_DECIMALS, RoundingMode.HALF_UP);
                BigDecimal total = purchase.getTitleAmount() == null
                        ? calcTotal
                        : BigDecimal.valueOf(purchase.getTitleAmount()).setScale(S_DECIMALS, RoundingMode.HALF_UP);;
                invoiceTotal = invoiceTotal.add(total);
                AppendKeyValue(invoiceBuilder, lineWidth, purchase.getTitle(), String.format("%1$5.3fx%2$5.2f=%3$5.2f", quantity, price, total));
                if (calcTotal.compareTo(total) != 0)
                    AppendKeyValue(invoiceBuilder, lineWidth, "НАДБАВКА", String.format("=%1$.2f", total.subtract(calcTotal).setScale(S_DECIMALS, RoundingMode.HALF_UP).doubleValue()));

                HashMap<String, BigDecimal> purchaseTaxes = CalculateTaxes(taxMode, total, purchase.getTaxCodes());
                for (Map.Entry<String, BigDecimal> purchaseTax : purchaseTaxes.entrySet())
                {
                    String taxCode = purchaseTax.getKey();
                    BigDecimal taxAmount = purchaseTax.getValue();
                    if (taxCalcs.containsKey(taxCode))
                        taxCalcs.put(taxCode, ((BigDecimal)taxCalcs.get(taxCode)).add(taxAmount));
                    else
                        taxCalcs.put(taxCode, taxAmount);
                }
            }
        }
        else
        {
            String description = transaction.getDescription();
            if (hasProducts) {
                StringBuilder descriptionBuilder = new StringBuilder();
                for (int i = 0; i < transaction.getProducts().size(); i++) {
                    TransactionItem.Product nextProduct = transaction.getProducts().get(i);
                    descriptionBuilder.append(nextProduct.getDescription().getTitleReceipt()).append("\n");
                    ArrayList<String> fields = new ArrayList<>();
                    for (TransactionItem.ProductField field : nextProduct.getFields()) {
                        StringBuilder fieldBuilder = new StringBuilder();
                        if (field.getDescription().printInReceipt() && field.getTextValue() != null) {
                            fieldBuilder.delete(0, fieldBuilder.length());
                            fieldBuilder.append(field.getDescription().getTitleReceipt());
                            if (field.getTextValue().length() + field.getDescription().getTitleReceipt().length() < lineWidth && field.getTextValue().split("\n").length == 1) {
                                char [] spaces = new char [lineWidth - (field.getTextValue().length() + field.getDescription().getTitleReceipt().length())];
                                Arrays.fill(spaces, ' ');
                                fieldBuilder.append(spaces);
                                fieldBuilder.append(field.getTextValue());
                                fields.add(fieldBuilder.toString());
                            } else {
                                fields.add(fieldBuilder.toString());
                                fieldBuilder.delete(0, fieldBuilder.length());
                                if (field.getTextValue().length() < lineWidth && field.getTextValue().split("\n").length == 1) {
                                    char [] spaces = new char [lineWidth - field.getTextValue().length()];
                                    Arrays.fill(spaces, ' ');
                                    fieldBuilder.append(spaces);
                                    fieldBuilder.append(field.getTextValue());
                                    fields.add(fieldBuilder.toString());
                                } else {
                                    fields.add(fieldBuilder.toString());
                                    String [] lines = field.getTextValue().split("\n");
                                    for (String line : lines) {
                                        fieldBuilder.delete(0, fieldBuilder.length());
                                        if (line.length() < lineWidth) {
                                            char [] spaces = new char [lineWidth - line.length()];
                                            Arrays.fill(spaces, ' ');
                                            fieldBuilder.append(spaces);
                                            fieldBuilder.append(line);
                                            fields.add(fieldBuilder.toString());
                                        } else {
                                            fields.add(line);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    for (String field : fields)
                        descriptionBuilder.append(field).append("\n");
                    description = descriptionBuilder.toString().trim();
                }
            }

            BigDecimal price = tranAmount;
            BigDecimal quantity = BigDecimal.ONE.setScale(Q_DECIMALS, RoundingMode.HALF_UP);
            BigDecimal total = price.multiply(quantity).setScale(S_DECIMALS, RoundingMode.HALF_UP);
            invoiceTotal = invoiceTotal.add(total);
            if (hasProducts) {
                invoiceBuilder.append(description).append("\n");
                String amount = String.format("%1$5.3fx%2$5.2f=%3$5.2f", quantity, price, total);
                String spaces = "";
                if (amount.length() < lineWidth - 1) {
                    char[] c_spaces = new char[lineWidth - 1 - amount.length()];
                    Arrays.fill(c_spaces, ' ');
                    spaces = new String(c_spaces);
                }
                AppendKeyValue(invoiceBuilder, lineWidth, spaces, amount);
            } else
                AppendKeyValue(invoiceBuilder, lineWidth, description, String.format("%1$5.3fx%2$5.2f=%3$5.2f", quantity, price, total));

            List<String> appliedTaxes = null;
            if (transaction.getTaxes() != null) {
                appliedTaxes = new ArrayList<String>(transaction.getTaxes().size());
                for (Tax tax : transaction.getTaxes())
                    appliedTaxes.add(tax.getCode());
            }
            taxCalcs = CalculateTaxes(taxMode, total, appliedTaxes);
        }
        if (transaction.getTaxContributions() != null && transaction.getTaxContributions().size() > 0)
        {
            taxCalcs.clear();
            for (TaxContribution tc : transaction.getTaxContributions())
                taxCalcs.put(tc.getCode(), BigDecimal.valueOf(tc.getTotal()).setScale(S_DECIMALS, RoundingMode.HALF_UP));
        }
        invoiceBuilder.append(divider);
        AppendKeyValue(invoiceBuilder, lineWidth, "ИТОГ", String.format("%.2f", invoiceTotal));
        invoiceBuilder.append(divider);

        AppendKeyValue(invoiceBuilder, lineWidth, "НАЛИЧНЫМИ", String.format("%.2f", (transaction.getCard().isCash() ? cashGot : 0d)));
        AppendKeyValue(invoiceBuilder, lineWidth, "ЭЛЕКТРОННЫМИ", String.format("%.2f", (paidByCard ? tranAmount.doubleValue() : 0d)));
        AppendKeyValue(invoiceBuilder, lineWidth, "ПРЕДВАРИТЕЛЬНАЯ ОПЛАТА (АВАНС)", String.format("%.2f", (transaction.getCard().isPrepaid() ? tranAmount.doubleValue() : invoiceTotal.subtract(tranAmount).doubleValue())));
        AppendKeyValue(invoiceBuilder, lineWidth, "ПОСЛЕДУЮЩАЯ ОПЛАТА (КРЕДИТ)", String.format("%.2f", (transaction.getCard().isCredit() ? tranAmount.doubleValue() : 0d)));
        AppendKeyValue(invoiceBuilder, lineWidth, "СДАЧА", String.format("%.2f", (transaction.getCard().isCash() ? cashGot.subtract(tranAmount).doubleValue() : 0d)));

        AppendKeyValue(invoiceBuilder, lineWidth, "Сумма без НДС", String.format("%.2f", taxCalcs.containsKey(Purchase.TaxCode.VAT_NA) ? taxCalcs.get(Purchase.TaxCode.VAT_NA) : 0d));
        AppendKeyValue(invoiceBuilder, lineWidth, "Сумма по НДС 0%", String.format("%.2f", taxCalcs.containsKey(Purchase.TaxCode.VAT_0) ? taxCalcs.get(Purchase.TaxCode.VAT_0) : 0d));
        AppendKeyValue(invoiceBuilder, lineWidth, "Сумма НДС 10%", String.format("%.2f", taxCalcs.containsKey(Purchase.TaxCode.VAT_10) ? taxCalcs.get(Purchase.TaxCode.VAT_10) : 0d));
        AppendKeyValue(invoiceBuilder, lineWidth, "Сумма НДС 18%", String.format("%.2f", taxCalcs.containsKey(Purchase.TaxCode.VAT_18) ? taxCalcs.get(Purchase.TaxCode.VAT_18) : 0d));
        AppendKeyValue(invoiceBuilder, lineWidth, "Сумма НДС 20%", String.format("%.2f", taxCalcs.containsKey(Purchase.TaxCode.VAT_20) ? taxCalcs.get(Purchase.TaxCode.VAT_20) : 0d));
        AppendKeyValue(invoiceBuilder, lineWidth, "Сумма НДС 10/110", String.format("%.2f", taxCalcs.containsKey(Purchase.TaxCode.VAT_110) ? taxCalcs.get(Purchase.TaxCode.VAT_110) : 0d));
        AppendKeyValue(invoiceBuilder, lineWidth, "Сумма НДС 20/120", String.format("%.2f", taxCalcs.containsKey(Purchase.TaxCode.VAT_120) ? taxCalcs.get(Purchase.TaxCode.VAT_120) : 0d));

        if (transaction.getFiscalInfo() != null)
        {
            invoiceBuilder.append("--------------ФИСК. БЛОК-------------\n");
            boolean fz54 = transaction.getFiscalInfo().getFiscalMark() != null;
            if (fz54)
            {
                AppendKeyValue(invoiceBuilder, lineWidth, new SimpleDateFormat("dd.MM.yy HH:mm").format(transaction.getFiscalInfo().getFiscalDatetime()), " ");
                AppendKeyValue(invoiceBuilder, lineWidth, "ИНН", account.getTaxID());
                AppendKeyValue(invoiceBuilder, lineWidth, "СНО", transaction.getTaxSystemName());
                AppendKeyValue(invoiceBuilder, lineWidth, "САЙТ ФНС", "http://nalog.ru");
                AppendKeyValue(invoiceBuilder, lineWidth, "СМЕНА №", String.valueOf(transaction.getFiscalInfo().getFiscalPrinterShift()));
                AppendKeyValue(invoiceBuilder, lineWidth, "ЧЕК №", String.valueOf(transaction.getFiscalInfo().getFiscalDocSN()));
                AppendKeyValue(invoiceBuilder, lineWidth, "ЗН ККТ", transaction.getFiscalInfo().getFiscalDeviceID());
                AppendKeyValue(invoiceBuilder, lineWidth, "РН ККТ", transaction.getFiscalInfo().getFiscalDeviceRegNumber());
                AppendKeyValue(invoiceBuilder, lineWidth, "ФН", transaction.getFiscalInfo().getFiscalStorageNumber());
                AppendKeyValue(invoiceBuilder, lineWidth, "ФД", transaction.getFiscalInfo().getFiscalDocumentNumber());
                AppendKeyValue(invoiceBuilder, lineWidth, "ФПД", transaction.getFiscalInfo().getFiscalMark());
            }
            else if (transaction.getFiscalInfo().getCVC() != null && transaction.getFiscalInfo().getCVC().trim().length() > 0)
            {
                AppendKeyValue(invoiceBuilder, lineWidth, "ИНН", account.getTaxID());
                AppendKeyValue(invoiceBuilder, lineWidth, "СМЕНА №", String.valueOf(transaction.getFiscalInfo().getFiscalPrinterShift()));
                AppendKeyValue(invoiceBuilder, lineWidth, "ДОКУМЕНТ №", String.valueOf(transaction.getFiscalInfo().getFiscalDocSN()));
                AppendKeyValue(invoiceBuilder, lineWidth, "ЗН ККТ", transaction.getFiscalInfo().getFiscalDeviceID());
                AppendKeyValue(invoiceBuilder, lineWidth, "КПК", String.format("%.2f", transaction.getFiscalInfo().getCVC()));
            }
            invoiceBuilder.append("-----------КОНЕЦ ФИСК. БЛОКА---------\n");
        }
        invoiceBuilder.append("--------------КОНЕЦ ЧЕКА-------------\n");

        return invoiceBuilder.toString().trim();
    }
}
