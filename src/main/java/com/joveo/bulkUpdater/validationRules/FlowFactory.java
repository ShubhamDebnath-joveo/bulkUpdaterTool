package com.joveo.bulkUpdater.validationRules;

import com.joveo.bulkUpdater.CliUtils;
import com.joveo.eqrtestsdk.core.entities.Driver;

import java.util.Set;

public abstract class FlowFactory {

    public static Flow getFLow(Set<String> headers, Driver driver){
        if(CliUtils.hasOption(CliUtils.ADD_JOBGROUP)){
            return new CreateFlow(headers, driver);
        }else{
            return new EditFlow(headers, driver);
        }
    }
}
