package com.alibaba.alink.bit;

import com.alibaba.alink.common.annotation.NameCn;
import com.alibaba.alink.pipeline.MapTransformer;
import org.apache.flink.ml.api.misc.param.Params;

@NameCn("向量函数")
public class DetailSelect extends MapTransformer <DetailSelect>
	implements DetailMergeParams <DetailSelect> {

	private static final long serialVersionUID = 1611703161062319302L;

	public DetailSelect() {
		this(null);
	}

	public DetailSelect(Params param) {
		super(DetailSelectMapper::new, param);
	}

}

