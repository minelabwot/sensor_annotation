package com.yyn.dao;

import com.yyn.util.RDFReasoning;
import org.apache.jena.query.Dataset;


/**
 * Created by yangyunong on 2017/5/10.
 */
public class DatasetContainer {

    private Dataset dataset;

//    public void init() {
//
//        String tdbRoot = ("/WEB-INF/RDF_Database/");
//        Dataset dataset = RDFReasoning.getDataset(tdbRoot, "sensor_annotation", "Wot.owl");
//        setDataset(dataset);
//        System.out.println("Start up Listener execute, dataset  has been set to context");
//    }

    public Dataset getDataset() {
        return dataset;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }
}
