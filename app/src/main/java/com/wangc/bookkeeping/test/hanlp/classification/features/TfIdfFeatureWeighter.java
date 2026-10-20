package com.wangc.bookkeeping.test.hanlp.classification.features;

public class TfIdfFeatureWeighter implements IFeatureWeighter {
    private static final long serialVersionUID = 3180282829151489479L;

    public int numDocs;
    public int[] df;

    public TfIdfFeatureWeighter() {}

    public TfIdfFeatureWeighter(int numDocs, int[] df) {
        this.numDocs = numDocs;
        this.df = df;
    }

    @Override
    public double weight(int feature, int tf) {
        if (this.df == null || feature < 0 || feature >= this.df.length) {
            return 0.0;
        }
        int featureDf = this.df[feature];
        if (featureDf <= 0) {
            return 0.0;
        }
        return Math.log10(tf + 1.0) * Math.log10((((double) this.numDocs) / ((double) featureDf)) + 1.0);
    }
}
