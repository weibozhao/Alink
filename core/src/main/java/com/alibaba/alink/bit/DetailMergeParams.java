package com.alibaba.alink.bit;

import com.alibaba.alink.params.clustering.lda.HasAlphaArray;
import com.alibaba.alink.params.shared.colname.HasReservedColsDefaultAsNull;
import com.alibaba.alink.params.shared.colname.HasSelectedCols;

public interface DetailMergeParams<T> extends
	HasSelectedCols <T>, HasReservedColsDefaultAsNull<T>,
	HasAlphaArray <T> {

}