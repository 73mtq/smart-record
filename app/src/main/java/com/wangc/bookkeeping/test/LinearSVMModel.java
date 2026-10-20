package com.wangc.bookkeeping.test;

import com.wangc.bookkeeping.test.hanlp.classification.features.IFeatureWeighter;
import com.wangc.bookkeeping.test.hanlp.classification.models.AbstractModel;
import de.bwaldvogel.liblinear.Model;

public class LinearSVMModel extends AbstractModel {
    private static final long serialVersionUID = -5859047455009671578L;

    public int c;
    public int d;
    public int n;
    public IFeatureWeighter featureWeighter;
    public Model svmModel;
}
