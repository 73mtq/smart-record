package com.wangc.bookkeeping.test.hanlp.classification.features;

import java.io.Serializable;

public interface IFeatureWeighter extends Serializable {
    double weight(int feature, int tf);
}
