/**
 * Copyright (c) 2016 - 2018 Syncleus, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.asterix.external.library.classifier;

import java.util.Arrays;
import java.util.Dictionary;

import com.opencsv.CSVReader;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import org.apache.asterix.external.library.classifier.bayes.CPUBayesClassifier;
import org.apache.asterix.external.library.classifier.bayes.BayesClassifier;
import org.apache.asterix.external.library.classifier.bayes.FeatureGenerator;
import org.apache.asterix.external.library.classifier.bayes.GPUBayesClassifier;
import org.apache.asterix.external.library.classifier.example.getDevice;
import org.apache.asterix.external.library.java.JTypeTag;
import org.apache.asterix.external.library.java.base.JString;
import org.apache.asterix.external.library.classifier.Classifier;
import org.apache.asterix.external.api.IFunctionHelper;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.Math;
import java.nio.file.Files;

public class GPUUDF {

    private GPUBayesClassifier<char[], String> bayes;
    private FeatureGenerator featureGenerator;

    public GPUUDF() {
        this.bayes = new GPUBayesClassifier<char[], String>();
        this.bayes.setMemoryCapacity(4000000);
        this.featureGenerator = new FeatureGenerator();
    }

    public static Reader getReader(String relativePath) throws UnsupportedEncodingException, FileNotFoundException {
        return new InputStreamReader(new FileInputStream(relativePath), "UTF-8");
    }

    
    public static void main(String[] args){
        GPUUDF test = new GPUUDF();
        test.trainModel();
        System.out.println(test.classify(new JString("I hate my life and want to die")));
    }

    public void trainModel(){
        String trainingCsvFile = "/Users/torsten/Java-Naive-Bayes-Classifier/example/training.shuffled.csv";
        BufferedReader br = null;
        long startTime, endTime;
        FeatureGenerator FeatureGenerator = new FeatureGenerator();
        int trainingTotal = 0;
        startTime = System.nanoTime();
        try {
            CsvParserSettings settings = new CsvParserSettings();
            CsvParser parser = new CsvParser(settings);
            Reader reader = getReader(trainingCsvFile);
            parser.beginParsing(reader);

            String[] tweet;
            while ((tweet = parser.parseNext()) != null) {
                trainingTotal++;
                if (trainingTotal > 10000) {
                    break;
                }
                tweet[0] = tweet[0].replaceAll("\"", "");
                tweet[5] = tweet[5].replaceAll("\"", "");

                if (tweet[0].equals("4")) {
                    // System.out.println("Learned positive , tweet=" + tweet[5] + "]");
                    this.bayes.learn("positive", FeatureGenerator.TweetToFeaturesChararray(tweet[5]));
                } else if (tweet[0].equals("0")) {
                    // System.out.println("Learned negative , tweet=" + tweet[5] + "]");
                    this.bayes.learn("negative", FeatureGenerator.TweetToFeaturesChararray(tweet[5]));
                }
            }
            parser.stopParsing();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        endTime = System.nanoTime();
        this.bayes.populateVectors();
        System.out.println("Training completed in " + String.valueOf((endTime - startTime) / Math.pow(10, 9)) + " seconds");
    }

    public String classify(JString asterixTweet) {
        String at = asterixTweet.getValue();
        return this.bayes.classify(FeatureGenerator.TweetToFeaturesChararray(at)).getCategory();
    }
}
