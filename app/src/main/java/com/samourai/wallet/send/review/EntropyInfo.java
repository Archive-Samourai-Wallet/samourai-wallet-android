package com.samourai.wallet.send.review;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EntropyInfo that = (EntropyInfo) o;
        return Objects.equals(entropy, that.entropy) && Objects.equals(interpretations, that.interpretations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entropy, interpretations);
    }
}
