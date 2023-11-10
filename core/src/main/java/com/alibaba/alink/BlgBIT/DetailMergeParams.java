package com.alibaba.alink.BlgBIT;

import com.alibaba.alink.params.clustering.lda.HasAlphaArray;
import com.alibaba.alink.params.shared.colname.HasSelectedCols;

public interface DetailMergeParams<T> extends
	HasSelectedCols <T>,
	HasAlphaArray <T> {

}