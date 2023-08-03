package com.samourai.wallet.paynym.fragments;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.samourai.wallet.R;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.bip47.paynym.WebUtil;
import com.samourai.wallet.paynym.paynymDetails.PayNymDetailsActivity;
import com.samourai.wallet.widgets.CircleImageView;
import com.samourai.wallet.widgets.ItemDividerDecorator;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;


public class PaynymListFragment extends Fragment {

    private RecyclerView list;
    private static final String TAG = "PaynymListFragment";
    private PaynymAdapter paynymAdapter;

    public static PaynymListFragment newInstance() {
        return new PaynymListFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.paynym_account_list_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        list = view.findViewById(R.id.paynym_accounts_rv);
        Drawable drawable = this.getResources().getDrawable(R.drawable.divider_grey);
        list.addItemDecoration(new ItemDividerDecorator(drawable));
        list.setLayoutManager(new LinearLayoutManager(this.getContext()));
        list.setNestedScrollingEnabled(true);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        paynymAdapter = new PaynymAdapter();
        list.setAdapter(paynymAdapter);

    }

    public void addPcodes(ArrayList<String> list) {
        if(isAdded()){
            paynymAdapter.setPcodes(list);
        }
    }

    private ArrayList<String> filterArchived(ArrayList<String> list) {
        ArrayList<String> filtered = new ArrayList<>();

        for (String item : list) {
            if (!BIP47Meta.getInstance().getArchived(item)) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    public void onPayNymItemClick(final String pcode, final View avatar) {
        final ActivityOptionsCompat options = ActivityOptionsCompat.
                makeSceneTransitionAnimation(getActivity(), avatar, "profile");


        startActivity(new Intent(getActivity(), PayNymDetailsActivity.class).putExtra("pcode", pcode),options.toBundle());
    }


    class PaynymAdapter extends RecyclerView.Adapter<PaynymAdapter.ViewHolder> {

        private ArrayList<String> pcodes = new ArrayList<>();

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.paynym_list_item, parent, false));
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {

            final String strPaymentCode = pcodes.get(position);
            final CircleImageView avatar = holder.avatar;
            final View itemView = holder.itemView;
            final TextView paynymCode = holder.paynymCode;

            try {
                Picasso.get().load(WebUtil.PAYNYM_API + strPaymentCode + "/avatar")
                        .into(avatar, new Callback() {
                            @Override
                            public void onSuccess() {
                                paynymCode.setText(BIP47Meta.getInstance().getDisplayLabel(strPaymentCode));
                                itemView.setOnClickListener(view -> onPayNymItemClick(strPaymentCode, avatar));
                            }

                            @Override
                            public void onError(Exception e) {

                                Picasso.get().load(WebUtil.PAYNYM_API + "preview/" + strPaymentCode)
                                        .into(avatar, new Callback() {
                                            @Override
                                            public void onSuccess() {
                                                paynymCode.setText(BIP47Meta.getInstance().getDisplayLabel(strPaymentCode));
                                                itemView.setOnClickListener(view -> onPayNymItemClick(strPaymentCode, avatar));
                                            }

                                            @Override
                                            public void onError(final Exception e) {
                                                Log.e(TAG, "issue when loading avatar for " + strPaymentCode, e);
                                            }
                                        });
                            }
                        });
            } catch (final Throwable t) {
                /**
                 * This catch block is useful if ever the onSuccess/onError callback system
                 * throws a runtime exception.
                 * It indicates a problem to be fixed, so we log in error.
                 * This has already been the case through the method LogUtil#error.
                 */
                Log.e(TAG, "error with Picasso: " + t.getMessage(), t);
            }
        }

        @Override
        public int getItemCount() {
            return pcodes.size();
        }

        public void setPcodes(ArrayList<String> list) {
            pcodes.clear();
            pcodes.addAll(list);
            this.notifyDataSetChanged();
        }

        class ViewHolder extends RecyclerView.ViewHolder {

            CircleImageView avatar;
            TextView paynymCode;

            ViewHolder(View itemView) {
                super(itemView);
                avatar = itemView.findViewById(R.id.paynym_avatar);
                paynymCode = itemView.findViewById(R.id.paynym_code);
            }
        }
    }
}
