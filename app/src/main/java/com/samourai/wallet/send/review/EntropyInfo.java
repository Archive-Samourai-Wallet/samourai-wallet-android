package com.samourai.wallet.send.review;

public class EntropyInfo {

    private Double entropy;
    private Integer interpretations;

    private EntropyInfo() {}

    public static EntropyInfo create() {
        return new EntropyInfo();
    }

    public Double getEntropy() {
        return entropy;
    }

    public EntropyInfo setEntropy(final Double entropy) {
        this.entropy = entropy;
        return this;
    }

    public Integer getInterpretations() {
        return interpretations;
    }

    public EntropyInfo setInterpretations(final Integer interpretations) {
        this.interpretations = interpretations;
        return this;
    }
}
