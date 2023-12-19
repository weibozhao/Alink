package com.alibaba.alink;

import com.alibaba.alink.common.io.filesystem.FilePath;
import com.alibaba.alink.params.PipelineModelParams;
import com.alibaba.alink.pipeline.LocalPredictor;
import com.alibaba.alink.pipeline.ModelExporterUtils;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.ml.api.misc.param.Params;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.types.Row;

import static com.alibaba.alink.pipeline.ModelExporterUtils.deserializeMeta;

/**
 * Bit software beta.
 */
public class Main {
    public static void main(String[] args) throws Exception {

        String wcyPath = "/home/weibo/workspace/blg/wcy_model.ak";
        String dklPath = "/home/weibo/workspace/blg/dkl_model.ak";
//        String wcyPath = "./wcy_model.ak";//"/home/weibo/workspace/blg/wcy_model.ak";
//        String dklPath = "./dkl_model.ak";//"/home/weibo/workspace/blg/dkl_model.ak";
        System.out.println("**************************************************************");
        System.out.println("              Welcome to use bit.");
        System.out.println("**************************************************************");

        Tuple2<TableSchema, Row> wcySchemaAndMeta = ModelExporterUtils.loadMetaFromAkFile(
                new FilePath(wcyPath));
        Params wcyParams = deserializeMeta(wcySchemaAndMeta.f1, wcySchemaAndMeta.f0, 1).f1;
        String wcyInputDataSchema = wcyParams.get(PipelineModelParams.TRAINING_DATA_SCHEMA);

        LocalPredictor wcyPredictor = new LocalPredictor(wcyPath, wcyInputDataSchema);

        Tuple2<TableSchema, Row> dklSchemaAndMeta = ModelExporterUtils.loadMetaFromAkFile(
                new FilePath(dklPath));
        Params dklParams = deserializeMeta(dklSchemaAndMeta.f1, dklSchemaAndMeta.f0, 1).f1;
        String dklInputDataSchema = dklParams.get(PipelineModelParams.TRAINING_DATA_SCHEMA);

        LocalPredictor dklPredictor = new LocalPredictor(wcyPath, dklInputDataSchema);

        if (args.length == 1 && args[0].equals("--gui")) {
            Gui.runGui(new LocalPredictor[]{wcyPredictor, dklPredictor}, "--linux");
        } else   if (args.length == 2 && args[0].equals("--gui")) {
            Gui.runGui(new LocalPredictor[]{wcyPredictor, dklPredictor}, args[1]);
        } else {
            Terminal.runTerminal(new LocalPredictor[]{wcyPredictor, dklPredictor});
        }
    }
}
