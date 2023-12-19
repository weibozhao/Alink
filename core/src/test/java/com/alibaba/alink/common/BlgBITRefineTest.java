package com.alibaba.alink.common;

import com.alibaba.alink.bit.DetailMerge;
import com.alibaba.alink.bit.DetailSelect;
import com.alibaba.alink.common.utils.JsonConverter;
import com.alibaba.alink.operator.batch.BatchOperator;
import com.alibaba.alink.operator.batch.dataproc.SplitBatchOp;
import com.alibaba.alink.operator.batch.sink.AkSinkBatchOp;
import com.alibaba.alink.operator.batch.sink.CsvSinkBatchOp;
import com.alibaba.alink.operator.batch.source.AkSourceBatchOp;
import com.alibaba.alink.operator.batch.source.CsvSourceBatchOp;
import com.alibaba.alink.operator.batch.utils.DataSetConversionUtil;
import com.alibaba.alink.pipeline.Pipeline;
import com.alibaba.alink.pipeline.classification.*;
import com.alibaba.alink.pipeline.dataproc.StandardScaler;
import com.alibaba.alink.pipeline.tuning.BinaryClassificationTuningEvaluator;
import com.alibaba.alink.pipeline.tuning.GridSearchCV;
import com.alibaba.alink.pipeline.tuning.ParamGrid;
import org.apache.flink.api.common.functions.GroupReduceFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.type.TypeReference;
import org.apache.flink.table.api.Table;
import org.apache.flink.types.Row;
import org.apache.flink.util.Collector;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

public class BlgBITRefineTest {


    @Test
    public void testGenerateData() throws Exception {
        AlinkGlobalConfiguration.setPrintProcessInfo(true);
        BatchOperator.setParallelism(1);

        String basePath = "/home/weibo/workspace/blg/sample_";
        String path = "/home/weibo/workspace/data/blgs_app.csv";

        BatchOperator<?> data = new CsvSourceBatchOp().setFilePath(path).setFieldDelimiter(",").setSchemaStr(
                "f0 string, f1 double,  f2 double,  f3 double, "
                        + " f4 double,  f5 double,  f6 double,  f7 double,  f8 double, "
                        + " f9 double,  f10 double,  f11 double,  f12 double,  f13 double, "
                        + " f14 double,  f15 double,  f16 double,  f17 double,  f18 double, "
                        + " f19 double,  f20 double, f21 double, label int");


        for (int i = 0; i < 500; ++i) {
            System.out.println("Round: " + i);
            data = data.shuffle();

            data.link(new AkSinkBatchOp().setOverwriteSink(true).setFilePath("/tmp/tmp_trian.ak"));
            BatchOperator.execute();

            BatchOperator<?> trainData = new AkSourceBatchOp().setFilePath("/tmp/tmp_trian.ak").link(new SplitBatchOp(0.8));
            String[] numCols = new String[]{"f1", "f2", "f3", "f4","f5", "f6", "f7", "f8","f9",
                    "f10", "f11", "f12","f13", "f14", "f15", "f16","f17", "f18", "f19", "f20","f21"};
            StandardScaler standardScaler = new StandardScaler().setSelectedCols(numCols);

            FmClassifier fmClassifier = new FmClassifier()
                    .setFeatureCols(numCols)
                    .setLearnRate(0.004)
                    .setLabelCol("label").setNumFactor(10)
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

            RandomForestClassifier forestClassifier = new RandomForestClassifier()
                    .setFeatureCols(numCols)
                    .setLabelCol("label")
                    .setPredictionCol("pred")
                    .setPredictionDetailCol("detail_RF");

            Pipeline pipeline = new Pipeline()
                    .setStepMode(true)
                    .add(forestClassifier)
                    .add(fmClassifier)
                    .add(lr)
                    .add(standardScaler);

            pipeline.fit(trainData).transform(trainData.getSideOutput(0)).link(new CsvSinkBatchOp().setOverwriteSink(true).setFilePath(basePath + i));
            BatchOperator.execute();
        }
    }

    @Test
    public void testGenerateTypeSample() throws Exception {
        AlinkGlobalConfiguration.setPrintProcessInfo(true);
        BatchOperator.setParallelism(1);

        String path = "/home/weibo/workspace/blg/sample_all";

        BatchOperator<?> data = new CsvSourceBatchOp().setFilePath(path).setFieldDelimiter(",").setSchemaStr(
                "f0 string, f1 double,  f2 double,  f3 double, "
                        + " f4 double,  f5 double,  f6 double,  f7 double,  f8 double, "
                        + " f9 double,  f10 double,  f11 double,  f12 double,  f13 double, "
                        + " f14 double,  f15 double,  f16 double,  f17 double,  f18 double, "
                        + " f19 double,  f20 double, f21 double, label int, pred int, detail_rf string, detail_fm string, detail_lr string");

        DataSet<Row> dataMid = data.getDataSet().groupBy(new KeySelector<Row, String>() {
                    @Override
                    public String getKey(Row value) throws Exception {
                        return (String) value.getField(0);
                    }
                })
                .reduceGroup(new GroupReduceFunction<Row, Row>() {
                    @Override
                    public void reduce(Iterable<Row> values, Collector<Row> out) throws Exception {
                        int cnt = 0;
                        Row ret = null;
                        for (Row row : values) {
                            if (ret == null) {
                                ret = row;
                            } else {
                                String jsonrf = (String) row.getField(24);
                                String jsonfm = (String) row.getField(25);
                                String jsonlr = (String) row.getField(26);
                                ret.setField(24, mergeDetail(ret.getFieldAs(24), jsonrf));
                                ret.setField(25, mergeDetail(ret.getFieldAs(25), jsonfm));
                                ret.setField(26, mergeDetail(ret.getFieldAs(26), jsonlr));
                            }
                            cnt++;
                        }

                        ret.setField(24, scale(ret.getFieldAs(24), cnt));
                        ret.setField(25, scale(ret.getFieldAs(25), cnt));
                        ret.setField(26, scale(ret.getFieldAs(26), cnt));
                        out.collect(ret);

                    }
                }).map(new MapFunction<Row, Row>() {
                    @Override
                    public Row map(Row row) throws Exception {
                        int label = row.getFieldAs(22);
                        String jsonrf = (String) row.getField(24);
                        String jsonfm = (String) row.getField(25);
                        String jsonlr = (String) row.getField(26);
                        Map<String, Object> detail_fm = JsonConverter.fromJson(jsonfm,
                                new TypeReference<HashMap<String, Object>>() {
                                }.getType());

                        Map<String, Object> detail_rf = JsonConverter.fromJson(jsonrf,
                                new TypeReference<HashMap<String, Object>>() {
                                }.getType());

                        Map<String, Object> detail_lr = JsonConverter.fromJson(jsonlr,
                                new TypeReference<HashMap<String, Object>>() {
                                }.getType());
                        int type;


                        double rf = Double.parseDouble(detail_rf.get("" + label).toString());
                        double fm = Double.parseDouble(detail_fm.get("" + label).toString());
                        double lr = Double.parseDouble(detail_lr.get("" + label).toString());
                        if (rf >= fm && rf>= lr) {
                            type = 0;
                        } else if  (fm >= rf && fm>= lr){
                            type = 1;
                        } else {
                            type = 2;
                        }
                        row.setField(23, type);
                        return row;
                    }
                });
        Table table = DataSetConversionUtil.toTable(data.getMLEnvironmentId(), dataMid, data.getSchema());
        BatchOperator batchOperator = BatchOperator.fromTable(table);
        batchOperator.select("f0, f1,  f2,  f3, "
                        + " f4,  f5,  f6,  f7,  f8, "
                        + " f9,  f10,  f11,  f12,  f13, "
                        + " f14,  f15,  f16,  f17,  f18, "
                        + " f19,  f20, f21, label, pred").link(new CsvSinkBatchOp().setOverwriteSink(true).setFilePath("/home/weibo/workspace/blg/data_with_type"));

        BatchOperator.execute();

    }

    private static String scale(String json1, int cnt) {
        Map<String, Object> detail_1 = JsonConverter.fromJson(json1,
                new TypeReference<HashMap<String, Object>>() {
                }.getType());
        for (String s : detail_1.keySet()) {
            double val1 = Double.parseDouble(detail_1.get(s).toString());
            detail_1.put(s, val1 / cnt);
        }
        return JsonConverter.toJson(detail_1);
    }

    private static String mergeDetail(String json1, String json2) {
        Map<String, Object> detail_1 = JsonConverter.fromJson(json1,
                new TypeReference<HashMap<String, Object>>() {
                }.getType());
        Map<String, Object> detail_2 = JsonConverter.fromJson(json2,
                new TypeReference<HashMap<String, Object>>() {
                }.getType());
        for (String s : detail_1.keySet()) {
            double val1 = Double.parseDouble(detail_1.get(s).toString());
            double val2 = Double.parseDouble(detail_2.get(s).toString());
            detail_1.put(s, val1 + val2);
        }
        return JsonConverter.toJson(detail_1);
    }

    @Test
    public void testGridSearch() throws Exception {
        AlinkGlobalConfiguration.setPrintProcessInfo(true);
        BatchOperator.setParallelism(1);

        String path = "/home/weibo/workspace/blg/data_with_type";

        BatchOperator<?> data = new CsvSourceBatchOp().setFilePath(path).setFieldDelimiter(",").setSchemaStr(
                "f0 string, f1 double,  f2 double,  f3 double, "
                        + " f4 double,  f5 double,  f6 double,  f7 double,  f8 double, "
                        + " f9 double,  f10 double,  f11 double,  f12 double,  f13 double, "
                        + " f14 double,  f15 double,  f16 double,  f17 double,  f18 double, "
                        + " f19 double,  f20 double, f21 double, label int, type int");

        for (int i = 0; i < 3; ++i) {
            System.out.println("Round: " + i);
            data = data.shuffle();

            data.link(new AkSinkBatchOp().setOverwriteSink(true).setFilePath("/tmp/tmp_trian.ak"));
            BatchOperator.execute();

            BatchOperator<?> trainData = new AkSourceBatchOp().setFilePath("/tmp/tmp_trian.ak");
            String[] numCols = new String[]{
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
                    // .add(gbdt)
                    .add(lr)
                    //.add(svm)
                    //.add(nb)
                    .add(forestClassifier1)
                    .add(new DetailSelect().setSelectedCols("label", "type"
                            , "detail_RF"
                            , "detail_fm"
                            , "detail_LR"
//                            , "detail_nb"
//                            , "detail_lr"
//                            , "detail_svm"
                    ).setAlphaArray(new double[]{0.3, 0.4, 0.3}));

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
                    .add(fmClassifier)
                    //.add(gbdt)
                    .add(lr)
                    //.add(svm)
                    //.add(nb)
                    .add(forestClassifier1)
                    .add(new DetailMerge()
                            .setReservedCols("label")
                            .setSelectedCols(
                             "detail_RF"
                            //, "detail_gbdt"
                            , "detail_fm"
                            , "detail_lr"
                            //, "detail_nb"
                            //, "detail_svm"
                    ).setAlphaArray(new double[]{0.5, 0.3, 0.2}));

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
                    .add(forestClassifier1)
                    .add(new DetailMerge()
                            .setReservedCols("label")
                            .setSelectedCols(
                                    "detail_RF"
                            //, "detail_gbdt"
                            , "detail_fm"
                            //, "detail_lr"
                            //, "detail_nb"
                            //, "detail_svm"
                    ).setAlphaArray(new double[]{0.5, 0.5}));

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
        System.out.println("auc : " + auc / 500);
        System.out.println("acc: " + acc / 500);
        System.out.println("F1 : " + F1 / 500);
        System.out.println("recall : " + recall / 500);
    }
}
