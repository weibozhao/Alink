package com.alibaba.alink.operator.batch.classification;

import org.apache.flink.types.Row;

import com.alibaba.alink.common.AlinkGlobalConfiguration;
import com.alibaba.alink.operator.batch.BatchOperator;
import com.alibaba.alink.operator.batch.sink.AkSinkBatchOp;
import com.alibaba.alink.operator.batch.source.AkSourceBatchOp;
import com.alibaba.alink.operator.batch.source.CsvSourceBatchOp;
import com.alibaba.alink.operator.batch.source.MemSourceBatchOp;
import com.alibaba.alink.pipeline.Pipeline;
import com.alibaba.alink.pipeline.classification.FmClassifier;
import com.alibaba.alink.pipeline.classification.GbdtClassifier;
import com.alibaba.alink.pipeline.classification.LinearSvm;
import com.alibaba.alink.pipeline.classification.LogisticRegression;
import com.alibaba.alink.pipeline.classification.NaiveBayes;
import com.alibaba.alink.pipeline.classification.RandomForestClassifier;
import com.alibaba.alink.pipeline.dataproc.StandardScaler;
import com.alibaba.alink.pipeline.tuning.BinaryClassificationTuningEvaluator;
import com.alibaba.alink.pipeline.tuning.GridSearchCV;
import com.alibaba.alink.pipeline.tuning.ParamGrid;
import com.alibaba.alink.testutil.AlinkTestBase;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class SvmTest extends AlinkTestBase {

	@Test
	public void testGridSearch() throws Exception {
		AlinkGlobalConfiguration.setPrintProcessInfo(true);
		BatchOperator.setParallelism(1);

		String path = "/Users/weibo/workspace/data/blg/blgs_app.csv";

		BatchOperator data = new CsvSourceBatchOp().setFilePath(path).setFieldDelimiter(",").setSchemaStr(
			"f0 string, f1 double,  f2 double,  f3 double, "
				+ " f4 double,  f5 double,  f6 double,  f7 double,  f8 double, "
				+ " f9 double,  f10 double,  f11 double,  f12 double,  f13 double, "
				+ " f14 double,  f15 double,  f16 double,  f17 double,  f18 double, "
				+ " f19 double,  f20 double, f21 double, label int");

		for (int i = 0; i < 5; ++i) {
			data = data.shuffle();


			data.link(new AkSinkBatchOp().setOverwriteSink(true).setFilePath("/tmp/tmp_trian.ak"));
			BatchOperator.execute();

			BatchOperator trainData = new AkSourceBatchOp().setFilePath("/tmp/tmp_trian.ak");
			String[] numCols = new String[] {
				"f1",
				"f2",
				"f3",
				"f4",
				"f5",
				"f6",
				"f7",
				"f8",
				"f9",
				"f10", "f11", "f12",
				"f13",
				"f14", "f15",
				"f16",
				"f17",
				"f18",
				"f19",
				"f20", "f21"
			};
			StandardScaler standardScaler = new StandardScaler().setSelectedCols(numCols);

			GbdtClassifier gbdt = new GbdtClassifier()
				.setFeatureCols(numCols)
				.setNumTrees(10)
				.setMaxDepth(4)
				.setLabelCol("label")
				.setPredictionCol("pred")
				.setPredictionDetailCol("detail_gbdt");

			BinaryClassificationTuningEvaluator tuningEvaluator = new BinaryClassificationTuningEvaluator()
				.setLabelCol("label")
				.setPredictionDetailCol("detail")
				.setTuningBinaryClassMetric("accuracy");

			ParamGrid paramGrid1 = new ParamGrid()
				.addGrid(gbdt, GbdtClassifier.MAX_DEPTH, new Integer[] {4})
				.addGrid(gbdt, GbdtClassifier.GAMMA, new Double[] {3.0})
				.addGrid(gbdt, GbdtClassifier.NUM_TREES, new Integer[] {20})
				.addGrid(gbdt, GbdtClassifier.MAX_BINS, new Integer[] {16})
				.addGrid(gbdt, GbdtClassifier.MIN_SAMPLES_PER_LEAF, new Integer[] {1})
				.addGrid(gbdt, GbdtClassifier.LAMBDA, new Double[] {0.3});

			FmClassifier fmClassifier = new FmClassifier()
				.setFeatureCols(numCols)
				.setLearnRate(0.005)
				.setLabelCol("label").setNumFactor(5)
				.setWithLinearItem(true)
				.setWithIntercept(true)
				.setNumEpochs(10)
				.setLambda2(0.01)
				.setInitStdev(0.05)
				.setPredictionCol("pred").setPredictionDetailCol("detail_fm");
			FmClassifier fmClassifier1 = new FmClassifier()
				.setFeatureCols(numCols)
				.setLearnRate(0.01)
				.setLabelCol("label").setNumFactor(0)
				.setWithLinearItem(true)
				.setWithIntercept(true)
				.setNumEpochs(5)
				.setLambda2(0.1)
				.setInitStdev(0.05)
				.setPredictionCol("pred").setPredictionDetailCol("detail_fm1");
			LogisticRegression lr = new LogisticRegression()
				.setFeatureCols(numCols)
				.setOptimMethod("SGD")
				.setMaxIter(200)
				.setLearningRate(0.1)
				.setLabelCol("label")
				.setPredictionCol("pred").setPredictionDetailCol("detail_LR");
			LinearSvm svm = new LinearSvm()
				.setFeatureCols(numCols)
				.setOptimMethod("SGD")
				.setLearningRate(0.1)
				.setMaxIter(200)
				.setLabelCol("label")
				.setPredictionCol("pred").setPredictionDetailCol("detail_svm");
			ParamGrid paramGridLr = new ParamGrid()
				.addGrid(lr, LogisticRegression.WITH_INTERCEPT, new Boolean[] {true});

			RandomForestClassifier forestClassifier = new RandomForestClassifier()
				.setFeatureCols(numCols)
				.setLabelCol("label")
				.setMaxDepth(4)
				.setMaxLeaves(100)
				.setMaxBins(128)
				.setPredictionCol("pred")
				.setPredictionDetailCol("detail_RF");

			NaiveBayes nb = new NaiveBayes()
				.setFeatureCols(numCols)
				.setSmoothing(0.1)
				.setLabelCol("label")
				.setPredictionCol("pred").setPredictionDetailCol("detail_nb");

			Pipeline pipeline = new Pipeline()
				.add(standardScaler)
				.add(fmClassifier)
				//.add(lr)
				//.add(svm)
				//.add(nb)
				.add(forestClassifier)
				.add(new DetailMerge().setSelectedCols("label"
					, "detail_RF"
					, "detail_fm"
					//, "detail_lr"
					//, "detail_nb"
					//, "detail_svm"
				).setAlphaArray(new double[]{0.3, 0.7}));

			ParamGrid paramGridRF = new ParamGrid();
			//	.addGrid(forestClassifier, RandomForestClassifier.MAX_DEPTH, new Integer[] {6});
			//.addGrid(nb, NaiveBayes.SMOOTHING, new Double[] {1.0,0.1,0.01});
			GridSearchCV gridSearchCV = new GridSearchCV()
				.setEstimator(pipeline)
				.setTuningEvaluator(tuningEvaluator)
				.setNumFolds(5)
				.setParamGrid(paramGridRF);
			gridSearchCV.fit(trainData);
		}
	}

	BatchOperator <?> getData() {
		Row[] array = new Row[] {
			Row.of("$31$0:1.0 1:1.0 2:1.0 30:1.0", "1.0  1.0  1.0  1.0", 1.0, 1.0, 1.0, 1.0, 1),
			Row.of("$31$0:1.0 1:1.0 2:0.0 30:1.0", "1.0  1.0  0.0  1.0", 1.0, 1.0, 0.0, 1.0, 1),
			Row.of("$31$0:1.0 1:0.0 2:1.0 30:1.0", "1.0  0.0  1.0  1.0", 1.0, 0.0, 1.0, 1.0, 1),
			Row.of("$31$0:1.0 1:0.0 2:1.0 30:1.0", "1.0  0.0  1.0  1.0", 1.0, 0.0, 1.0, 1.0, 1),
			Row.of("$31$0:0.0 1:1.0 2:1.0 30:0.0", "0.0  1.0  1.0  0.0", 0.0, 1.0, 1.0, 0.0, 0),
			Row.of("$31$0:0.0 1:1.0 2:1.0 30:0.0", "0.0  1.0  1.0  0.0", 0.0, 1.0, 1.0, 0.0, 0),
			Row.of("$31$0:0.0 1:1.0 2:1.0 30:0.0", "0.0  1.0  1.0  0.0", 0.0, 1.0, 1.0, 0.0, 0),
			Row.of("$31$0:0.0 1:1.0 2:1.0 30:0.0", "0.0  1.0  1.0  0.0", 0.0, 1.0, 1.0, 0.0, 0)
		};

		return new MemSourceBatchOp(
			Arrays.asList(array), new String[] {"svec", "vec", "f0", "f1", "f2", "f3", "labels"});

	}

	@Test
	public void batchTest() {
		String[] xVars = new String[] {"f0", "f1", "f2", "f3"};
		String yVar = "labels";
		String vectorName = "vec";
		String svectorName = "svec";
		BatchOperator <?> trainData = getData();

		LinearSvmTrainBatchOp svm = new LinearSvmTrainBatchOp()
			.setLabelCol(yVar)
			.setFeatureCols(xVars)
			.setOptimMethod("gd").linkFrom(trainData);

		svm.lazyPrintTrainInfo();
		svm.lazyPrintModelInfo();
		svm.getSideOutput(0).collect();
		svm.getSideOutput(1).collect();

		LinearSvmTrainBatchOp vectorSvm = new LinearSvmTrainBatchOp()
			.setLabelCol(yVar)
			.setVectorCol(vectorName).linkFrom(trainData);

		LinearSvmTrainBatchOp sparseVectorSvm = new LinearSvmTrainBatchOp()
			.setLabelCol(yVar)
			.setVectorCol(svectorName)
			.setOptimMethod("lbfgs")
			.setMaxIter(100).linkFrom(trainData);

		BatchOperator <?> result1 = new LinearSvmPredictBatchOp()
			.setPredictionCol("svmpred").linkFrom(svm, trainData);
		BatchOperator <?> result2 = new LinearSvmPredictBatchOp()
			.setPredictionCol("svsvmpred").linkFrom(vectorSvm, result1);
		BatchOperator <?> result3 = new LinearSvmPredictBatchOp()
			.setReservedCols(new String[] {yVar, "svmpred", "svsvmpred"})
			.setPredictionCol("dvsvmpred").linkFrom(sparseVectorSvm, result2);

		List <Row> d = result3.collect();
		for (Row row : d) {
			for (int i = 1; i < 4; ++i) {
				Assert.assertEquals(row.getField(0), row.getField(i));
			}
		}
	}
}
