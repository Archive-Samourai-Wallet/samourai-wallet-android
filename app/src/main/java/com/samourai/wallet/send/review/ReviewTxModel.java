package com.samourai.wallet.send.review;

import static com.samourai.wallet.send.cahoots.JoinbotHelper.UTXO_COMPARATOR_BY_VALUE;
import static com.samourai.wallet.send.review.EnumSendType.SPEND_BATCH;
import static com.samourai.wallet.send.review.EnumSendType.SPEND_BOLTZMANN;
import static com.samourai.wallet.send.review.EnumSendType.SPEND_JOINBOT;
import static com.samourai.wallet.send.review.EnumSendType.SPEND_RICOCHET;
import static com.samourai.wallet.send.review.EnumSendType.SPEND_SIMPLE;
import static com.samourai.wallet.send.review.EnumSendType.valueOf;
import static com.samourai.wallet.util.tech.ThreadHelper.pauseMillis;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static java.lang.Math.max;
import static java.lang.Math.round;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import android.app.Application;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.work.Operation;

import com.google.android.material.snackbar.Snackbar;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListenableFuture;
import com.samourai.boltzmann.beans.BoltzmannSettings;
import com.samourai.boltzmann.beans.Txos;
import com.samourai.boltzmann.linker.TxosLinkerOptionEnum;
import com.samourai.boltzmann.processor.TxProcessor;
import com.samourai.boltzmann.processor.TxProcessorResult;
import com.samourai.wallet.R;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.api.fee.EnumFeeRepresentation;
import com.samourai.wallet.bip47.BIP47Meta;
import com.samourai.wallet.network.dojo.DojoUtil;
import com.samourai.wallet.ricochet.RicochetMeta;
import com.samourai.wallet.segwit.BIP84Util;
import com.samourai.wallet.segwit.bech32.Bech32Util;
import com.samourai.wallet.send.BlockedUTXO;
import com.samourai.wallet.send.FeeUtil;
import com.samourai.wallet.send.MyTransactionOutPoint;
import com.samourai.wallet.send.SendActivity;
import com.samourai.wallet.send.SendFactory;
import com.samourai.wallet.send.SuggestedFee;
import com.samourai.wallet.send.UTXO;
import com.samourai.wallet.send.UTXOFactory;
import com.samourai.wallet.send.batch.InputBatchSpendHelper;
import com.samourai.wallet.send.cahoots.SelectCahootsType;
import com.samourai.wallet.service.WalletRefreshWorker;
import com.samourai.wallet.util.FormatsUtilGeneric;
import com.samourai.wallet.util.PrefsUtil;
import com.samourai.wallet.util.func.AddressFactory;
import com.samourai.wallet.util.func.BatchSendUtil;
import com.samourai.wallet.util.func.BatchSendUtil.BatchSend;
import com.samourai.wallet.util.func.FormatsUtil;
import com.samourai.wallet.util.tech.AppUtil;
import com.samourai.wallet.util.tech.SimpleCallback;
import com.samourai.wallet.util.tech.SimpleTaskRunner;
import com.samourai.wallet.utxos.PreSelectUtil;
import com.samourai.wallet.utxos.models.UTXOCoin;
import com.samourai.whirlpool.client.wallet.beans.SamouraiAccountIndex;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.script.Script;
import org.bouncycastle.util.encoders.Hex;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class ReviewTxModel extends AndroidViewModel {

    private static final String TAG = "ReviewTxModel";
    public static final String MINER = "miner";

    public static final String MISSING_ADDRESS = "missing address";
    public static final String ADDR_LABEL_BATCH_SPEND = "Batch Spend";

    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    private int account;
    private String address;
    private String addressLabel;
    private String preselectedUtxoId;

    private long amount = 0l;
    private MutableLiveData<Long> _impliedAmount;
    private List<UTXOCoin> preselectedUTXOs;
    private EnumSendType type = SPEND_SIMPLE;
    private EnumSendType sendType = null;
    private MutableLiveData<EnumSendType> impliedSendType;
    private boolean ricochetStaggeredDelivery;
    private Long _balance = null;
    private TxData txData = TxData.create(getApplication());
    private SelectCahootsType.type selectedCahootsType = SelectCahootsType.type.NONE;
    private MutableLiveData<EntropyInfo> entropy = null;
    private MutableLiveData<Map<String, Long>> _fees = null;
    private MutableLiveData<Boolean> isSomethingLoading = new MutableLiveData<>(false);
    private MutableLiveData<Long> _feeAggregated = null;
    private MutableLiveData<Long> _feeLowRate;
    private MutableLiveData<Long> _feeMedRate;
    private MutableLiveData<Long> _feeHighRate;
    private MutableLiveData<Long> _feeMaxRate;
    private MutableLiveData<Long> minerFeeRates = null;
    private MutableLiveData<EnumTransactionPriority> transactionPriority = new MutableLiveData<>(EnumTransactionPriority.NORMAL);
    private MutableLiveData<EnumTransactionPriority> transactionPriorityRequested = new MutableLiveData<>(EnumTransactionPriority.NORMAL);
    private MutableLiveData<String> txNote = new MutableLiveData<>(StringUtils.EMPTY);
    private Pair<List<MyTransactionOutPoint>, List<TransactionOutput>> _pair;
    private List<UTXO> shuffledUtxosP2PKH;
    private List<UTXO> shuffledUtxosP2SH_P2WPKH;
    private List<UTXO> shuffledUtxosP2WPKH;

    private final AtomicBoolean feesRefreshed = new AtomicBoolean();

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

    public LiveData<EnumTransactionPriority> getTransactionPriorityRequested() {
        return transactionPriorityRequested;
    }

    public void setTransactionPriorityRequested(final EnumTransactionPriority priority) {
        transactionPriorityRequested.postValue(priority);
    }

    public MutableLiveData<Boolean> getIsSomethingLoading() {
        return isSomethingLoading;
    }

    public void setIsSomethingLoading(MutableLiveData<Boolean> isSomethingLoading) {
        this.isSomethingLoading = isSomethingLoading;
    }

    public boolean refreshFees(final Runnable postRefresh) {

        if (feesRefreshed.compareAndSet(false, true)) {
            SimpleTaskRunner.create().executeAsync(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    isSomethingLoading.postValue(true);
                    APIFactory.getInstance(getApplication()).loadFees();
                    recomputeOnUpdateFees(computeFeeRate());
                    return null;
                }
            }, new SimpleCallback<Object>() {
                @Override
                public void onComplete(final Object result) {
                    isSomethingLoading.postValue(false);
                    postRefresh.run();
                }

                @Override
                public void onException(final Throwable t) {
                    isSomethingLoading.postValue(false);
                    postRefresh.run();
                }
            });
            return true;
        }

        return false;
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
            if (computeSendType() == SPEND_BOLTZMANN) {
                computeEntropyAsync();
            }
        }
        return entropy;
    }

    private void computeEntropyAsync() {
        try {
            buildBoltzmannTxData();
            final Disposable entropyDisposable = reactiveCalculateEntropy(txData.getSelectedUTXO(), txData.getReceivers())
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
            Log.e(TAG, e.getMessage());
        }
    }

    private final AtomicBoolean minerFeeRatesAndComputationThreadStarted = new AtomicBoolean();
    private final AtomicLong minerFeeRatesThreadCache = new AtomicLong();

    public void setMinerFeeRatesAndComputeFees(final long value) {
        if (minerFeeRatesThreadCache.getAndSet(value) != value) {
            recomputeOnUpdateFees(value);
        }
    }

    public void setMinerFeeRatesAndComputeFeesAsync(final long value) {
        minerFeeRatesThreadCache.set(value);
        if (minerFeeRatesAndComputationThreadStarted.get()) return;
        SimpleTaskRunner.create().executeAsync(new Callable<Long>() {
            @Override
            public Long call() throws Exception {
                if (minerFeeRatesAndComputationThreadStarted.compareAndSet(false, true)) {

                    // pause in order to skip a lot of intermediate computations
                    // to be responsive on touch when using slider for example
                    pauseMillis(150L);

                    minerFeeRatesAndComputationThreadStarted.set(false);

                    // to be more responsive on touch, here it is better to use the param value
                    // given in method than minerFeeRatesThreadCache.
                    // This will reduce the number of intermediate calls of recomputeFees()
                    // on onComplete method
                    return value;
                }
                return null;
            }
        }, new SimpleCallback<Long>() {
            @Override
            public void onComplete(final Long sessionFeeRates) {
                if (isNull(sessionFeeRates)) return;
                if (minerFeeRatesThreadCache.get() == sessionFeeRates.longValue()) {
                    recomputeOnUpdateFees(value);
                } else {
                    setMinerFeeRatesAndComputeFeesAsync(minerFeeRatesThreadCache.get());
                }
            }

            @Override
            public void onException(final Throwable t) {
                final String msg = "on SimpleTaskRunner of setMinerFeeRatesAndComputeFeesAsync : " +
                        t.getMessage();
                Log.e(TAG, msg, t);
            }
        });
    }

    synchronized private void recomputeOnUpdateFees(final long feeRates) {
        setMinerFeeRates(feeRates);
        setSuggestedFee(feeRates);
        setTransactionPriority(feeRates);
        txData.restoreChangeIndexes(getApplication());
        _pair = null;
        recomputeFees();
        if (getSendType() == SPEND_BOLTZMANN) {
            computeEntropyAsync();
        }
    }

    private void setMinerFeeRates(final long value) {
        if (isNull(minerFeeRates)) {
            minerFeeRates = new MutableLiveData<>(value);
        } else {
            minerFeeRates.postValue(value);
        }
    }

    private void setTransactionPriority(final long feeRates) {

        // enable it only if there is no adj on method computeFeeRate() when fees are same
        final boolean detailedPriority = false;
        if (detailedPriority) {
            final FeeUtil feeUtil = FeeUtil.getInstance();
            final String feeIdentifier = feeUtil.retrievesNearFeeIdentifier((int) feeRates);
            final EnumTransactionPriority priority = EnumTransactionPriority.fromIdentifier(
                    feeIdentifier,
                    feeUtil.getFeeRepresentation());
            transactionPriority.postValue(priority);
        } else {
            if (feeRates >= getFeeHighRate().getValue()) {
                transactionPriority.postValue(EnumTransactionPriority.NEXT_BLOCK);
            } else if (feeRates <= getFeeLowRate().getValue()) {
                transactionPriority.postValue(EnumTransactionPriority.LOW);
            } else {
                transactionPriority.postValue(EnumTransactionPriority.NORMAL);
            }
        }
    }

    private static void setSuggestedFee(final long feeRates) {
        final SuggestedFee suggestedFee = new SuggestedFee();
        suggestedFee.setDefaultPerKB(BigInteger.valueOf(feeRates * 1000l));
        FeeUtil.getInstance().setSuggestedFee(suggestedFee);
    }

    private void recomputeFees() {
        impliedSendType.postValue(computeSendType());
        computeFeeValues();
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

    public MutableLiveData<Long> getFeeMaxRate() {
        if (isNull(_feeMaxRate)) {
            computeFeeRate();
        }
        return _feeMaxRate;
    }

    public List<UTXOCoin> getPreselectedUTXOs() {
        return preselectedUTXOs;
    }

    public ReviewTxModel setType(final EnumSendType type) {
        this.type = type;
        if (type == SPEND_BATCH) {
            initForBatchSpend();
        }
        return this;
    }

    private void initForBatchSpend() {
        final List<BatchSend> spendList = BatchSendUtil.getInstance().getSends();
        final Collection<BatchSend> batch = CollectionUtils.emptyIfNull(spendList);
        amount = 0;
        for (final BatchSend spend : batch) {
            amount += spend.amount;
            try {
                spend.computeAddressIfNeeded(getApplication());
            } catch (final Exception e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
        if (isNull(_impliedAmount)) {
            _impliedAmount = new MutableLiveData<>(amount);
        } else {
            _impliedAmount.postValue(amount);
        }
    }

    public ReviewTxModel buildRicochetTxData() {
        return this;
    }

    public ReviewTxModel buildSimleTxData() throws Exception {
        getSimpleFees();
        return this;
    }

    public ReviewTxModel buildSpendBatchTxData() throws Exception {

        txData.clear();

        int countP2WSH_P2TR = 0;
        for (final BatchSendUtil.BatchSend _data : BatchSendUtil.getInstance().getSends()) {

            _data.computeAddressIfNeeded(getApplication());

            Log.d(TAG, "output:" + _data.amount);
            Log.d(TAG, "output:" + _data.addr);
            Log.d(TAG, "output:" + _data.pcode);

            if (txData.getReceivers().containsKey(_data.addr)) {

                final BigInteger addrAmount = txData.getReceivers().get(_data.addr);
                txData.getReceivers().put(_data.addr, addrAmount.add(BigInteger.valueOf(_data.amount)));

            } else {

                txData.getReceivers().put(_data.addr, BigInteger.valueOf(_data.amount));
                if(FormatsUtilGeneric.getInstance().isValidP2WSH_P2TR(_data.addr))    {
                    ++ countP2WSH_P2TR;
                }
            }
        }

        final List<UTXO> utxos = Lists.newArrayList();
        if (account == SamouraiAccountIndex.DEPOSIT) {
            utxos.addAll(APIFactory.getInstance(getApplication()).getUtxos(true));
        } else if (account == SamouraiAccountIndex.POSTMIX) {
            utxos.addAll(APIFactory.getInstance(getApplication()).getUtxosPostMix(true));
        }
        Collections.sort(utxos, new UTXO.UTXOComparator());

        int p2pkh = 0;
        int p2sh_p2wpkh = 0;
        int p2wpkh = 0;
        long totalValueSelected = 0L;
        int totalSelected = 0;

        for (final UTXO utxo : utxos) {
            Log.d(TAG, "utxo value:" + utxo.getValue());
            txData.getSelectedUTXO().add(utxo);
            totalValueSelected += utxo.getValue();
            totalSelected += utxo.getOutpoints().size();
            final Triple<Integer, Integer, Integer> outpointTypes = FeeUtil.getInstance().getOutpointCount(new Vector(utxo.getOutpoints()));
            p2pkh += outpointTypes.getLeft();
            p2sh_p2wpkh += outpointTypes.getMiddle();
            p2wpkh += outpointTypes.getRight();
            final long estimatedFee = FeeUtil.getInstance().estimatedFeeSegwit(
                    p2pkh,
                    p2sh_p2wpkh,
                    p2wpkh,
                    txData.getReceivers().size() - countP2WSH_P2TR + 1,
                    countP2WSH_P2TR
            ).longValue();
            final long spendValueEst = amount + SamouraiWallet.bDust.longValue() + estimatedFee;
            if (totalValueSelected >= spendValueEst) {
                break;
            }
        }

        Log.d(TAG, "totalSelected:" + totalSelected);
        Log.d(TAG, "totalValueSelected:" + totalValueSelected);

        final List<MyTransactionOutPoint> outpoints = Lists.newArrayList();
        for (final UTXO utxo : txData.getSelectedUTXO()) {
            outpoints.addAll(utxo.getOutpoints());
            for (final MyTransactionOutPoint out : utxo.getOutpoints()) {
                Log.d(TAG, "outpoint hash:" + out.getTxHash().toString());
                Log.d(TAG, "outpoint idx:" + out.getTxOutputN());
                Log.d(TAG, "outpoint address:" + out.getAddress());
            }
        }
        final Triple<Integer, Integer, Integer> outpointTypes = FeeUtil.getInstance().getOutpointCount(new Vector(outpoints));
        final BigInteger fee = FeeUtil.getInstance().estimatedFeeSegwit(
                outpointTypes.getLeft(),
                outpointTypes.getMiddle(),
                outpointTypes.getRight(),
                txData.getReceivers().size() - countP2WSH_P2TR + 1, countP2WSH_P2TR
        );

        if (amount + fee.longValue() > getBalance()) {
            Toast.makeText(getApplication(), R.string.insufficient_funds, Toast.LENGTH_SHORT);
            return this;
        }

        final long changeAmount = totalValueSelected - (amount + fee.longValue());
        int change_idx = 0;
        if (changeAmount > 0L) {
            txData.setChange(changeAmount);
            Log.d(TAG, "changeAmount output:" + changeAmount);
        } else {
            Toast.makeText(getApplication(), R.string.error_change_output, Toast.LENGTH_SHORT).show();
            return this;
        }

        return this;
    }

    public ReviewTxModel buildJoinbotTxData() {

        txData.clear();
        txData.getReceivers().put(address, BigInteger.valueOf(getImpliedAmount().getValue()));
        selectedCahootsType = SelectCahootsType.type.MULTI_SOROBAN;
        return this;
    }

    public ReviewTxModel buildBoltzmannTxData() throws Exception {

        txData.clear();
        txData.getReceivers().put(address, BigInteger.valueOf(getImpliedAmount().getValue()));

        int countP2WSH_P2TR = 0;
        if(FormatsUtilGeneric.getInstance().isValidP2WSH_P2TR(address))    {
            countP2WSH_P2TR = 1;
        }

        final Pair<List<MyTransactionOutPoint>, List<TransactionOutput>> pair = getStonewallPair();

        long totalValueSelected = 0L;
        for (final MyTransactionOutPoint outpoint : pair.getLeft()) {

            final UTXO u = new UTXO();
            u.setOutpoints(Lists.newArrayList(outpoint));
            txData.getSelectedUTXO().add(u);
            totalValueSelected += u.getValue();
        }
        for (final TransactionOutput output : pair.getRight()) {
            final Script script = new Script(output.getScriptBytes());
            if (Bech32Util.getInstance().isP2WPKHScript(Hex.toHexString(output.getScriptBytes())) ||
                    Bech32Util.getInstance().isP2TRScript(Hex.toHexString(output.getScriptBytes()))) {

                final String addressFromScript = Bech32Util.getInstance()
                        .getAddressFromScript(script);
                final BigInteger value = BigInteger.valueOf(output.getValue().longValue());
                txData.getReceivers().put(addressFromScript, value);
            } else {
                final String address = script
                        .getToAddress(SamouraiWallet.getInstance().getCurrentNetworkParams())
                        .toString();
                final BigInteger value = BigInteger.valueOf(output.getValue().longValue());
                txData.getReceivers().put(address, value);
            }
        }

        if (txData.getSelectedUTXO().size() > 0) {

            final List<MyTransactionOutPoint> outpoints = new ArrayList<>();
            for (final UTXO utxo : txData.getSelectedUTXO()) {
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

    private Pair<List<MyTransactionOutPoint>, List<TransactionOutput>> getStonewallPair() {
        if (isNull(_pair)) {
            _pair = computeStonewallPair();
        }
        return _pair;
    }

    private Pair<List<MyTransactionOutPoint>, List<TransactionOutput>> computeStonewallPair() {
        final Pair<List<UTXO>, List<UTXO>> utxo1AndUtxo2 = getUtxo1AndUtxo2();
        final List<UTXO> _utxos1 = utxo1AndUtxo2.getLeft();
        final List<UTXO> _utxos2 = utxo1AndUtxo2.getRight();

        // boltzmann spend (STONEWALL)
        return SendFactory.getInstance(getApplication())
                .boltzmann(_utxos1, _utxos2, BigInteger.valueOf(amount), address, account);
    }

    public MutableLiveData<EnumSendType> getImpliedSendType() {
        if (isNull(impliedSendType)) {
            impliedSendType = new MutableLiveData<>(computeSendType());
        }
        return impliedSendType;
    }

    public EnumSendType getSendType() {
        if (isNull(sendType)) {
            computeSendType();
        }
        return sendType;
    }

    private EnumSendType computeSendType() {
        sendType = processSendType();
        return sendType;
    }

    private EnumSendType processSendType() {
        switch (type) {
            case SPEND_SIMPLE:
            case SPEND_BOLTZMANN:
                return canDoBoltzmann() ? SPEND_BOLTZMANN : SPEND_SIMPLE;
            default:
                return type;
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
        return defaultIfBlank(addressLabel, defaultIfBlank(address, MISSING_ADDRESS));
    }

    public long getAmount() {
        return amount;
    }

    public MutableLiveData<Long> getImpliedAmount() {
        if (isNull(_impliedAmount)) {
            _impliedAmount = new MutableLiveData<>(amount);
        }
        return _impliedAmount;
    }

    private void computeFeeValues() {

        final Map<String, Long> fees = computeFees();
        if (isNull(_fees)) {
            _fees = new MutableLiveData<>(fees);
        } else {
            _fees.postValue(fees);
        }

        final long feeAggregated = computeFeeAggregated(fees);
        if (isNull(_feeAggregated)) {
            _feeAggregated = new MutableLiveData<>(feeAggregated);
        } else {
            _feeAggregated.postValue(feeAggregated);
        }
        updateImpliedAmount(feeAggregated);

    }

    private void updateImpliedAmount(final long feeAggregated) {
        final long impliedAmountValue = computeImpliedAmount(feeAggregated);

        if (isNull(_impliedAmount)) {
            _impliedAmount = new MutableLiveData<>(impliedAmountValue);
        } else {
            _impliedAmount.postValue(impliedAmountValue);
        }
    }

    private long computeImpliedAmount(final long feeAggregated) {
        if (amount == getBalance()) {
            return amount - feeAggregated;
        }
        return amount;
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
                        .put("Joinbot Fee", computeJoinbotFee())
                        .build());
            case SPEND_BATCH:
                return getSpendBatchFees();
            case SPEND_SIMPLE:
            default:
                return getSimpleFees();
        }
    }

    private Map<String, Long> getSpendBatchFees() {

        int countP2WSH_P2TR = 0;

        final Set<String> destAddresses = Sets.newHashSet();
        for (final BatchSendUtil.BatchSend batchSend : BatchSendUtil.getInstance().getSends()) {

            try {
                batchSend.computeAddressIfNeeded(getApplication());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            if (! destAddresses.contains(batchSend.addr)) {
                destAddresses.add(batchSend.addr);
                if(FormatsUtilGeneric.getInstance().isValidP2WSH_P2TR(batchSend.addr))    {
                    ++ countP2WSH_P2TR;
                }
            }
        }

        final List<UTXO> utxos = Lists.newArrayList();
        if (account == SamouraiAccountIndex.DEPOSIT) {
            utxos.addAll(APIFactory.getInstance(getApplication()).getUtxos(true));
        } else if (account == SamouraiAccountIndex.POSTMIX) {
            utxos.addAll(APIFactory.getInstance(getApplication()).getUtxosPostMix(true));
        }

        Collections.sort(utxos, new UTXO.UTXOComparator());

        final List<UTXO> selectedUTXO = Lists.newArrayList();
        int p2pkh = 0;
        int p2sh_p2wpkh = 0;
        int p2wpkh = 0;
        long totalValueSelected = 0L;
        int totalSelected = 0;

        for (final UTXO utxo : utxos) {

            selectedUTXO.add(utxo);
            totalValueSelected += utxo.getValue();
            totalSelected += utxo.getOutpoints().size();

            final Triple<Integer, Integer, Integer> outpointTypes = FeeUtil.getInstance()
                    .getOutpointCount(new Vector(utxo.getOutpoints()));
            p2pkh += outpointTypes.getLeft();
            p2sh_p2wpkh += outpointTypes.getMiddle();
            p2wpkh += outpointTypes.getRight();

            final long estimatedFee = FeeUtil.getInstance().estimatedFeeSegwit(
                    p2pkh,
                    p2sh_p2wpkh,
                    p2wpkh,
                    destAddresses.size() - countP2WSH_P2TR + 1,
                    countP2WSH_P2TR
            ).longValue();

            final long spendValueEst = amount + SamouraiWallet.bDust.longValue() + estimatedFee;
            if (totalValueSelected >= spendValueEst) {
                break;
            }
        }

        final List<MyTransactionOutPoint> outpoints = Lists.newArrayList();
        for (final UTXO utxo : selectedUTXO) {
            outpoints.addAll(utxo.getOutpoints());
        }

        final Triple<Integer, Integer, Integer> outpointTypes = FeeUtil.getInstance()
                .getOutpointCount(new Vector(outpoints));

        final BigInteger fee = FeeUtil.getInstance().estimatedFeeSegwit(
                outpointTypes.getLeft(),
                outpointTypes.getMiddle(),
                outpointTypes.getRight(),
                destAddresses.size() - countP2WSH_P2TR + 1, countP2WSH_P2TR
        );

        return Maps.newLinkedHashMap(ImmutableMap.of(MINER, fee.longValue()));
    }

    private long computeJoinbotFee() {
        // 3.5 % fee for Samourai service
        return max(1l, round(amount * 35d / 1000d));
    }

    private Map<String, Long> getBoltzmannFees() {

        final Pair<List<MyTransactionOutPoint>, List<TransactionOutput>> pair = getStonewallPair();

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
            computeFeeValues();
        }
        return _feeAggregated;
    }

    public long getMinerFee() {
        return computeFees().get(MINER);
    }

    private long computeFeeAggregated(final Map<String, Long> fees) {
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

            final Pair<List<MyTransactionOutPoint>, List<TransactionOutput>> pair = getStonewallPair();

            if (isNull(pair)) {
                txData.restoreChangeIndexes(getApplication());
                return false;
            } else {
                return true;
            }
        }
    }

    public int getChangeType() {
        
        if (type == SPEND_BATCH) {
            return 84;
        }

        boolean useLikeType = PrefsUtil.getInstance(getApplication())
                .getValue(PrefsUtil.USE_LIKE_TYPED_CHANGE, true);

        if (nonNull(DojoUtil.getInstance(getApplication()).getDojoParams()) &&
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

    private static <T> List<T> shuffleCopy(final List<T> list) {
        final List<T> copy = Lists.newArrayList(list);
        Collections.shuffle(copy);
        return ImmutableList.copyOf(copy);
    }

    private List<UTXO> getShuffledUtxosP2PKH() {
        if (isNull(shuffledUtxosP2PKH)) {
            if (CollectionUtils.isEmpty(preselectedUTXOs) && isPostmixAccount()) {
                shuffledUtxosP2PKH = ImmutableList.of();
            } else {
                shuffledUtxosP2PKH = shuffleCopy(APIFactory.getInstance(getApplication()).getUtxosP2PKH(true));
            }
        }
        return shuffledUtxosP2PKH;

    }
    private List<UTXO> getShuffledUtxosP2SH_P2WPKH() {
        if (isNull(shuffledUtxosP2SH_P2WPKH)) {
            if (CollectionUtils.isEmpty(preselectedUTXOs) && isPostmixAccount()) {
                shuffledUtxosP2SH_P2WPKH = ImmutableList.of();
            } else {
                shuffledUtxosP2SH_P2WPKH = shuffleCopy(APIFactory.getInstance(getApplication()).getUtxosP2SH_P2WPKH(true));
            }
        }
        return shuffledUtxosP2SH_P2WPKH;
    }

    private List<UTXO> getShuffledUtxosP2WPKH() {
        if (isNull(shuffledUtxosP2WPKH)) {
            if (CollectionUtils.isEmpty(preselectedUTXOs) && isPostmixAccount()) {
                shuffledUtxosP2WPKH = shuffleCopy(APIFactory.getInstance(getApplication()).getUtxosPostMix(true));
            } else {
                shuffledUtxosP2WPKH = shuffleCopy(APIFactory.getInstance(getApplication()).getUtxosP2WPKH(true));
            }
        }
        return shuffledUtxosP2WPKH;
    }

    private Map<String, Long> getSimpleFees() {

        txData.clear();
        txData.getReceivers().put(address, BigInteger.valueOf(getImpliedAmount().getValue()));

        int countP2WSH_P2TR = 0;
        if(FormatsUtilGeneric.getInstance().isValidP2WSH_P2TR(address))    {
            countP2WSH_P2TR = 1;
        }

        final List<UTXO> utxos = getAllUtxo();
        Collections.sort(utxos, UTXO_COMPARATOR_BY_VALUE);
        Collections.reverse(utxos);

        long totalValueSelected = 0l;

        if (amount == getBalance()) {

            txData.getSelectedUTXO().addAll(utxos);
            for (final UTXO u : txData.getSelectedUTXO()) {
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
                    txData.getSelectedUTXO().add(utxo);
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

            if (txData.getSelectedUTXO().size() == 0) {
                // sort in descending order by value
                Collections.sort(utxos, new UTXO.UTXOComparator());
                int selected = 0;
                int p2pkh = 0;
                int p2sh_p2wpkh = 0;
                int p2wpkh = 0;

                // get largest UTXOs > than spend + fee + dust
                for (final UTXO u : utxos) {

                    txData.getSelectedUTXO().add(u);
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

        if (txData.getSelectedUTXO().size() > 0) {

            final List<MyTransactionOutPoint> outpoints = new ArrayList<>();
            for (final UTXO utxo : txData.getSelectedUTXO()) {
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

        // due to random operation in this method, it is preferable to use a cache
        // to compute pair result after amending tx fee rates

        final List<UTXO> utxosP2WPKH = Lists.newArrayList(getShuffledUtxosP2WPKH());
        final List<UTXO> utxosP2SH_P2WPKH = Lists.newArrayList(getShuffledUtxosP2SH_P2WPKH());
        final List<UTXO> utxosP2PKH = Lists.newArrayList(getShuffledUtxosP2PKH());

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

        if (CollectionUtils.isEmpty(_utxos1)) {
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

        if (CollectionUtils.isNotEmpty(_utxos1)) {
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

    public static Single<TxProcessorResult> reactiveCalculateEntropy(
            final List<UTXO> selectedUTXO,
            final Map<String, BigInteger> receivers) {

        return Single.create(emitter -> {
            emitter.onSuccess(calculateEntropy(selectedUTXO, receivers));
        });
    }

    private static TxProcessorResult calculateEntropy(
            final List<UTXO> selectedUTXO,
            final Map<String, BigInteger> receivers) {

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
        return result;
    }

    public boolean isEnabledRicochetStaggered() {
        return !AppUtil.getInstance(getApplication()).isBroadcastDisabled() &&
                ricochetStaggeredDelivery;
    }

    /**
     * return selected feeRate
     */
    private long computeFeeRate() {

        long feeLow = FeeUtil.getInstance().getLowFee().getDefaultPerKB().longValue();
        long feeMed = FeeUtil.getInstance().getNormalFee().getDefaultPerKB().longValue();
        long feeHigh = FeeUtil.getInstance().getHighFee().getDefaultPerKB().longValue();

        if (feeLow == feeMed && feeMed == feeHigh) {
            // offset of low and high
            feeLow = max(1, feeMed * 85L / 100L);
            feeHigh = feeMed * 115L / 100L;
            final SuggestedFee lo_sf = new SuggestedFee();
            lo_sf.setDefaultPerKB(BigInteger.valueOf(feeLow / 1000L * 1000L));
            FeeUtil.getInstance().setLowFee(lo_sf);
            final SuggestedFee hi_sf = new SuggestedFee();
            hi_sf.setDefaultPerKB(BigInteger.valueOf(feeHigh / 1000L * 1000L));
            FeeUtil.getInstance().setHighFee(hi_sf);
        } else if (feeLow == feeMed || feeMed == feeHigh) {
            if (FeeUtil.getInstance().getFeeRepresentation() == EnumFeeRepresentation.BLOCK_COUNT) {
                // offset of mid
                feeMed = (feeLow + feeHigh) / 2L;
                final SuggestedFee mi_sf = new SuggestedFee();
                mi_sf.setDefaultPerKB(BigInteger.valueOf(feeMed / 1000L * 1000L));
                FeeUtil.getInstance().setNormalFee(mi_sf);
            } else if (feeLow == feeMed) {
                feeLow = max(1, feeMed * 85L / 100L);
                final SuggestedFee lo_sf = new SuggestedFee();
                lo_sf.setDefaultPerKB(BigInteger.valueOf(feeLow / 1000L * 1000L));
                FeeUtil.getInstance().setLowFee(lo_sf);
            } else {
                feeHigh = feeMed * 115L / 100L;
                final SuggestedFee hi_sf = new SuggestedFee();
                hi_sf.setDefaultPerKB(BigInteger.valueOf(feeHigh / 1000L * 1000L));
                FeeUtil.getInstance().setHighFee(hi_sf);
            }
        }

        if (feeLow < 1000L) {
            feeLow = 1000L;
            final SuggestedFee lo_sf = new SuggestedFee();
            lo_sf.setDefaultPerKB(BigInteger.valueOf(feeLow));
            FeeUtil.getInstance().setLowFee(lo_sf);
        }
        if (feeMed < 1000L) {
            feeMed = 1000L;
            final SuggestedFee mi_sf = new SuggestedFee();
            mi_sf.setDefaultPerKB(BigInteger.valueOf(feeMed));
            FeeUtil.getInstance().setNormalFee(mi_sf);
        }
        if (feeHigh < 1000L) {
            feeHigh = 1000L;
            final SuggestedFee hi_sf = new SuggestedFee();
            hi_sf.setDefaultPerKB(BigInteger.valueOf(feeHigh));
            FeeUtil.getInstance().setHighFee(hi_sf);
        }

        if (isNull(_feeLowRate)) {
            _feeLowRate = new MutableLiveData<>(feeLow / 1000L);
        } else {
            _feeLowRate.postValue(feeLow / 1000L);
        }

        if (isNull(_feeMedRate)) {
            _feeMedRate = new MutableLiveData<>(feeMed / 1000L);
            setSuggestedFee(_feeMedRate.getValue());
        } else {
            _feeMedRate.postValue(feeMed / 1000L);
        }

        if (isNull(_feeHighRate)) {
            _feeHighRate = new MutableLiveData<>(feeHigh / 1000L);
        } else {
            _feeHighRate.postValue(feeHigh / 1000L);
        }

        final long maxFee = max(1L, (feeHigh/2L + feeHigh) / 1000L + 10L);
        if (isNull(_feeMaxRate)) {
            _feeMaxRate = new MutableLiveData<>(maxFee);
        } else {
            _feeMaxRate.postValue(maxFee);
        }

        if (nonNull(minerFeeRates)) {
            if (minerFeeRates.getValue() > maxFee) {
                minerFeeRates.postValue(maxFee);
                return maxFee;
            } else {
                return minerFeeRates.getValue();
            }
        } else {
            return 0L;
        }
    }
}
