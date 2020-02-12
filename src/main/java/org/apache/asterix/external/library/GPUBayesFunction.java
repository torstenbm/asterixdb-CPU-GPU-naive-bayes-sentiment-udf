/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.asterix.external.library;

import java.util.Timer;
import java.util.concurrent.Callable;

import org.apache.asterix.external.api.IExternalScalarFunction;
import org.apache.asterix.external.api.IFunctionHelper;
import org.apache.asterix.external.library.classifier.CPUUDF;
import org.apache.asterix.external.library.classifier.GPUUDF;
import org.apache.asterix.external.library.classifier.bayes.BayesClassifier;
import org.apache.asterix.external.library.classifier.example.getDevice;
import org.apache.asterix.external.library.java.JTypeTag;
import org.apache.asterix.external.library.java.base.JLong;
import org.apache.asterix.external.library.java.base.JRecord;
import org.apache.asterix.external.library.java.base.JString;

public class GPUBayesFunction implements IExternalScalarFunction {

    private JString sentiment;
    public GPUUDF BayesClasifier;
    public getDevice gd;

    private long startTime, stopTime, batchTime;
    private Integer classifiedInBatch = 0;
    private boolean isNotDone = true;
    private Integer seconds;

    @Override
    public void deinitialize() {}

    @Override
    public void evaluate(IFunctionHelper functionHelper) throws Exception {
        // Read input record
        JRecord inputRecord = (JRecord) functionHelper.getArgument(0);

        JLong id = (JLong) inputRecord.getValueByName("id");
        JString text = (JString) inputRecord.getValueByName("text");

        // Populate result record
        JRecord result = (JRecord) functionHelper.getResultObject();
        result.setField("id", id);
        result.setField("text", text);

        if (id.getValue() == 0){
            startTime = System.nanoTime();
        }
        if (System.nanoTime() - this.batchTime > 1000000000 && this.isNotDone){
            logClassifiedInBatchString();
            this.batchTime = System.nanoTime();
            this.seconds++;
        }

        sentiment.setValue(this.BayesClasifier.classify(text));
        result.setField("Sentiment", sentiment);
        functionHelper.setResult(result);
        
        if (id.getValue() == 999999){
            stopTime = System.nanoTime();
            Long finalTime = stopTime-startTime;
            System.out.println("Total classifying time: " + finalTime.toString());
            this.isNotDone = false;
        }
        this.classifiedInBatch++;
    }

    public void logClassifiedInBatchString(){
        System.out.println("Classified during second " + this.seconds.toString() + ": " + this.classifiedInBatch.toString());
        this.classifiedInBatch = 0;
    }

    @Override
    public void initialize(IFunctionHelper functionHelper) throws Exception{
        sentiment = new JString("");
        this.gd = new getDevice();
        this.BayesClasifier = new GPUUDF();
        System.out.println("Initialization of Bayes Classifier started");
        this.BayesClasifier.trainModel();
        System.out.println("Bayes Classifier Initialized");
        this.batchTime = System.nanoTime();
        this.seconds = 0;
        // this.timer = new Timer();
        // timer.schedule(new LogTask( 
        //     new Callable<String>() {  
        //         public String call(){ 
        //             return getClassifiedInBatchString(); 
        //         } 
        //     }
        // ), 0, 1000);
        // System.out.println("I got out of initialize now");
    }
}