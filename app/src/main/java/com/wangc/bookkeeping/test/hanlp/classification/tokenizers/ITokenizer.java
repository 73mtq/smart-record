package com.wangc.bookkeeping.test.hanlp.classification.tokenizers;

import java.io.Serializable;

public interface ITokenizer extends Serializable {
    String[] segment(String text);
}
