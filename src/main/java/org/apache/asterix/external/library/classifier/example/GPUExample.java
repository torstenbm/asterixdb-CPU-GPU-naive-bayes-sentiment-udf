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
package org.apache.asterix.external.library.classifier.example;

import java.util.Arrays;
import com.opencsv.CSVReader;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

import org.apache.log4j.helpers.SyslogWriter;

import org.apache.asterix.external.library.classifier.bayes.CPUBayesClassifier;
import org.apache.asterix.external.library.classifier.bayes.BayesClassifier;
import org.apache.asterix.external.library.classifier.bayes.FeatureGenerator;
import org.apache.asterix.external.library.classifier.bayes.GPUBayesClassifier;
import org.apache.asterix.external.library.classifier.bayes.GPUIntBayesClassifier;
import org.apache.asterix.external.library.classifier.bayes.GPUOtherIntBayesClassifier;
import org.apache.asterix.external.library.classifier.Classifier;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.Math;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class GPUExample {

    public static Reader getReader(String relativePath) throws UnsupportedEncodingException, FileNotFoundException {
        return new InputStreamReader(new FileInputStream(relativePath), "UTF-8");
    }

    public static void main(String[] args) throws InterruptedException {

        /*
         * Create a new classifier instance. The context features are Strings and the
         * context will be classified with a String according to the featureset of the
         * context.
         */
        final GPUBayesClassifier<char[], String> bayes = new GPUBayesClassifier<char[], String>();
        /*
         * Please note, that this particular classifier implementation will "forget"
         * learned classifications after a few learning sessions. The number of learning
         * sessions it will record can be set as follows:
         */
        bayes.setMemoryCapacity(4000000);

        /*
         * The classifier can learn from classifications that are handed over to the
         * learn methods. Imagine a tokenized text as follows. The tokens are the text's
         * features. The category of the text will either be positive or negative.
         */

        String testingCsvFile = "/Users/torsten/Java-Naive-Bayes-Classifier/example/testdata.manual.csv";
        String trainingCsvFile = "/Users/torsten/aparapi-examples/src/main/java/com/aparapi/examples/classifier/example/training.shuffled.csv";
        BufferedReader br = null;
        String cvsSplitBy = ",";
        long startTime, endTime;
        FeatureGenerator FeatureGenerator = new FeatureGenerator();
        int trainingTotal = 0, learnedPositives = 0, learnedNegatives = 0;
        startTime = System.nanoTime();
        try {

            CsvParserSettings settings = new CsvParserSettings();
            CsvParser parser = new CsvParser(settings);
            Reader reader = getReader(trainingCsvFile);
            parser.beginParsing(reader);

            String[] tweet;
            while ((tweet = parser.parseNext()) != null) {	
                trainingTotal++;
                if (trainingTotal > 10000){
                    break;
                }
                tweet[0] = tweet[0].replaceAll("\"", "");
                tweet[5] = tweet[5].replaceAll("\"", "");

				if (tweet[0].equals("4")){
					// System.out.println("Learned positive , tweet=" + tweet[5] + "]");
                    bayes.learn("positive", FeatureGenerator.TweetToFeaturesChararray(tweet[5]));
                    learnedPositives++;
                } else if (tweet[0].equals("0")){
					// System.out.println("Learned negative , tweet=" + tweet[5] + "]");
                    bayes.learn("negative", FeatureGenerator.TweetToFeaturesChararray(tweet[5]));
                    learnedNegatives++;
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
        bayes.populateVectors();

        System.out.println("Training completed in " + String.valueOf((endTime-startTime)/Math.pow(10, 9)) + " seconds");
        System.out.println("Learned positives: " + learnedPositives);
        System.out.println("Learned negatives: " + learnedNegatives);
        System.out.println("Total tweets learned from: "+trainingTotal);
        System.out.println("Total features: " + bayes.totalFeatureCount.size());
        System.out.println("Total chars in features: " + bayes.featureVector.length);
        System.out.println("Total categories: " + bayes.featureCountPerCategory.size());
        // char[] testarray = new char[100];
        // System.arraycopy(bayes.featureVector, 0, testarray, 0, 100);
        // System.out.println(new String(testarray));
        // char[][] testarray2 = new char[10][];
        // System.arraycopy(bayes.unFlattenedFeatureVector, 0, testarray2, 0, 10);
        // System.out.println(Arrays.deepToString(testarray2));
        // System.out.println(bayes.featureIndexVector[0]);
        // System.out.println(bayes.featureIndexVector[1]);
        // System.out.println(bayes.featureIndexVector[2]);
        // System.out.println(bayes.featureIndexVector[3]);

        /*
         * Now that the classifier has "learned" two classifications, it will
         * be able to classify similar sentences. The classify method returns
         * a Classification Object, that contains the given featureset,
         * classification probability and resulting category.
         */

        String category; 
        int rights = 0, wrongs = 0;
        startTime = System.nanoTime();
        int totals = 0;

        try {
            CsvParserSettings settings = new CsvParserSettings();
            CsvParser parser = new CsvParser(settings);
            Reader reader = getReader(trainingCsvFile);
            parser.beginParsing(reader);

            String[] tweet;


            while ((tweet = parser.parseNext()) != null) {	
                totals++;
                if (totals <= 10000){
                    continue;
                }
                if (totals > 10010){
                    break;
                }

                tweet[0] = tweet[0].replaceAll("\"", "");
                tweet[5] = tweet[5].replaceAll("\"", "");

                if (tweet[0].equals("2")){
                    continue;
                }
                category = bayes.classify(FeatureGenerator.TweetToFeaturesChararray(tweet[5])).getCategory();
                
                if ( (category.equals("negative") && tweet[0].equals("0")) || (category.equals("positive") && tweet[0].equals("4")) ){
                    rights++;
                } else {
                    // System.out.println(String.format("Predicted %s but was %s", category, tweet[0]));
                    wrongs++;
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

        System.out.println("Classification completed in " + String.valueOf((endTime-startTime)/Math.pow(10, 9)) + " seconds");
        System.out.println(String.format("With %d correct predictions and %d wrong", rights, wrongs));
        System.out.println(totals);
        /*
         * The BayesClassifier extends the abstract Classifier and provides
         * detailed classification results that can be retrieved by calling
         * the classifyDetailed Method.
         *
         * The classification with the highest probability is the resulting
         * classification. The returned List will look like this.
         * [
         *   Classification [
         *     category=negative,
         *     probability=0.0078125,
         *     featureset=[today, is, a, sunny, day]
         *   ],
         *   Classification [
         *     category=positive,
         *     probability=0.0234375,
         *     featureset=[today, is, a, sunny, day]
         *   ]
         * ]
         */
        // System.out.println(((BayesClassifier<String, String>) bayes).classifyDetailed(
        //         Arrays.asList(unknownText2)));

    }
}
