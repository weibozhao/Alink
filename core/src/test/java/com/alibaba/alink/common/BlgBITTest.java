package com.alibaba.alink.common;

import com.alibaba.alink.BlgBIT.DetailMerge;
import com.alibaba.alink.operator.batch.BatchOperator;
import com.alibaba.alink.operator.batch.sink.AkSinkBatchOp;
import com.alibaba.alink.operator.batch.source.AkSourceBatchOp;
import com.alibaba.alink.operator.batch.source.CsvSourceBatchOp;
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
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileReader;

public class BlgBITTest {

	@Test
	public void testGridSearch() throws Exception {
		AlinkGlobalConfiguration.setPrintProcessInfo(true);
		BatchOperator.setParallelism(1);

		String path = "/home/weibo/workspace/data/blgs_app.csv";

		BatchOperator<?> data = new CsvSourceBatchOp().setFilePath(path).setFieldDelimiter(",").setSchemaStr(
			"f0 string, f1 double,  f2 double,  f3 double, "
				+ " f4 double,  f5 double,  f6 double,  f7 double,  f8 double, "
				+ " f9 double,  f10 double,  f11 double,  f12 double,  f13 double, "
				+ " f14 double,  f15 double,  f16 double,  f17 double,  f18 double, "
				+ " f19 double,  f20 double, f21 double, label int");

		for (int i = 0; i < 100; ++i) {
			System.out.println("Round: " + i);
			data = data.shuffle();

			data.link(new AkSinkBatchOp().setOverwriteSink(true).setFilePath("/tmp/tmp_trian.ak"));
			BatchOperator.execute();

			BatchOperator<?> trainData = new AkSourceBatchOp().setFilePath("/tmp/tmp_trian.ak");
			String[] numCols = new String[] {
				"f1", "f2", "f3", "f4", "f5", "f6", "f7",
				"f8", "f9", "f10", "f11", "f12", "f13", "f14", "f15",
				"f16", "f17", "f18", "f19", "f20", "f21"
			};
			StandardScaler standardScaler = new StandardScaler().setSelectedCols(numCols);

			GbdtClassifier gbdt = new GbdtClassifier()
				.setFeatureCols(numCols)
				.setMaxDepth(4)
				.setLabelCol("label")
				.setPredictionCol("pred")
				.setPredictionDetailCol("detail_gbdt");

			BinaryClassificationTuningEvaluator tuningEvaluator = new BinaryClassificationTuningEvaluator()
				.setLabelCol("label")
				.setPredictionDetailCol("detail")
				.setTuningBinaryClassMetric("accuracy");

			FmClassifier fmClassifier = new FmClassifier()
				.setFeatureCols(numCols)
				.setLearnRate(0.004)
				.setLabelCol("label").setNumFactor(5)
				.setWithLinearItem(true)
				.setWithIntercept(true)
				.setNumEpochs(10)
				.setLambda2(0.01)
				.setInitStdev(0.05)
				.setPredictionCol("pred").setPredictionDetailCol("detail_fm");

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

			RandomForestClassifier forestClassifier = new RandomForestClassifier()
				.setFeatureCols(numCols)
				.setLabelCol("label")
				.setMaxDepth(4)
				.setMaxLeaves(100)
				.setMaxBins(128)
				.setPredictionCol("pred")
				.setPredictionDetailCol("detail_RF");

			RandomForestClassifier forestClassifier1 = new RandomForestClassifier()
					.setFeatureCols(numCols)
					.setLabelCol("label")
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
				//.add(gbdt)
				//.add(lr)
				//.add(svm)
				//.add(nb)
				.add(forestClassifier1)
				.add(new DetailMerge().setSelectedCols("label"
					, "detail_RF"
					//, "detail_gbdt"
					, "detail_fm"
					, "detail_lr"
					//, "detail_nb"
					//, "detail_svm"
				).setAlphaArray(new double[]{0.5, 0.5}));

			ParamGrid paramGridRF = new ParamGrid();
			//.addGrid(forestClassifier, RandomForestClassifier.MAX_DEPTH, new Integer[] {6});
			//.addGrid(nb, NaiveBayes.SMOOTHING, new Double[] {1.0,0.1,0.01});
			GridSearchCV gridSearchCV = new GridSearchCV()
				.setEstimator(pipeline)
				.setTuningEvaluator(tuningEvaluator)
				.setNumFolds(5)
				.setParamGrid(paramGridRF);
			gridSearchCV.fit(trainData);


			Pipeline pipeline1 = new Pipeline()
					.add(standardScaler)
					//.add(fmClassifier)
					//.add(gbdt)
					//.add(lr)
					//.add(svm)
					//.add(nb)
					.add(forestClassifier1)
					.add(new DetailMerge().setSelectedCols("label"
							, "detail_RF"
							//, "detail_gbdt"
							//, "detail_fm"
							//, "detail_lr"
							//, "detail_nb"
							//, "detail_svm"
					).setAlphaArray(new double[]{1.0}));

			//.addGrid(forestClassifier, RandomForestClassifier.MAX_DEPTH, new Integer[] {6});
			//.addGrid(nb, NaiveBayes.SMOOTHING, new Double[] {1.0,0.1,0.01});
			GridSearchCV gridSearchCV1 = new GridSearchCV()
					.setEstimator(pipeline1)
					.setTuningEvaluator(tuningEvaluator)
					.setNumFolds(5)
					.setParamGrid(paramGridRF);
			gridSearchCV1.fit(trainData);


			Pipeline pipeline2 = new Pipeline()
					.add(standardScaler)
					.add(fmClassifier)
					//.add(gbdt)
					//.add(lr)
					//.add(svm)
					//.add(nb)
					//.add(forestClassifier1)
					.add(new DetailMerge().setSelectedCols("label"
							//, "detail_RF"
							//, "detail_gbdt"
							, "detail_fm"
							//, "detail_lr"
							//, "detail_nb"
							//, "detail_svm"
					).setAlphaArray(new double[]{1.0}));

			//.addGrid(forestClassifier, RandomForestClassifier.MAX_DEPTH, new Integer[] {6});
			//.addGrid(nb, NaiveBayes.SMOOTHING, new Double[] {1.0,0.1,0.01});
			GridSearchCV gridSearchCV2 = new GridSearchCV()
					.setEstimator(pipeline2)
					.setTuningEvaluator(tuningEvaluator)
					.setNumFolds(5)
					.setParamGrid(paramGridRF);
			gridSearchCV2.fit(trainData);


		}
	}

	@Test
	public void procData() throws Exception {
		String filePath = "/home/hotsun/workspace/data/blgs_app.csv";
		BufferedReader br = new BufferedReader(new FileReader(filePath));
		String line;
		double auc = 0;
		double acc = 0;
		double F1 = 0;
		double recall = 0;
		while ((line = br.readLine()) != null) {
			String[] conts = line.split(" ");
			auc += Double.parseDouble(conts[0]);
			acc += Double.parseDouble(conts[1]);
			F1 += Double.parseDouble(conts[2]);
			recall += Double.parseDouble(conts[3]);
		}
		System.out.println("auc : " + auc/500);
		System.out.println("acc: " + acc/500);
		System.out.println("F1 : " + F1/500);
		System.out.println("recall : " + recall/500);
	}
}
