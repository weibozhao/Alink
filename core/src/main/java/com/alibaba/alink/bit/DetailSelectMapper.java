package com.alibaba.alink.bit;

import com.alibaba.alink.common.mapper.Mapper;
import com.alibaba.alink.common.type.AlinkTypes;
import com.alibaba.alink.params.shared.colname.HasSelectedCols;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.ml.api.misc.param.Params;
import org.apache.flink.table.api.TableSchema;

/**
 * This mapper maps many columns to one vector. the columns should be vector or numerical columns.
 */
public class DetailSelectMapper extends Mapper {

	private static final long serialVersionUID = -8419340084734506661L;

	private double[] alphaArray;
	public DetailSelectMapper(TableSchema dataSchema, Params params) {
		super(dataSchema, params);
		alphaArray = params.get(DetailMerge.ALPHA_ARRAY);
	}

	@Override
	protected Tuple4 <String[], String[], TypeInformation <?>[], String[]> prepareIoSchema(TableSchema dataSchema,
																						   Params params) {
		return Tuple4.of(params.get(HasSelectedCols.SELECTED_COLS), new String[]{"label", "type", "detail"},
			new TypeInformation[]{AlinkTypes.INT, AlinkTypes.INT, AlinkTypes.STRING}, new String[]{});
	}

	@Override
	protected void map(SlicedSelectedSample selection, SlicedResult result) throws Exception {
		int type = (int) selection.get(1);
		int label = (int) selection.get(0);
		result.set(0, label);
		result.set(1, type);
		if (type == 1) {
			result.set(2, selection.get(2));
		} else if (type == 0){
			result.set(2, selection.get(3));
		} else {
			result.set(2, selection.get(3));

		}
	}
}
