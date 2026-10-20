package com.wangc.bookkeeping.test.hanlp.classification.tokenizers;

import com.hankcs.hanlp.seg.common.Term;
import com.hankcs.hanlp.tokenizer.StandardTokenizer;
import java.util.ArrayList;
import java.util.List;

public class HanLPTokenizer implements ITokenizer {
    private static final long serialVersionUID = 438833387961750207L;

    @Override
    public String[] segment(String str) {
        if (str == null || str.isEmpty()) {
            return new String[0];
        }
        List<Term> list = StandardTokenizer.segment(str);
        List<String> words = new ArrayList<>(list.size());
        for (Term term : list) {
            if (term.word != null && term.word.indexOf(0) < 0) {
                words.add(term.word);
            }
        }
        return words.toArray(new String[0]);
    }
}
