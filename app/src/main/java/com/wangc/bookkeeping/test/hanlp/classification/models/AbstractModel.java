package com.wangc.bookkeeping.test.hanlp.classification.models;

import com.hankcs.hanlp.collection.trie.bintrie.BinTrie;
import com.wangc.bookkeeping.test.hanlp.classification.tokenizers.ITokenizer;
import java.io.Serializable;

public abstract class AbstractModel implements Serializable {
    private static final long serialVersionUID = 3492092230110686076L;

    public String[] catalog;
    public ITokenizer tokenizer;
    public BinTrie<Integer> wordIdTrie;
}
