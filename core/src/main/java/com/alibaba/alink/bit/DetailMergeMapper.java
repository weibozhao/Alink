package com.alibaba.alink.bit;

import com.alibaba.alink.params.shared.colname.HasReservedColsDefaultAsNull;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.ml.api.misc.param.Params;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.type.TypeReference;
import org.apache.flink.table.api.TableSchema;

import com.alibaba.alink.common.mapper.Mapper;
import com.alibaba.alink.common.type.AlinkTypes;
import com.alibaba.alink.common.utils.JsonConverter;
import com.alibaba.alink.params.shared.colname.HasSelectedCols;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * This mapper maps many columns to one vector. the columns should be vector or numerical columns.
 */
public class DetailMergeMapper extends Mapper {

	private static final long serialVersionUID = -8419340084734506661L;

	private double[] alphaArray;
	public DetailMergeMapper(TableSchema dataSchema, Params params) {
		super(dataSchema, params);
		alphaArray = params.get(DetailMerge.ALPHA_ARRAY);
	}

	@Override
	protected Tuple4 <String[], String[], TypeInformation <?>[], String[]> prepareIoSchema(TableSchema dataSchema,
																						   Params params) {
		return Tuple4.of(params.get(HasSelectedCols.SELECTED_COLS), new String[]{"detail"},
			new TypeInformation[]{AlinkTypes.STRING}, params.get(HasReservedColsDefaultAsNull.RESERVED_COLS));
	}

	@Override
	protected void map(SlicedSelectedSample selection, SlicedResult result) throws Exception {
		if (alphaArray == null) {
			alphaArray = new double[selection.length()];
			Arrays.fill(alphaArray, 1.0 / (selection.length()));
		}
		Map <String, Double> detail = new HashMap <>();
		for (int i = 0; i < selection.length(); ++i) {
			String json1 = (String) selection.get(i);

			Map <String, Object> detail_fo = JsonConverter.fromJson(json1,
				new TypeReference <HashMap <String, Object>>() {}.getType());
			int size = selection.length();
			for (String s : detail_fo.keySet()) {
				if (detail.containsKey(s)) {
					double val = Double.parseDouble(detail_fo.get(s).toString());
					detail.put(s, alphaArray[i] * val +  detail.get(s));
				} else {
					double val = Double.parseDouble(detail_fo.get(s).toString());
					detail.put(s, alphaArray[i] * val);
				}
			}
		}
		String jsonDetail = JsonConverter.toJson(detail);
		result.set(0, jsonDetail);
	}
}
