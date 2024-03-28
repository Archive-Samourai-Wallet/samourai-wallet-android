package com.samourai.wallet.util.func;

import com.samourai.wallet.SamouraiActivity;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.constants.SamouraiAccountIndex;
import com.samourai.wallet.send.BlockedUTXO;
import com.samourai.wallet.utxos.PreSelectUtil;
import com.samourai.wallet.utxos.models.UTXOCoin;

import java.util.List;

import static java.util.Objects.isNull;

public class BalanceUtil {

    private BalanceUtil() {}

    public static long getBalance(final int account, final SamouraiActivity activity) {

        if (isNull(activity)) return 0L;

        long balance = 0l;
        try {
            if (account == SamouraiAccountIndex.POSTMIX) {
                balance = APIFactory.getInstance(activity).getXpubPostMixBalance();
            } else {
                balance = APIFactory.getInstance(activity).getXpubBalance();
            }
        } catch (java.lang.NullPointerException npe) {
            npe.printStackTrace();
        }

        if (activity.getIntent().getExtras().containsKey("preselected")) {
            //Reloads preselected utxo's if it changed on last call
            final List<UTXOCoin> preselectedUTXOs = PreSelectUtil.getInstance()
                    .getPreSelected(activity.getIntent().getExtras().getString("preselected"));

            if (preselectedUTXOs != null && preselectedUTXOs.size() > 0) {

                //Checks utxo's state, if the item is blocked it will be removed from preselectedUTXOs
                for (int i = preselectedUTXOs.size()-1; i >= 0; --i) {
                    final UTXOCoin coin = preselectedUTXOs.get(i);
                    if (BlockedUTXO.getInstance().containsAny(coin.hash, coin.idx)) {
                        preselectedUTXOs.remove(i);
                    }
                }
                long amount = 0;
                for (final UTXOCoin utxo : preselectedUTXOs) {
                    amount += utxo.amount;
                }
                balance = amount;
            }

        }
        return balance;
    }
}
