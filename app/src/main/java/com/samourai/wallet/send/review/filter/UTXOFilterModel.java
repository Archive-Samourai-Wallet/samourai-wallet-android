package com.samourai.wallet.send.review.filter;

public class UTXOFilterModel {

    private boolean segwitNative = true;
    private boolean segwitCompatible = true;
    private boolean legacy = true;
    private boolean payNymOutputs = true;
    private boolean unmixedToxicChange = true;
    private boolean mixedOutputs = true;
    private boolean postmixTransactionChange = true;
    private boolean unconfirmed = true;

    private UTXOFilterModel() {}

    public static UTXOFilterModel create() {
        return new UTXOFilterModel();
    }

    public static UTXOFilterModel createCopy(final UTXOFilterModel model) {
        return UTXOFilterModel.create().updateWith(model);
    }

    public UTXOFilterModel updateWith(final UTXOFilterModel model) {
        return setLegacy(model.isLegacy())
                .setSegwitCompatible(model.isSegwitCompatible())
                .setSegwitNative(model.isSegwitNative())
                .setPayNymOutputs(model.isPayNymOutputs())
                .setUnmixedToxicChange(model.isUnmixedToxicChange())
                .setMixedOutputs(model.isMixedOutputs())
                .setPostmixTransactionChange(model.isPostmixTransactionChange())
                .setUnconfirmed(model.isUnconfirmed())
                ;
    }

    public UTXOFilterModel setSegwitNative(boolean segwitNative) {
        this.segwitNative = segwitNative;
        return this;
    }

    public UTXOFilterModel setSegwitCompatible(boolean segwitCompatible) {
        this.segwitCompatible = segwitCompatible;
        return this;
    }

    public UTXOFilterModel setLegacy(boolean legacy) {
        this.legacy = legacy;
        return this;
    }

    public UTXOFilterModel setPayNymOutputs(boolean payNymOutputs) {
        this.payNymOutputs = payNymOutputs;
        return this;
    }

    public UTXOFilterModel setUnmixedToxicChange(boolean unmixedToxicChange) {
        this.unmixedToxicChange = unmixedToxicChange;
        return this;
    }

    public UTXOFilterModel setMixedOutputs(boolean mixedOutputs) {
        this.mixedOutputs = mixedOutputs;
        return this;
    }

    public UTXOFilterModel setPostmixTransactionChange(boolean postmixTransactionChange) {
        this.postmixTransactionChange = postmixTransactionChange;
        return this;
    }

    public UTXOFilterModel setUnconfirmed(boolean unconfirmed) {
        this.unconfirmed = unconfirmed;
        return this;
    }

    public boolean isSegwitNative() {
        return segwitNative;
    }

    public boolean isSegwitCompatible() {
        return segwitCompatible;
    }

    public boolean isLegacy() {
        return legacy;
    }

    public boolean isPayNymOutputs() {
        return payNymOutputs;
    }

    public boolean isUnmixedToxicChange() {
        return unmixedToxicChange;
    }

    public boolean isMixedOutputs() {
        return mixedOutputs;
    }

    public boolean isPostmixTransactionChange() {
        return postmixTransactionChange;
    }

    public boolean isUnconfirmed() {
        return unconfirmed;
    }

    public boolean isAllChecked(final boolean postmixAccount) {
        if (postmixAccount) {
            return segwitNative &&
                    segwitCompatible &&
                    legacy &&
                    mixedOutputs &&
                    postmixTransactionChange &&
                    unconfirmed;
        } else {
            return segwitNative &&
                    segwitCompatible &&
                    legacy &&
                    payNymOutputs &&
                    unmixedToxicChange &&
                    unconfirmed;
        }
    }
}
