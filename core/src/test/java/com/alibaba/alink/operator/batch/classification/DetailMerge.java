package com.alibaba.alink.operator.batch.classification;

import org.apache.flink.ml.api.misc.param.Params;

import com.alibaba.alink.common.annotation.NameCn;
import com.alibaba.alink.pipeline.MapTransformer;

@NameCn("向量函数")
public class DetailMerge extends MapTransformer <DetailMerge>
	implements DetailMergeParams <DetailMerge> {

	private static final long serialVersionUID = 1611703161062319302L;

	public DetailMerge() {
		this(null);
	}

	public DetailMerge(Params param) {
		super(DetailMergeMapper::new, param);
	}

}

