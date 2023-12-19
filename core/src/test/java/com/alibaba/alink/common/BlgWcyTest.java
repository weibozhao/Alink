package com.alibaba.alink.common;

import com.alibaba.alink.bit.DetailMerge;
import com.alibaba.alink.operator.batch.BatchOperator;
import com.alibaba.alink.operator.batch.dataproc.SplitBatchOp;
import com.alibaba.alink.operator.batch.sink.AkSinkBatchOp;
import com.alibaba.alink.operator.batch.sink.CsvSinkBatchOp;
import com.alibaba.alink.operator.batch.source.AkSourceBatchOp;
import com.alibaba.alink.operator.batch.source.CsvSourceBatchOp;
import com.alibaba.alink.pipeline.Pipeline;
import com.alibaba.alink.pipeline.PipelineModel;
import com.alibaba.alink.pipeline.classification.*;
import com.alibaba.alink.pipeline.dataproc.StandardScaler;
import com.alibaba.alink.pipeline.tuning.BinaryClassificationTuningEvaluator;
import com.alibaba.alink.pipeline.tuning.GridSearchCV;
import com.alibaba.alink.pipeline.tuning.ParamGrid;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.FileReader;

public class BlgWcyTest {

    @Test
    public void testGridSearch() throws Exception {
        AlinkGlobalConfiguration.setPrintProcessInfo(true);
        BatchOperator.setParallelism(1);

        String path = "/home/weibo/workspace/blg/wcy_data.csv";

        BatchOperator<?> data = new CsvSourceBatchOp().setFilePath(path).setFieldDelimiter(" ").setSchemaStr(
                "label int, f1 double,  f2 double,  f3 double, f4 double");

        for (int i = 0; i < 10; ++i) {
            System.out.println("Round: " + i);
            data = data.shuffle();

            data.link(new AkSinkBatchOp().setOverwriteSink(true).setFilePath("/tmp/tmp_trian.ak"));
            BatchOperator.execute();

            BatchOperator<?> trainData = new AkSourceBatchOp().setFilePath("/tmp/tmp_trian.ak");
            String[] numCols = new String[]{"f1", "f2", "f3", "f4"};
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
                    .setTuningBinaryClassMetric("accuracy"); // "F1", "AUC", "recall"

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
                    .setStepMode(true)
                    .add(standardScaler)
                    .add(fmClassifier)
                    .add(gbdt)
                    .add(lr)
                    .add(svm)
                    .add(nb)
                    .add(forestClassifier1)
                    .add(new DetailMerge()
                            .setReservedCols("label").setSelectedCols(
                            //, "detail_RF"
                            //, "detail_gbdt"
                             "detail_fm"
                            //, "detail_lr"
                            , "detail_nb"
                            //, "detail_svm"
                    ).setAlphaArray(new double[]{0.5, 0.5}));

            ParamGrid paramGridRF = new ParamGrid();
//                    .addGrid(forestClassifier, RandomForestClassifier.MAX_DEPTH, new Integer[]{6})
//                    .addGrid(fmClassifier, FmClassifier.LEARN_RATE, new Double[]{0.1, 0.01});
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
                    .add(new DetailMerge()
                            .setReservedCols("label").setSelectedCols("detail_RF"
                            //, "detail_gbdt"
                            //, "detail_fm"
                            //, "detail_lr"
                            //, "detail_nb"
                            //, "detail_svm"
                    ).setAlphaArray(new double[]{1.0}));

            //.addGrid(forestClassifier, RandomForestClassifier.MAX_DEPTH, new Integer[] {6});
            //.addGrid(nb, NaiveBayes.SMOOTHING, new Double[] {1.0,0.1,0.01});
//            GridSearchCV gridSearchCV1 = new GridSearchCV()
//                    .setEstimator(pipeline1)
//                    .setTuningEvaluator(tuningEvaluator)
//                    .setNumFolds(5)
//                    .setParamGrid(paramGridRF);
//            gridSearchCV1.fit(trainData);


            Pipeline pipeline2 = new Pipeline()
                    .add(standardScaler)
                    //.add(fmClassifier)
                    .add(gbdt)
                    .add(lr)
                    // .add(svm)
                    //.add(nb)
                    .add(forestClassifier1)
                    .add(new DetailMerge()
                            .setReservedCols("label").setSelectedCols(
                                    "detail_RF"
                            , "detail_gbdt"
                            //, "detail_fm"
                            , "detail_lr"
                            //, "detail_nb"
                            //, "detail_svm"
                    ).setAlphaArray(new double[]{0.5, 0.2, 0.3}));

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
    public void testGenerateData() throws Exception {
        AlinkGlobalConfiguration.setPrintProcessInfo(true);
        BatchOperator.setParallelism(1);

        String path = "/home/weibo/workspace/blg/wcy_data.csv";
        String basePath = "/home/weibo/workspace/blg/sample_";
        BatchOperator<?> data = new CsvSourceBatchOp().setFilePath(path).setFieldDelimiter(" ").setSchemaStr(
                "label int, f1 double,  f2 double,  f3 double, f4 double");

        for (int i = 0; i < 100; ++i) {
            System.out.println("Round: " + i);
            data = data.shuffle();

            data.link(new AkSinkBatchOp().setOverwriteSink(true).setFilePath("/tmp/tmp_trian.ak"));
            BatchOperator.execute();

            BatchOperator<?> trainData = new AkSourceBatchOp().setFilePath("/tmp/tmp_trian.ak").link(new SplitBatchOp(0.8));
            String[] numCols = new String[]{"f1", "f2", "f3", "f4"};
            StandardScaler standardScaler = new StandardScaler().setSelectedCols(numCols);

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
                    .setPredictionCol("pred")
                    .setPredictionDetailCol("detail_RF");

            Pipeline pipeline = new Pipeline()
                    .setStepMode(true)
                    .setStepMode(true)
                    .add(standardScaler)
                    .add(fmClassifier)
                    .add(lr)
                    .add(svm)
                    .add(forestClassifier);

            pipeline.fit(trainData).transform(trainData.getSideOutput(0)).link(new CsvSinkBatchOp().setFilePath(basePath + i));
            BatchOperator.execute();
        }
    }

    @Test
    public void makeAkModel() throws Exception {
        AlinkGlobalConfiguration.setPrintProcessInfo(true);
        BatchOperator.setParallelism(1);

        String path = "/home/weibo/workspace/blg/wcy_data.csv";

        BatchOperator<?> data = new CsvSourceBatchOp().setFilePath(path).setFieldDelimiter(" ").setSchemaStr(
                "label int, f1 double,  f2 double,  f3 double, f4 double")
                .select("f1,f2,f3,f4,label");
            data = data.shuffle();

            data.link(new AkSinkBatchOp().setOverwriteSink(true).setFilePath("/tmp/tmp_trian.ak"));
            BatchOperator.execute();

            BatchOperator<?> trainData = new AkSourceBatchOp().setFilePath("/tmp/tmp_trian.ak");
            String[] numCols = new String[]{"f1", "f2", "f3", "f4"};
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
                    .setTuningBinaryClassMetric("accuracy"); // "F1", "AUC", "recall"

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
                    .add(nb)
                    //.add(forestClassifier1)
                    .add(new DetailMerge()
                            .setReservedCols()
                            .setSelectedCols(
                                    //, "detail_RF"
                            //, "detail_gbdt"
                            "detail_fm"
                            //, "detail_lr"
                            , "detail_nb"
                            //, "detail_svm"
                    ).setAlphaArray(new double[]{0.5, 0.5}));

            PipelineModel model = pipeline.fit(trainData);
            model.transform(trainData).print();
            model.save().link(new AkSinkBatchOp().setFilePath("/home/weibo/workspace/blg/wcy_model.ak").setOverwriteSink(true));
            BatchOperator.execute();
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
        System.out.println("auc : " + auc / 500);
        System.out.println("acc: " + acc / 500);
        System.out.println("F1 : " + F1 / 500);
        System.out.println("recall : " + recall / 500);
    }
}
