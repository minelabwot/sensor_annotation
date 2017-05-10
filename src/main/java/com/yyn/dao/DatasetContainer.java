package com.yyn.dao;

import com.yyn.config.Config;
import com.yyn.util.RDFReasoning;
import org.apache.jena.query.Dataset;


/**
 * Created by yangyunong on 2017/5/10.
 */
public class DatasetContainer {

    private Dataset dataset;

    public void init() {
        Dataset dataset = RDFReasoning.getDataset(Config.TDB_ROOT, Config.TDB_DATABASE, Config.OWL_FILE_NAME);
        setDataset(dataset);
        System.out.println("TDB has been init into "+Config.TDB_ROOT+Config.TDB_DATABASE);
    }

    public Dataset getDataset() {
        return dataset;
    }

    private void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }
}
