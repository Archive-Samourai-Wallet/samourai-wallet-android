package com.samourai.wallet.send.review;

import static com.samourai.wallet.send.cahoots.JoinbotHelper.UTXO_COMPARATOR_BY_VALUE;
import static com.samourai.wallet.send.review.EnumSendType.SPEND_BOLTZMANN;
import static com.samourai.wallet.send.review.EnumSendType.SPEND_JOINBOT;
import static com.samourai.wallet.send.review.EnumSendType.SPEND_RICOCHET;
import static com.samourai.wallet.send.review.EnumSendType.SPEND_SIMPLE;
import static com.samourai.wallet.send.review.EnumSendType.valueOf;
import static java.lang.Math.max;
import static java.lang.Math.round;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.samourai.boltzmann.beans.BoltzmannSettings;
import com.samourai.boltzmann.beans.Txos;
import com.samourai.boltzmann.linker.TxosLinkerOptionEnum;
import com.samourai.boltzmann.processor.TxProcessor;
import com.samourai.boltzmann.processor.TxProcessorResult;
import com.samourai.wallet.R;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.hd.WALLET_INDEX;
import com.samourai.wallet.network.dojo.DojoUtil;
import com.samourai.wallet.ricochet.RicochetMeta;
import com.samourai.wallet.segwit.bech32.Bech32Util;
import com.samourai.wallet.send.BlockedUTXO;
import com.samourai.wallet.send.FeeUtil;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.SendActivity;
import com.samourai.wallet.send.SendFactory;
import com.samourai.wallet.send.SuggestedFee;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.send.UTXOFactory;
import com.samourai.wallet.send.cahoots.SelectCahootsType;
import com.samourai.wallet.util.FormatsUtilGeneric;
import com.samourai.wallet.util.PrefsUtil;
import com.samourai.wallet.util.func.AddressFactory;
import com.samourai.wallet.util.func.FormatsUtil;
import com.samourai.wallet.util.tech.AppUtil;
import com.samourai.wallet.util.tech.SimpleTaskRunner;
import com.samourai.wallet.utxos.PreSelectUtil;
import com.samourai.wallet.utxos.models.UTXOCoin;
import com.samourai.whirlpool.client.wallet.beans.SamouraiAccountIndex;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.ThreadUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.script.Script;
import org.bouncycastle.util.encoders.Hex;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedDeque;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class ReviewTxModel extends AndroidViewModel {

    public static class TxData {

        private int idxBIP44Internal = 0;
        private int idxBIP49Internal = 0;
        private int idxBIP84Internal = 0;
        private int idxBIP84PostMixInternal = 0;

        private final List<UTXO> selectedUTXO = Lists.newArrayList();
        private final Map<String, BigInteger> receivers = Maps.newHashMap();
        private long change = 0l;

        private TxData() {}

        public static TxData create(final Context context) {
            final TxData txData = new TxData();
            final AddressFactory addressFactory = AddressFactory.getInstance(context);
            txData.idxBIP84PostMixInternal = addressFactory.getIndex(WALLET_INDEX.POSTMIX_CHANGE);
            txData.idxBIP84Internal = addressFactory.getIndex(WALLET_INDEX.BIP84_CHANGE);
            txData.idxBIP49Internal = addressFactory.getIndex(WALLET_INDEX.BIP49_CHANGE);
            txData.idxBIP44Internal = addressFactory.getIndex(WALLET_INDEX.BIP44_CHANGE);
            return txData;
        }

        public void restoreChangeIndexes(final Context context) {
            AddressFactory.getInstance(context).setWalletIdx(
                    WALLET_INDEX.POSTMIX_CHANGE,
                    idxBIP84PostMixInternal,
                    true);
            AddressFactory.getInstance(context).setWalletIdx(
                    WALLET_INDEX.BIP84_CHANGE,
                    idxBIP84Internal,
                    true);
            AddressFactory.getInstance(context).setWalletIdx(
                    WALLET_INDEX.BIP49_CHANGE,
                    idxBIP49Internal,
                    true);
            AddressFactory.getInstance(context).setWalletIdx(
                    WALLET_INDEX.BIP44_CHANGE,
                    idxBIP44Internal,
                    true);
        }

        public List<UTXO> getSelectedUTXO() {
            return selectedUTXO;
        }

        public Map<String, BigInteger> getReceivers() {
            return receivers;
        }

        public long getChange() {
            return change;
        }

        public void setChange(long change) {
            this.change = change;
        }

        public int getIdxBIP44Internal() {
            return idxBIP44Internal;
        }

        public int getIdxBIP49Internal() {
            return idxBIP49Internal;
        }

        public int getIdxBIP84Internal() {
            return idxBIP84Internal;
        }

        public int getIdxBIP84PostMixInternal() {
            return idxBIP84PostMixInternal;
        }

        public void clear() {
            receivers.clear();
            selectedUTXO.clear();
        }
    }

    private static final String TAG = "BIP47Meta";
    public static final String MINER = "miner";

    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    private int account;
    private String address;
    private String addressLabel;
    private String preselectedUtxoId;

    private long amount = 0l;
    private List<UTXOCoin> preselectedUTXOs;
    private EnumSendType type = SPEND_SIMPLE;
    private boolean ricochetStaggeredDelivery;
    private Long _balance = null;
    private TxData txData = TxData.create(getApplication());
    private SelectCahootsType.type selectedCahootsType = SelectCahootsType.type.NONE;
    private MutableLiveData<EntropyInfo> entropy = null;

    private Pair<List<MyTransactionOutPoint>, List<TransactionOutput>> _pair = null;

    private MutableLiveData<Map<String, Long>> _fees = null;
    private MutableLiveData<Long> _feeAggregated = null;
    private MutableLiveData<Long> _feeLowRate;
    private MutableLiveData<Long> _feeMedRate;
    private MutableLiveData<Long> _feeHighRate;
    private MutableLiveData<Long> minerFeeRates = null;
    private MutableLiveData<EnumTransactionPriority> transactionPriority = new MutableLiveData<>(EnumTransactionPriority.NORMAL);
    private MutableLiveData<String> txNote = new MutableLiveData<>(StringUtils.EMPTY);

    private final ConcurrentLinkedDeque<Long> computeFeeValuesSynchronizer = new ConcurrentLinkedDeque<>();


    public ReviewTxModel(@NonNull Application application) {
        super(application);
        txData.restoreChangeIndexes(application);
    }

    @Override
    protected void onCleared() {
        if (! compositeDisposable.isDisposed()) {
            compositeDisposable.dispose();
        }
        super.onCleared();
    }

    public MutableLiveData<Long> getMinerFeeRates() {
        if (isNull(minerFeeRates)) {
            minerFeeRates = new MutableLiveData<>(getFeeMedRate().getValue());
        }
        return minerFeeRates;
    }

    public LiveData<EntropyInfo> getEntropy() {
        if (isNull(entropy)) {
            entropy = new MutableLiveData<>(EntropyInfo.create());
            try {
                buildBoltzmannTxData();
                final Disposable entropyDisposable = ReviewTxModel.calculateEntropy(txData.getSelectedUTXO(), txData.getReceivers())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeOn(Schedulers.computation())
                        .doOnSuccess(txProcessorResult -> {
                            entropy.postValue(EntropyInfo.create()
                                    .setEntropy(txProcessorResult.getEntropy())
                                    .setInterpretations(txProcessorResult.getNbCmbn()));
                        })
                        .doOnError(throwable -> {
                            entropy.postValue(EntropyInfo.create());
                            throwable.printStackTrace();
                        })
                        .subscribe();
                compositeDisposable.add(entropyDisposable);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, e.getMessage());
            }
        }
        return entropy;
    }

    public void setMinerFeeRates(final long value) {

        final SuggestedFee suggestedFee = new SuggestedFee();
        suggestedFee.setDefaultPerKB(BigInteger.valueOf(value * 1000l));
        FeeUtil.getInstance().setSuggestedFee(suggestedFee);

        if (isNull(minerFeeRates)) {
            minerFeeRates = new MutableLiveData<>(value);
        } else {
            minerFeeRates.postValue(value);
        }
        if (value >= getFeeHighRate().getValue()) {
            transactionPriority.postValue(EnumTransactionPriority.NEXT_BLOCK);
        } else if (value <= getFeeLowRate().getValue()) {
            transactionPriority.postValue(EnumTransactionPriority.LOW);
        } else {
            transactionPriority.postValue(EnumTransactionPriority.NORMAL);
        }

        _pair = null;
        if (computeFeeValuesSynchronizer.isEmpty()) {
            computeFeeValuesSynchronizer.addLast(value);
            computeFeeValuesAsync();
        } else {
            computeFeeValuesSynchronizer.addLast(value);
        }
    }

    public MutableLiveData<EnumTransactionPriority> getTransactionPriority() {
        return transactionPriority;
    }

    public LiveData<Long> getFeeLowRate() {
        if (isNull(_feeLowRate)) {
            computeFeeRate();
        }
        return _feeLowRate;
    }

    public LiveData<Long> getFeeMedRate() {
        if (isNull(_feeMedRate)) {
            computeFeeRate();
        }
        return _feeMedRate;
    }

    public LiveData<Long> getFeeHighRate() {
        if (isNull(_feeHighRate)) {
            computeFeeRate();
        }
        return _feeHighRate;
    }

    public List<UTXOCoin> getPreselectedUTXOs() {
        return preselectedUTXOs;
    }

    public ReviewTxModel setType(final EnumSendType type) {
        this.type = type;
        return this;
    }

    public ReviewTxModel buildRicochetTxData() {
        return this;
    }

    public ReviewTxModel buildSimleTxData() throws Exception {
        getSimpleFees();
        return this;
    }

    public ReviewTxModel buildJoinbotTxData() {

        txData.clear();
        txData.receivers.put(address, BigInteger.valueOf(amount));
        selectedCahootsType = SelectCahootsType.type.MULTI_SOROBAN;
        return this;
    }

    public ReviewTxModel buildBoltzmannTxData() throws Exception {

        txData.clear();
        txData.receivers.put(address, BigInteger.valueOf(amount));

        int countP2WSH_P2TR = 0;
        if(FormatsUtilGeneric.getInstance().isValidP2WSH_P2TR(address))    {
            countP2WSH_P2TR = 1;
        }

        final Pair<List<MyTransactionOutPoint>, List<TransactionOutput>> pair = computeStonewallPair();

        long totalValueSelected = 0L;
        for (final MyTransactionOutPoint outpoint : pair.getLeft()) {

            final UTXO u = new UTXO();
            u.setOutpoints(Lists.newArrayList(outpoint));
            txData.selectedUTXO.add(u);
            totalValueSelected += u.getValue();
        }
        for (final TransactionOutput output : pair.getRight()) {
            final Script script = new Script(output.getScriptBytes());
            if (Bech32Util.getInstance().isP2WPKHScript(Hex.toHexString(output.getScriptBytes())) ||
                    Bech32Util.getInstance().isP2TRScript(Hex.toHexString(output.getScriptBytes()))) {

                final String addressFromScript = Bech32Util.getInstance()
                        .getAddressFromScript(script);
                final BigInteger value = BigInteger.valueOf(output.getValue().longValue());
                txData.receivers.put(addressFromScript, value);
            } else {
                final String address = script
                        .getToAddress(SamouraiWallet.getInstance().getCurrentNetworkParams())
                        .toString();
                final BigInteger value = BigInteger.valueOf(output.getValue().longValue());
                txData.receivers.put(address, value);
            }
        }

        if (txData.selectedUTXO.size() > 0) {

            final List<MyTransactionOutPoint> outpoints = new ArrayList<>();
            for (final UTXO utxo : txData.selectedUTXO) {
                outpoints.addAll(utxo.getOutpoints());
            }

            final Triple<Integer, Integer, Integer> outpointTypes = FeeUtil.getInstance()
                    .getOutpointCount(new Vector(outpoints));

            if (amount == getBalance()) {

                final BigInteger fee = FeeUtil.getInstance().estimatedFeeSegwit(
                        outpointTypes.getLeft(),
                        outpointTypes.getMiddle(),
                        outpointTypes.getRight(),
                        1 - countP2WSH_P2TR,
                        countP2WSH_P2TR);
                Log.d("SendActivity", "fee:" + fee.longValue());
                txData.setChange(totalValueSelected - (amount + fee.longValue()));

            } else {

                final BigInteger fee = FeeUtil.getInstance().estimatedFeeSegwit(
                        outpointTypes.getLeft(),
                        outpointTypes.getMiddle(),
                        outpointTypes.getRight(),
                        2 - countP2WSH_P2TR,
                        countP2WSH_P2TR);

                txData.setChange(totalValueSelected - (amount + fee.longValue()));
            }
        }

        return this;
    }

    public SelectCahootsType.type getSelectedCahootsType() {
        return selectedCahootsType;
    }

    private Pair<List<MyTransactionOutPoint>, List<TransactionOutput>> computeStonewallPair() {
        if (isNull(_pair)) { // should be computed once because this bloc is not reentrant
            final Pair<List<UTXO>, List<UTXO>> utxo1AndUtxo2 = getUtxo1AndUtxo2();
            final List<UTXO> _utxos1 = utxo1AndUtxo2.getLeft();
            final List<UTXO> _utxos2 = utxo1AndUtxo2.getRight();

            // boltzmann spend (STONEWALL)
            _pair = SendFactory.getInstance(getApplication())
                    .boltzmann(_utxos1, _utxos2, BigInteger.valueOf(amount), address, account);
        }
        return _pair;
    }

    public EnumSendType getSendType() {
        switch (type) {
            case SPEND_SIMPLE:
            case SPEND_BOLTZMANN:
                return canDoBoltzmann() ? SPEND_BOLTZMANN : SPEND_SIMPLE;
            case SPEND_RICOCHET:
                return SPEND_RICOCHET;
            case SPEND_JOINBOT:
                return SPEND_JOINBOT;
            default:
                return SPEND_SIMPLE;
        }
    }

    public LiveData<Map<String, Long>> getFees() {
        if (isNull(_fees)) {
            computeFeeValues();
        }
        return _fees;
    }

    public MutableLiveData<String> getTxNote() {
        return txNote;
    }

    public ReviewTxModel setTxNote(final String note) {
        this.txNote.postValue(note);
        return this;
    }

    public TxData getTxData() {
        return txData;
    }

    public String getAddress() {
        return address;
    }

    public String getAddressLabel() {
        return addressLabel;
    }

    public long getAmount() {
        return amount;
    }

    private void computeFeeValuesAsync() {
        SimpleTaskRunner.create().executeAsync(() -> computeFeeValuesSync());
    }

    private void computeFeeValuesSync() {
        if (!computeFeeValuesSynchronizer.isEmpty()) {
            computeFeeValuesSynchronizer.clear();
            computeFeeValues();
            computeFeeValuesAsync();
        }
    }

    private void computeFeeValues() {
        if (isNull(_fees)) {
            _fees = new MutableLiveData<>(computeFees());
        } else {
            _fees.postValue(computeFees());
        }
        if (isNull(_feeAggregated)) {
            _feeAggregated = new MutableLiveData<>(computeFeeAggregated());
        } else {
            _feeAggregated.postValue(computeFeeAggregated());
        }
    }

    private Map<String, Long> computeFees() {
        switch (getSendType()) {
            case SPEND_BOLTZMANN:
                return getBoltzmannFees();
            case SPEND_RICOCHET:
                return getRicochetFees();
            case SPEND_JOINBOT:
                final Map<String, Long> simpleFees = getSimpleFees();
                return Maps.newLinkedHashMap(ImmutableMap.<String, Long>builder()
                        .putAll(simpleFees)
                        .put("Joinbot fee", computeJoinbotFee())
                        .build());
            case SPEND_SIMPLE:
            default:
                return getSimpleFees();
        }
    }

    private long computeJoinbotFee() {
        // 3.5 % fee for Samourai service
        return max(1l, round(amount * 35d / 1000d));
    }

    private Map<String, Long> getBoltzmannFees() {

        final Pair<List<MyTransactionOutPoint>, List<TransactionOutput>> pair = computeStonewallPair();

        long inputAmount = 0L;
        for (final MyTransactionOutPoint outpoint : pair.getLeft()) {
            inputAmount += outpoint.getValue().value;
        }

        long outputAmount = 0L;
        for (final TransactionOutput output : pair.getRight()) {
            outputAmount += output.getValue().longValue();
        }

        return Maps.newLinkedHashMap(ImmutableMap.of(MINER, inputAmount - outputAmount));
    }

    public LiveData<Long> getFeeAggregated() {
        if (isNull(_feeAggregated)) {
            computeFees();
        }
        return _feeAggregated;
    }

    public long getMinerFee() {
        return computeFees().get(MINER);
    }

    public long computeFeeAggregated() {
        final Map<String, Long> fees = computeFees();
        long sum = 0l;
        for (final Long fee : fees.values()) {
            sum += fee;
        }
        return sum;
    }

    public boolean canDoBoltzmann() {

        if (CollectionUtils.isNotEmpty(preselectedUTXOs)) {
            return false;
        }
        if (amount == getBalance()) {
            return false;
        }

        final Pair<List<UTXO>, List<UTXO>> utxo1AndUtxo2 = getUtxo1AndUtxo2();
        final List<UTXO> _utxos1 = utxo1AndUtxo2.getLeft();
        final List<UTXO> _utxos2 = utxo1AndUtxo2.getRight();

        if (CollectionUtils.isEmpty(_utxos1) && CollectionUtils.isEmpty(_utxos2)) {
            return false;
        } else {

            Log.d("SendActivity", "boltzmann spend");

            final Pair<List<MyTransactionOutPoint>, List<TransactionOutput>> pair = computeStonewallPair();

            if (isNull(pair)) {
                txData.restoreChangeIndexes(getApplication());
                return false;
            } else {
                return true;
            }
        }
    }

    public int getChangeType() {

        boolean useLikeType = PrefsUtil.getInstance(getApplication())
                .getValue(PrefsUtil.USE_LIKE_TYPED_CHANGE, true);
        if (DojoUtil.getInstance(getApplication()).getDojoParams() != null &&
                !DojoUtil.getInstance(getApplication()).isLikeType()) {
            useLikeType = false;
        }

        if (!useLikeType) {
            return 84;
        } else if (FormatsUtil.getInstance().isValidBech32(address) ||
                Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), address).isP2SHAddress()) {
            return FormatsUtil.getInstance().isValidBech32(address) ? 84 : 49;
        } else {
            return 44;
        }
    }

    private List<UTXO> getAllUtxo() {
        if (CollectionUtils.isNotEmpty(preselectedUTXOs)) {
            final List<UTXO> utxos = new ArrayList<>();
            // sort in descending order by value
            for (final UTXOCoin utxoCoin : preselectedUTXOs) {
                final UTXO u = new UTXO();
                final List<MyTransactionOutPoint> outs = new ArrayList<>();
                outs.add(utxoCoin.getOutPoint());
                u.setOutpoints(outs);
                utxos.add(u);
            }
            return utxos;
        } else {
            if (isPostmixAccount()) {
                return new ArrayList<>(APIFactory.getInstance(getApplication()).getUtxosPostMix(true));
            } else {
                if (nonNull(address)) {
                    return UTXOFactory.getInstance().getUTXOS(address, getNeededAmount(), account);
                }
                return Lists.newArrayList();
            }
        }
    }

    private List<UTXO> getUtxosP2PKH() {
        if (CollectionUtils.isEmpty(preselectedUTXOs) && isPostmixAccount()) {
            return Lists.newArrayList();
        } else {
            return new ArrayList<>(APIFactory.getInstance(getApplication()).getUtxosP2PKH(true));
        }
    }

    private List<UTXO> getUtxosP2SH_P2WPKH() {
        if (CollectionUtils.isEmpty(preselectedUTXOs) && isPostmixAccount()) {
            return Lists.newArrayList();
        } else {
            return new ArrayList<>(APIFactory.getInstance(getApplication()).getUtxosP2SH_P2WPKH(true));
        }
    }

    private List<UTXO> getUtxosP2WPKH() {
        if (CollectionUtils.isEmpty(preselectedUTXOs) && isPostmixAccount()) {
            return new ArrayList<>(APIFactory.getInstance(getApplication()).getUtxosPostMix(true));
        } else {
            return new ArrayList<>(APIFactory.getInstance(getApplication()).getUtxosP2WPKH(true));
        }
    }

    private Map<String, Long> getSimpleFees() {

        txData.clear();
        txData.receivers.put(address, BigInteger.valueOf(amount));

        int countP2WSH_P2TR = 0;
        if(FormatsUtilGeneric.getInstance().isValidP2WSH_P2TR(address))    {
            countP2WSH_P2TR = 1;
        }

        final List<UTXO> utxos = getAllUtxo();
        Collections.sort(utxos, UTXO_COMPARATOR_BY_VALUE);
        Collections.reverse(utxos);

        long totalValueSelected = 0l;

        if (amount == getBalance()) {

            txData.selectedUTXO.addAll(utxos);
            for (final UTXO u : txData.selectedUTXO) {
                totalValueSelected += u.getValue();
            }

        } else {

            for (final UTXO utxo : utxos) {
                final Triple<Integer, Integer, Integer> outpointTypes
                        = FeeUtil.getInstance().getOutpointCount(new Vector(utxo.getOutpoints()));
                if (utxo.getValue() >= (amount +
                        SamouraiWallet.bDust.longValue() +
                        FeeUtil.getInstance().estimatedFeeSegwit(
                                outpointTypes.getLeft(),
                                outpointTypes.getMiddle(),
                                outpointTypes.getRight(),
                                2 - countP2WSH_P2TR,
                                countP2WSH_P2TR).longValue())) {
                    txData.selectedUTXO.add(utxo);
                    totalValueSelected += utxo.getValue();
                    Log.d("SendActivity", "spend type:" + getSendType());
                    Log.d("SendActivity", "single output");
                    Log.d("SendActivity", "amount:" + amount);
                    Log.d("SendActivity", "value selected:" + utxo.getValue());
                    Log.d("SendActivity", "total value selected:" + totalValueSelected);
                    Log.d("SendActivity", "nb inputs:" + utxo.getOutpoints().size());
                    break;
                }
            }

            if (txData.selectedUTXO.size() == 0) {
                // sort in descending order by value
                Collections.sort(utxos, new UTXO.UTXOComparator());
                int selected = 0;
                int p2pkh = 0;
                int p2sh_p2wpkh = 0;
                int p2wpkh = 0;

                // get largest UTXOs > than spend + fee + dust
                for (final UTXO u : utxos) {

                    txData.selectedUTXO.add(u);
                    totalValueSelected += u.getValue();
                    selected += u.getOutpoints().size();

//                            Log.d("SendActivity", "value selected:" + u.getValue());
//                            Log.d("SendActivity", "total value selected/threshold:" + totalValueSelected + "/" + (amount + SamouraiWallet.bDust.longValue() + FeeUtil.getInstance().estimatedFee(selected, 2).longValue()));

                    final Triple<Integer, Integer, Integer> outpointTypes = FeeUtil.getInstance().getOutpointCount(new Vector<>(u.getOutpoints()));
                    p2pkh += outpointTypes.getLeft();
                    p2sh_p2wpkh += outpointTypes.getMiddle();
                    p2wpkh += outpointTypes.getRight();
                    if (totalValueSelected >= (amount + SamouraiWallet.bDust.longValue() + FeeUtil.getInstance().estimatedFeeSegwit(p2pkh, p2sh_p2wpkh, p2wpkh, 2 - countP2WSH_P2TR, countP2WSH_P2TR).longValue())) {
                        Log.d("SendActivity", "spend type:" + getSendType());
                        Log.d("SendActivity", "multiple outputs");
                        Log.d("SendActivity", "amount:" + amount);
                        Log.d("SendActivity", "total value selected:" + totalValueSelected);
                        Log.d("SendActivity", "nb inputs:" + selected);
                        break;
                    }
                }
            }
        }

        if (txData.selectedUTXO.size() > 0) {

            final List<MyTransactionOutPoint> outpoints = new ArrayList<>();
            for (final UTXO utxo : txData.selectedUTXO) {
                outpoints.addAll(utxo.getOutpoints());
            }

            final Triple<Integer, Integer, Integer> outpointTypes = FeeUtil.getInstance()
                    .getOutpointCount(new Vector(outpoints));

            if (amount == getBalance()) {

                final BigInteger fee = FeeUtil.getInstance().estimatedFeeSegwit(
                        outpointTypes.getLeft(),
                        outpointTypes.getMiddle(),
                        outpointTypes.getRight(),
                        1 - countP2WSH_P2TR,
                        countP2WSH_P2TR);
                Log.d("SendActivity", "fee:" + fee.longValue());

                txData.setChange(0);
                return Maps.newLinkedHashMap(ImmutableMap.of(MINER, fee.longValue()));

            } else {

                final BigInteger fee = FeeUtil.getInstance().estimatedFeeSegwit(
                        outpointTypes.getLeft(),
                        outpointTypes.getMiddle(),
                        outpointTypes.getRight(),
                        2 - countP2WSH_P2TR,
                        countP2WSH_P2TR);

                txData.setChange(totalValueSelected - (amount + fee.longValue()));
                return Maps.newLinkedHashMap(ImmutableMap.of(MINER, fee.longValue()));
            }
        }

        return Maps.newLinkedHashMap(ImmutableMap.of(MINER, 0l));
    }

    public JSONObject getRicochetJsonObj() {

        boolean samouraiFeeViaBIP47 = false;
        if (BIP47Meta.getInstance().getOutgoingStatus(BIP47Meta.strSamouraiDonationPCode) == BIP47Meta.STATUS_SENT_CFM) {
            samouraiFeeViaBIP47 = true;
        }

        final String strPCode = BIP47Meta.getInstance().getPcodeFromLabel(addressLabel);

        final JSONObject ricochetJson = RicochetMeta.getInstance(getApplication())
                .script(
                        amount,
                        FeeUtil.getInstance().getSuggestedFee().getDefaultPerKB().longValue(),
                        address,
                        RicochetMeta.defaultNbHops,
                        strPCode,
                        samouraiFeeViaBIP47,
                        isEnabledRicochetStaggered(),
                        account);
        return ricochetJson;
    }

    public String getRicochetMessage() {
        final JSONObject ricochetJsonObj = getRicochetJsonObj();
        if (nonNull(ricochetJsonObj)) {
            try {
                final long totalAmount = ricochetJsonObj.getLong("total_spend");
                return getApplication().getText(R.string.ricochet_spend1) + " " +
                        address + " " + getApplication().getText(R.string.ricochet_spend2) + " " +
                        FormatsUtil.formatBTC(totalAmount) + " " +
                        getApplication().getText(R.string.ricochet_spend3);
            } catch (final Exception e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
        return null;
    }

    private Map<String, Long> getRicochetFees() {

        final JSONObject ricochetJsonObj = getRicochetJsonObj();

        if (nonNull(ricochetJsonObj)) {
            try {

                final long hop0Fee = ricochetJsonObj.getJSONArray("hops")
                        .getJSONObject(0).getLong("fee");
                final long perHopFee = ricochetJsonObj.getJSONArray("hops")
                        .getJSONObject(0).getLong("fee_per_hop");
                final long ricochetFee = ricochetJsonObj.getLong("samourai_fee");

                return Maps.newLinkedHashMap(ImmutableMap.of(
                        MINER, hop0Fee + RicochetMeta.defaultNbHops * perHopFee,
                        "Ricochet Fee", ricochetFee
                ));

            } catch (final JSONException je) {
                Log.e(TAG, "JSONException on Json");
            }
        }

        return Maps.newLinkedHashMap();
    }

    public String getPreselectedUtxoId() {
        return preselectedUtxoId;
    }

    public boolean hasPreselectedUtxo() {
        return nonNull(preselectedUtxoId);
    }

    public boolean isPostmixAccount() {
        return account == SamouraiAccountIndex.POSTMIX;
    }

    private long getNeededAmount() {

        final int countP2WSH_P2TR = getCountP2WSH_P2TR();

        long neededAmount = 0L;

        if (FormatsUtil.getInstance().isValidBech32(address) || isPostmixAccount()) {
            neededAmount += FeeUtil.getInstance().estimatedFeeSegwit(0, 0, UTXOFactory.getInstance().getCountP2WPKH(), 4 - countP2WSH_P2TR, countP2WSH_P2TR).longValue();
        } else if (nonNull(address) && Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), address).isP2SHAddress()) {
            neededAmount += FeeUtil.getInstance().estimatedFeeSegwit(0, UTXOFactory.getInstance().getCountP2SH_P2WPKH(), 0, 4 - countP2WSH_P2TR, countP2WSH_P2TR).longValue();
        } else {
            neededAmount += FeeUtil.getInstance().estimatedFeeSegwit(UTXOFactory.getInstance().getCountP2PKH(), 0, 0, 4 - countP2WSH_P2TR, countP2WSH_P2TR).longValue();
        }

        neededAmount += amount;
        neededAmount += SamouraiWallet.bDust.longValue();

        return neededAmount;
    }

    private int getCountP2WSH_P2TR() {
        int countP2WSH_P2TR = 0;
        if(FormatsUtilGeneric.getInstance().isValidP2WSH_P2TR(address))    {
            countP2WSH_P2TR = 1;
        }
        return countP2WSH_P2TR;
    }

    public ReviewTxModel setRicochetStaggeredDelivery(final boolean ricochetStaggeredDelivery) {
        this.ricochetStaggeredDelivery = ricochetStaggeredDelivery;
        return this;
    }

    public ReviewTxModel setAddress(final String address) {
        this.address = address;
        return this;
    }

    public ReviewTxModel setAddressLabel(final String addressLabel) {
        this.addressLabel = addressLabel;
        return this;
    }

    public ReviewTxModel setPreselectedUtxo(final @Nullable String preselectedUtxoId) {
        this.preselectedUtxoId = preselectedUtxoId;
        if (nonNull(preselectedUtxoId)) {
            preselectedUTXOs = PreSelectUtil.getInstance().getPreSelected(preselectedUtxoId);
        }
        return this;
    }

    public int getAccount() {
        return account;
    }

    public ReviewTxModel setAccount(final int account) {
        this.account = account;
        return this;
    }

    public ReviewTxModel setAmount(final long amount) {
        this.amount = amount;
        return this;
    }

    private Pair<List<UTXO>, List<UTXO>> getUtxo1AndUtxo2() {

        final List<UTXO> utxosP2WPKH = getUtxosP2WPKH();
        final List<UTXO> utxosP2SH_P2WPKH = getUtxosP2SH_P2WPKH();
        final List<UTXO> utxosP2PKH = getUtxosP2PKH();

        long valueP2WPKH = UTXOFactory.getInstance().getTotalP2WPKH();
        long valueP2SH_P2WPKH = UTXOFactory.getInstance().getTotalP2SH_P2WPKH();
        long valueP2PKH = UTXOFactory.getInstance().getTotalP2PKH();
        if (isPostmixAccount()) {

            valueP2WPKH = UTXOFactory.getInstance().getTotalPostMix();
            valueP2SH_P2WPKH = 0L;
            valueP2PKH = 0L;

            utxosP2SH_P2WPKH.clear();
            utxosP2PKH.clear();
        }

        Log.d("SendActivity", "value P2WPKH:" + valueP2WPKH);
        Log.d("SendActivity", "value P2SH_P2WPKH:" + valueP2SH_P2WPKH);
        Log.d("SendActivity", "value P2PKH:" + valueP2PKH);


        List<UTXO> _utxos1 = null;
        List<UTXO> _utxos2 = null;

        boolean selectedP2WPKH = false;
        boolean selectedP2SH_P2WPKH = false;
        boolean selectedP2PKH = false;

        final long neededAmount = getNeededAmount();

        if ((valueP2WPKH > (neededAmount * 2)) && isPostmixAccount()) {
            Log.d("SendActivity", "set 1 P2WPKH 2x");
            _utxos1 = utxosP2WPKH;
            selectedP2WPKH = true;
        } else if ((valueP2WPKH > (neededAmount * 2)) && FormatsUtil.getInstance().isValidBech32(address)) {
            Log.d("SendActivity", "set 1 P2WPKH 2x");
            _utxos1 = utxosP2WPKH;
            selectedP2WPKH = true;
        } else if (!FormatsUtil.getInstance().isValidBech32(address) && (valueP2SH_P2WPKH > (neededAmount * 2)) && Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), address).isP2SHAddress()) {
            Log.d("SendActivity", "set 1 P2SH_P2WPKH 2x");
            _utxos1 = utxosP2SH_P2WPKH;
            selectedP2SH_P2WPKH = true;
        } else if (!FormatsUtil.getInstance().isValidBech32(address) && (valueP2PKH > (neededAmount * 2)) && !Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), address).isP2SHAddress()) {
            Log.d("SendActivity", "set 1 P2PKH 2x");
            _utxos1 = utxosP2PKH;
            selectedP2PKH = true;
        } else if (valueP2WPKH > (neededAmount * 2)) {
            Log.d("SendActivity", "set 1 P2WPKH 2x");
            _utxos1 = utxosP2WPKH;
            selectedP2WPKH = true;
        } else if (valueP2SH_P2WPKH > (neededAmount * 2)) {
            Log.d("SendActivity", "set 1 P2SH_P2WPKH 2x");
            _utxos1 = utxosP2SH_P2WPKH;
            selectedP2SH_P2WPKH = true;
        } else if (valueP2PKH > (neededAmount * 2)) {
            Log.d("SendActivity", "set 1 P2PKH 2x");
            _utxos1 = utxosP2PKH;
            selectedP2PKH = true;
        } else {
            ;
        }

        if (_utxos1 == null || _utxos1.size() == 0) {
            if (valueP2SH_P2WPKH > neededAmount) {
                Log.d("SendActivity", "set 1 P2SH_P2WPKH");
                _utxos1 = utxosP2SH_P2WPKH;
                selectedP2SH_P2WPKH = true;
            } else if (valueP2WPKH > neededAmount) {
                Log.d("SendActivity", "set 1 P2WPKH");
                _utxos1 = utxosP2WPKH;
                selectedP2WPKH = true;
            } else if (valueP2PKH > neededAmount) {
                Log.d("SendActivity", "set 1 P2PKH");
                _utxos1 = utxosP2PKH;
                selectedP2PKH = true;
            } else {
                ;
            }

        }

        if (_utxos1 != null && _utxos1.size() > 0) {
            if (!selectedP2SH_P2WPKH && valueP2SH_P2WPKH > neededAmount) {
                Log.d("SendActivity", "set 2 P2SH_P2WPKH");
                _utxos2 = utxosP2SH_P2WPKH;
                selectedP2SH_P2WPKH = true;
            }
            if (!selectedP2SH_P2WPKH && !selectedP2WPKH && valueP2WPKH > neededAmount) {
                Log.d("SendActivity", "set 2 P2WPKH");
                _utxos2 = utxosP2WPKH;
                selectedP2WPKH = true;
            }
            if (!selectedP2SH_P2WPKH && !selectedP2WPKH && !selectedP2PKH && valueP2PKH > neededAmount) {
                Log.d("SendActivity", "set 2 P2PKH");
                _utxos2 = utxosP2PKH;
            } else {
                ;
            }
        }
        Collections.shuffle(_utxos1);
        if (CollectionUtils.isNotEmpty(_utxos2)) {
            Collections.shuffle(_utxos2);
        }
        return Pair.of(_utxos1, _utxos2);
    }

    public long getBalance() {

        if (nonNull(_balance)) return _balance;

        try {
            if (isPostmixAccount()) {
                _balance = APIFactory.getInstance(getApplication()).getXpubPostMixBalance();
            } else {
                _balance = APIFactory.getInstance(getApplication()).getXpubBalance();
            }
        } catch (java.lang.NullPointerException npe) {
            npe.printStackTrace();
        }

        if (hasPreselectedUtxo()) {
            //Reloads preselected utxo's if it changed on last call
            if (CollectionUtils.isNotEmpty(preselectedUTXOs)) {

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
                _balance = amount;
            } else {
                ;
            }

        }

        if (isNull(_balance)) {
            _balance = 0L;
        }

        return _balance;
    }

    public static Single<TxProcessorResult> calculateEntropy(
            final List<UTXO> selectedUTXO,
            final Map<String, BigInteger> receivers) {

        return Single.create(emitter -> {

            final Map<String, Long> inputs = new HashMap<>();
            final Map<String, Long> outputs = new HashMap<>();

            for (final Map.Entry<String, BigInteger> mapEntry : receivers.entrySet()) {
                String toAddress = mapEntry.getKey();
                BigInteger value = mapEntry.getValue();
                outputs.put(toAddress, value.longValue());
            }

            for (int i = 0; i < selectedUTXO.size(); i++) {
                inputs.put(SendActivity.stubAddress[i], selectedUTXO.get(i).getValue());
            }

            final TxProcessor txProcessor = new TxProcessor(
                    BoltzmannSettings.MAX_DURATION_DEFAULT,
                    BoltzmannSettings.MAX_TXOS_DEFAULT);
            final Txos txos = new Txos(inputs, outputs);
            final TxProcessorResult result = txProcessor.processTx(
                    txos,
                    0.005f,
                    TxosLinkerOptionEnum.PRECHECK,
                    TxosLinkerOptionEnum.LINKABILITY,
                    TxosLinkerOptionEnum.MERGE_INPUTS);

            emitter.onSuccess(result);
        });
    }

    public boolean isEnabledRicochetStaggered() {
        return !AppUtil.getInstance(getApplication()).isBroadcastDisabled() &&
                ricochetStaggeredDelivery;
    }

    private void computeFeeRate() {

        long feeLow = FeeUtil.getInstance().getLowFee().getDefaultPerKB().longValue();
        long feeMed = FeeUtil.getInstance().getNormalFee().getDefaultPerKB().longValue();
        long feeHigh = FeeUtil.getInstance().getHighFee().getDefaultPerKB().longValue();

        if (feeLow == feeMed && feeMed == feeHigh) {
            // offset of low and high
            feeLow = feeMed * 85l / 100l;
            feeHigh = feeMed * 115l / 100l;
            final SuggestedFee lo_sf = new SuggestedFee();
            lo_sf.setDefaultPerKB(BigInteger.valueOf(feeLow / 1000l * 1000l));
            FeeUtil.getInstance().setLowFee(lo_sf);
            final SuggestedFee hi_sf = new SuggestedFee();
            hi_sf.setDefaultPerKB(BigInteger.valueOf(feeHigh / 1000l * 1000l));
            FeeUtil.getInstance().setHighFee(hi_sf);
        } else if (feeLow == feeMed || feeMed == feeHigh) {
            // offset of mid
            feeMed = (feeLow + feeHigh) / 2l;
            final SuggestedFee mi_sf = new SuggestedFee();
            mi_sf.setDefaultPerKB(BigInteger.valueOf(feeMed / 1000l * 1000l));
            FeeUtil.getInstance().setNormalFee(mi_sf);
        }

        if (feeLow < 1000l) {
            feeLow = 1000l;
            final SuggestedFee lo_sf = new SuggestedFee();
            lo_sf.setDefaultPerKB(BigInteger.valueOf(feeLow));
            FeeUtil.getInstance().setLowFee(lo_sf);
        }
        if (feeMed < 1000l) {
            feeMed = 1000l;
            final SuggestedFee mi_sf = new SuggestedFee();
            mi_sf.setDefaultPerKB(BigInteger.valueOf(feeMed));
            FeeUtil.getInstance().setNormalFee(mi_sf);
        }
        if (feeHigh < 1000l) {
            feeHigh = 1000l;
            final SuggestedFee hi_sf = new SuggestedFee();
            hi_sf.setDefaultPerKB(BigInteger.valueOf(feeHigh));
            FeeUtil.getInstance().setHighFee(hi_sf);
        }

        if (isNull(_feeMedRate)) {
            _feeMedRate = new MutableLiveData<>(feeMed / 1000l);
            final SuggestedFee suggestedFee = new SuggestedFee();
            suggestedFee.setDefaultPerKB(BigInteger.valueOf(_feeMedRate.getValue() * 1000l));
            FeeUtil.getInstance().setSuggestedFee(suggestedFee);
        } else {
            _feeMedRate.setValue(feeMed / 1000l);
        }

        if (isNull(_feeHighRate)) {
            _feeHighRate = new MutableLiveData<>(feeHigh / 1000l);
        } else {
            _feeHighRate.setValue(feeHigh / 1000l);
        }

        if (isNull(_feeLowRate)) {
            _feeLowRate = new MutableLiveData<>(feeLow / 1000l);
        } else {
            _feeLowRate.setValue(feeLow / 1000l);
        }
    }
}
