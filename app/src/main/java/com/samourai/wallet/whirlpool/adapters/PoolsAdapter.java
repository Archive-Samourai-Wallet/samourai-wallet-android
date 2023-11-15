package com.samourai.wallet.whirlpool.adapters;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.Group;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.samourai.wallet.R;
import com.samourai.wallet.whirlpool.models.PoolViewModel;
import com.samourai.whirlpool.client.tx0.Tx0Preview;

import java.util.ArrayList;

import static com.samourai.wallet.util.func.FormatsUtil.getBTCDecimalFormat;
import static com.samourai.wallet.util.func.FormatsUtil.getPoolBTCDecimalFormat;

public class PoolsAdapter extends RecyclerView.Adapter<PoolsAdapter.ViewHolder> {

    private Context mContext;
    private OnItemsSelected onItemsSelected;
    private static final String TAG = "CoinsAdapter";
    private final AsyncListDiffer<PoolViewModel> mDiffer = new AsyncListDiffer<>(this, DIFF_CALLBACK);

    public PoolsAdapter(Context context, ArrayList<PoolViewModel> pools) {
        mContext = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pool, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final PoolViewModel poolViewModel = mDiffer.getCurrentList().get(position);
        if(poolViewModel.getTx0Preview() != null){
            Tx0Preview preview = poolViewModel.getTx0Preview();
            long embeddedTotalFees = preview.getMixMinerFee();
            long totalFees = embeddedTotalFees + preview.getFeeValue() + preview.getTx0MinerFee();
            holder.poolAmount.setText(getPoolBTCDecimalFormat(preview.getPool().getDenomination()).concat(" BTC Pool"));
            holder.poolFees.setText(mContext.getString(R.string.pool_fee).concat(" ").concat(getBTCDecimalFormat(preview.getFeeValue())).concat(" BTC"));
            holder.totalFees.setText(mContext.getString(R.string.total_fees).concat("  ").concat(getBTCDecimalFormat(totalFees)).concat(" BTC"));
            holder.minerFee.setText(mContext.getString(R.string.miner_fee).concat("  ").concat(getBTCDecimalFormat(preview.getTx0MinerFee()).concat(" BTC")));
        }else{
            holder.poolAmount.setText(getPoolBTCDecimalFormat(poolViewModel.getDenomination()).concat(" BTC Pool"));
            holder.poolFees.setText(mContext.getString(R.string.pool_fee).concat(" ").concat(getBTCDecimalFormat(poolViewModel.getFeeValue())).concat(" BTC"));
            holder.totalFees.setText(mContext.getString(R.string.total_fees).concat("  ").concat(getBTCDecimalFormat(poolViewModel.getTotalFee())).concat(" BTC").concat(" (").concat(String.valueOf(poolViewModel.getTotalEstimatedBytes())).concat( " bytes)"));
            holder.minerFee.setText(mContext.getString(R.string.miner_fee).concat("  ").concat(getBTCDecimalFormat(poolViewModel.getMinerFee() * poolViewModel.getTotalEstimatedBytes())).concat(" BTC"));
        }
         if (poolViewModel.isSelected()) {
            holder.feesGroup.setVisibility(View.VISIBLE);
        }

        holder.checkBox.setEnabled(poolViewModel.getTx0Preview() != null && poolViewModel.getTx0Preview().getNbPremix() != 0);
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(poolViewModel.isSelected() && poolViewModel.getTx0Preview().getNbPremix() != 0);
        holder.checkBox.setOnCheckedChangeListener((compoundButton, b) -> {
            onItemsSelected.onItemsSelected(position);
        });

        if (poolViewModel.getTx0Preview() != null && poolViewModel.getTx0Preview().getNbPremix() != 0) {
            holder.layout.setOnClickListener(view -> onItemsSelected.onItemsSelected(position));
            holder.layout.setAlpha(1f);
            holder.layout.setClickable(true);
        }else{
            holder.layout.setAlpha(0.4f);
            holder.layout.setClickable(false);
        }
    }


    @Override
    public int getItemCount() {
        return mDiffer.getCurrentList().size();
    }

    public void setOnItemsSelectListener(OnItemsSelected onItemsSelected) {
        this.onItemsSelected = onItemsSelected;
    }

    public void update(ArrayList<PoolViewModel> poolViewModels) {
        mDiffer.submitList(poolViewModels);
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private TextView poolAmount, poolFees, minerFee, totalFees;
        private MaterialCheckBox checkBox;
        private View layout;
        private Group feesGroup;

        ViewHolder(View itemView) {
            super(itemView);
            poolAmount = itemView.findViewById(R.id.pool_item_amount);
            poolFees = itemView.findViewById(R.id.pool_item_fee);
            minerFee = itemView.findViewById(R.id.pool_item_miner_fee);
            totalFees = itemView.findViewById(R.id.pool_item_total_fee);
            checkBox = itemView.findViewById(R.id.pool_item_checkbox);
            feesGroup = itemView.findViewById(R.id.item_pool_fees_group);
            layout = itemView.findViewById(R.id.item_coin_layout);
        }
    }


    public interface OnItemsSelected {
        void onItemsSelected(int position);
    }


    public static final DiffUtil.ItemCallback<PoolViewModel> DIFF_CALLBACK
            = new DiffUtil.ItemCallback<PoolViewModel>() {
        @Override
        public boolean areItemsTheSame(@NonNull PoolViewModel oldItem, @NonNull PoolViewModel newItem) {
            return oldItem.getPoolId().equals(newItem.getPoolId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull PoolViewModel oldItem, @NonNull PoolViewModel newItem) {
            return false;
        }

    };

}
