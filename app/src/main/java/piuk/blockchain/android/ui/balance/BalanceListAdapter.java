package piuk.blockchain.android.ui.balance;

import android.graphics.Color;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import info.blockchain.wallet.contacts.data.FacilitatedTransaction;
import info.blockchain.wallet.multiaddress.TransactionSummary;
import info.blockchain.wallet.multiaddress.TransactionSummary.Direction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import piuk.blockchain.android.R;
import piuk.blockchain.android.data.contacts.ContactTransactionModel;
import piuk.blockchain.android.util.DateUtil;
import piuk.blockchain.android.util.MonetaryUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.SpanFormatter;
import piuk.blockchain.android.util.StringUtils;

public class BalanceListAdapter extends RecyclerView.Adapter {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_FCTX = 1;
    private static final int VIEW_TYPE_TRANSACTION = 2;

    private List<Object> objects;
    private PrefsUtil prefsUtil;
    private MonetaryUtil monetaryUtil;
    private StringUtils stringUtils;
    private DateUtil dateUtil;
    private double btcExchangeRate;
    private boolean isBtc;
    private BalanceListClickListener listClickListener;
    private HashMap<String, String> contactsTransactionMap;
    private HashMap<String, String> notesTransactionMap;

    public BalanceListAdapter(HashMap<String, String> contactsTransactionMap,
                              HashMap<String, String> notesTransactionMap,
                              PrefsUtil prefsUtil,
                              MonetaryUtil monetaryUtil,
                              StringUtils stringUtils,
                              DateUtil dateUtil,
                              double btcExchangeRate,
                              boolean isBtc) {

        objects = new ArrayList<>();
        this.contactsTransactionMap = contactsTransactionMap;
        this.notesTransactionMap = notesTransactionMap;
        this.prefsUtil = prefsUtil;
        this.monetaryUtil = monetaryUtil;
        this.stringUtils = stringUtils;
        this.dateUtil = dateUtil;
        this.btcExchangeRate = btcExchangeRate;
        this.isBtc = isBtc;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEW_TYPE_HEADER:
                View header = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_accounts_row_header, parent, false);
                return new HeaderViewHolder(header);
            case VIEW_TYPE_FCTX:
                View fctxView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact_transactions, parent, false);
                return new FctxViewHolder(fctxView);
            default:
                View txView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_balance, parent, false);
                return new TxViewHolder(txView);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        switch (getItemViewType(position)) {
            case VIEW_TYPE_HEADER:
                bindHeaderView(holder, position);
                break;
            case VIEW_TYPE_FCTX:
                bindFctxView(holder, position);
                break;
            default:
                bindTxView(holder, position);
                break;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position, List payloads) {
        onBindViewHolder(holder, position);
    }

    private void bindHeaderView(RecyclerView.ViewHolder holder, int position) {
        final HeaderViewHolder headerViewHolder = (HeaderViewHolder) holder;
        final String header = (String) objects.get(position);
        headerViewHolder.header.setText(header);
    }

    private void bindFctxView(RecyclerView.ViewHolder holder, int position) {

        final FctxViewHolder fctxViewHolder = (FctxViewHolder) holder;
        final ContactTransactionModel model = (ContactTransactionModel) objects.get(position);
        final FacilitatedTransaction transaction = model.getFacilitatedTransaction();
        final String contactName = model.getContactName();

        // Click listener
        holder.itemView.setOnClickListener(view -> {
            if (listClickListener != null) listClickListener.onFctxClicked(transaction.getId());
        });

        holder.itemView.setOnLongClickListener(view -> {
            if (listClickListener != null) listClickListener.onFctxLongClicked(transaction.getId());
            return true;
        });

        fctxViewHolder.indicator.setVisibility(View.GONE);
        fctxViewHolder.title.setTextColor(ContextCompat.getColor(fctxViewHolder.title.getContext(), R.color.black));

        double btcBalance = transaction.getIntendedAmount() / 1e8;
        double fiatBalance = btcExchangeRate * btcBalance;

        String fiatString = prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
        Spannable amountSpannable = getDisplaySpannable(transaction.getIntendedAmount(), fiatBalance, fiatString);

        if (transaction.getState().equals(FacilitatedTransaction.STATE_DECLINED)) {
            fctxViewHolder.title.setText(R.string.contacts_receiving_declined);

        } else if (transaction.getState().equals(FacilitatedTransaction.STATE_CANCELLED)) {
            fctxViewHolder.title.setText(R.string.contacts_receiving_cancelled);

        } else if (transaction.getState().equals(FacilitatedTransaction.STATE_WAITING_FOR_ADDRESS)) {
            if (transaction.getRole().equals(FacilitatedTransaction.ROLE_PR_RECEIVER)) {

                Spanned display = SpanFormatter.format(
                        stringUtils.getString(R.string.contacts_receiving_from_contact_waiting_to_accept),
                        amountSpannable,
                        contactName);
                fctxViewHolder.title.setText(display);
                fctxViewHolder.indicator.setVisibility(View.VISIBLE);

            } else if (transaction.getRole().equals(FacilitatedTransaction.ROLE_RPR_INITIATOR)) {

                Spanned display = SpanFormatter.format(
                        stringUtils.getString(R.string.contacts_sending_to_contact_waiting),
                        amountSpannable,
                        contactName);
                fctxViewHolder.title.setText(display);
            }
        } else if (transaction.getState().equals(FacilitatedTransaction.STATE_WAITING_FOR_PAYMENT)) {
            if (transaction.getRole().equals(FacilitatedTransaction.ROLE_RPR_RECEIVER)) {

                Spanned display = SpanFormatter.format(
                        stringUtils.getString(R.string.contacts_payment_requested_ready_to_send),
                        amountSpannable,
                        contactName);
                fctxViewHolder.title.setText(display);
                fctxViewHolder.indicator.setVisibility(View.VISIBLE);

            } else if (transaction.getRole().equals(FacilitatedTransaction.ROLE_PR_INITIATOR)) {

                Spanned display = SpanFormatter.format(
                        stringUtils.getString(R.string.contacts_requesting_from_contact_waiting_for_payment),
                        amountSpannable,
                        contactName);
                fctxViewHolder.title.setText(display);
            }
        }

        fctxViewHolder.subtitle.setText(transaction.getNote());
    }

    private void bindTxView(RecyclerView.ViewHolder holder, int position) {

        final TxViewHolder txViewHolder = (TxViewHolder) holder;
        final TransactionSummary tx = (TransactionSummary) objects.get(position);
        String fiatString = prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
        double btcBalance = tx.getTotal().longValue() / 1e8;
        double fiatBalance = btcExchangeRate * btcBalance;

        txViewHolder.result.setTextColor(Color.WHITE);
        txViewHolder.timeSince.setText(dateUtil.formatted(tx.getTime()));

        switch (tx.getDirection()) {
            case TRANSFERRED:
                txViewHolder.direction.setText(
                        txViewHolder.direction.getContext().getString(R.string.MOVED));
                break;
            case RECEIVED:
                if (contactsTransactionMap.containsKey(tx.getHash())) {
                    String contactName = contactsTransactionMap.get(tx.getHash());
                    txViewHolder.direction.setText(
                            txViewHolder.direction.getContext().getString(R.string.contacts_received, contactName));
                } else {
                    txViewHolder.direction.setText(
                            txViewHolder.direction.getContext().getString(R.string.RECEIVED));
                }
                break;
            case SENT:
                if (contactsTransactionMap.containsKey(tx.getHash())) {
                    String contactName = contactsTransactionMap.get(tx.getHash());
                    txViewHolder.direction.setText(
                            txViewHolder.direction.getContext().getString(R.string.contacts_sent, contactName));
                } else {
                    txViewHolder.direction.setText(
                            txViewHolder.direction.getContext().getString(R.string.SENT));
                }
                break;
        }

        if (notesTransactionMap.containsKey(tx.getHash())) {
            txViewHolder.note.setText(notesTransactionMap.get(tx.getHash()));
            txViewHolder.note.setVisibility(View.VISIBLE);
        } else {
            txViewHolder.note.setVisibility(View.GONE);
        }

        txViewHolder.result.setText(getDisplaySpannable(tx.getTotal().longValue(), fiatBalance, fiatString));

        if (tx.getDirection() == Direction.TRANSFERRED) {
            txViewHolder.result.setBackgroundResource(
                    getColorForConfirmations(tx, R.drawable.rounded_view_transferred_50, R.drawable.rounded_view_transferred));

            txViewHolder.direction.setTextColor(ContextCompat.getColor(txViewHolder.direction.getContext(),
                    getColorForConfirmations(tx, R.color.product_gray_transferred_50, R.color.product_gray_transferred)));

        } else if (tx.getDirection() == Direction.SENT) {
            txViewHolder.result.setBackgroundResource(
                    getColorForConfirmations(tx, R.drawable.rounded_view_red_50, R.drawable.rounded_view_red));

            txViewHolder.direction.setTextColor(ContextCompat.getColor(txViewHolder.direction.getContext(),
                    getColorForConfirmations(tx, R.color.product_red_sent_50, R.color.product_red_sent)));

        } else {
            txViewHolder.result.setBackgroundResource(
                    getColorForConfirmations(tx, R.drawable.rounded_view_green_50, R.drawable.rounded_view_green));

            txViewHolder.direction.setTextColor(ContextCompat.getColor(txViewHolder.direction.getContext(),
                    getColorForConfirmations(tx, R.color.product_green_received_50, R.color.product_green_received)));
        }

        txViewHolder.watchOnly.setVisibility(tx.isWatchOnly() ? View.VISIBLE : View.GONE);
        txViewHolder.doubleSpend.setVisibility(tx.isDoubleSpend() ? View.VISIBLE : View.GONE);

        txViewHolder.result.setOnClickListener(v -> {
            onViewFormatUpdated(!isBtc);
            if (listClickListener != null) listClickListener.onValueClicked(isBtc);
        });

        txViewHolder.itemView.setOnClickListener(v -> {
            if (listClickListener != null) {
                listClickListener.onTransactionClicked(
                        getRealTxPosition(txViewHolder.getAdapterPosition()), position);
            }
        });
    }

    private Spannable getDisplaySpannable(double btcAmount, double fiatAmount, String fiatString) {
        Spannable spannable;
        if (isBtc) {
            spannable = Spannable.Factory.getInstance().newSpannable(
                    monetaryUtil.getDisplayAmountWithFormatting(Math.abs(btcAmount)) + " " + getDisplayUnits());
            spannable.setSpan(
                    new RelativeSizeSpan(0.67f),
                    spannable.length() - getDisplayUnits().length(),
                    spannable.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else {
            spannable = Spannable.Factory.getInstance().newSpannable(
                    monetaryUtil.getFiatFormat(fiatString).format(Math.abs(fiatAmount)) + " " + fiatString);
            spannable.setSpan(
                    new RelativeSizeSpan(0.67f),
                    spannable.length() - 3,
                    spannable.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return spannable;
    }

    @Override
    public int getItemCount() {
        return objects != null ? objects.size() : 0;
    }

    @Override
    public int getItemViewType(int position) {
        if (objects.get(position) instanceof String) {
            return VIEW_TYPE_HEADER;
        } else if (objects.get(position) instanceof ContactTransactionModel) {
            return VIEW_TYPE_FCTX;
        } else if (objects.get(position) instanceof TransactionSummary) {
            return VIEW_TYPE_TRANSACTION;
        } else {
            throw new IllegalArgumentException(
                    "Object list contained unsupported item: " + objects.get(position));
        }
    }

    private int getRealTxPosition(int position) {
        int totalTransactions = 0;
        for (Object object : objects) {
            if (object instanceof TransactionSummary) {
                totalTransactions++;
            }
        }

        int diff = getItemCount() - totalTransactions;
        return position - diff;
    }

    private int getColorForConfirmations(TransactionSummary tx, @DrawableRes int colorLight, @DrawableRes int colorDark) {
        return tx.getConfirmations() < 3 ? colorLight : colorDark;
    }

    private String getDisplayUnits() {
        return (String) monetaryUtil.getBTCUnits()[prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)];
    }

    public void onTransactionsUpdated(List<Object> objects) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new BalanceDiffUtil(this.objects, objects));
        this.objects = objects;
        diffResult.dispatchUpdatesTo(this);
    }

    public void setTxListClickListener(BalanceListClickListener listClickListener) {
        this.listClickListener = listClickListener;
    }

    public void onViewFormatUpdated(boolean isBtc) {
        if (this.isBtc != isBtc) {
            this.isBtc = isBtc;
            notifyAdapterDataSetChanged(null);
        }
    }

    public void onContactsMapChanged(HashMap<String, String> contactsTransactionMap,
                                     HashMap<String, String> notesTransactionMap) {
        this.contactsTransactionMap = contactsTransactionMap;
        this.notesTransactionMap = notesTransactionMap;
        notifyDataSetChanged();
    }

    public void notifyAdapterDataSetChanged(@Nullable Double btcExchangeRate) {
        if (btcExchangeRate != null && this.btcExchangeRate != btcExchangeRate) {
            this.btcExchangeRate = btcExchangeRate;
        }
        monetaryUtil.updateUnit(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));
        notifyDataSetChanged();
    }

    public interface BalanceListClickListener {

        void onTransactionClicked(int correctedPosition, int absolutePosition);

        void onValueClicked(boolean isBtc);

        void onFctxClicked(String fctxId);

        void onFctxLongClicked(String fctxId);
    }

    private static class TxViewHolder extends RecyclerView.ViewHolder {

        TextView result;
        TextView timeSince;
        TextView direction;
        TextView watchOnly;
        TextView note;
        ImageView doubleSpend;

        TxViewHolder(View view) {
            super(view);
            result = (TextView) view.findViewById(R.id.result);
            timeSince = (TextView) view.findViewById(R.id.ts);
            direction = (TextView) view.findViewById(R.id.direction);
            watchOnly = (TextView) view.findViewById(R.id.watch_only);
            note = (TextView) view.findViewById(R.id.note);
            doubleSpend = (ImageView) view.findViewById(R.id.double_spend_warning);
        }
    }

    private static class FctxViewHolder extends RecyclerView.ViewHolder {

        ImageView indicator;
        TextView title;
        TextView subtitle;

        FctxViewHolder(View itemView) {
            super(itemView);
            indicator = (ImageView) itemView.findViewById(R.id.imageview_indicator);
            title = (TextView) itemView.findViewById(R.id.transaction_title);
            subtitle = (TextView) itemView.findViewById(R.id.transaction_subtitle);
        }
    }

    private static class HeaderViewHolder extends RecyclerView.ViewHolder {

        TextView header;

        HeaderViewHolder(View itemView) {
            super(itemView);
            header = (TextView) itemView.findViewById(R.id.header_name);
            itemView.findViewById(R.id.imageview_plus).setVisibility(View.GONE);
        }
    }
}
