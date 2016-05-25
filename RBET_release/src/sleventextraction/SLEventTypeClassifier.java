/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sleventextraction;

import AceJet.AceEvent;
import Jet.JetTest;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_problem;
import org.jdom2.JDOMException;
import static sleventextraction.SLAPFReader.ReadEvents;
import static sleventextraction.SLEventExtraction.BPGetNPs;
import static sleventextraction.SLEventExtraction.Lookup_All_wordvec;
import static sleventextraction.SLEventExtraction.overlap;
import static sleventextraction.SLEventExtraction.word2vec;
import slvectormodel.SLVectorModel;

/**
 *
 * @author v-lesha
 */
public class SLEventTypeClassifier {

    public static StanfordCoreNLP pipeline;

    svm_model WhichEventmodel = null, EventPruneModel = null;
    static String folder ;
    public PrintStream EventPruningNegtiveExampleRecorder;
    public PrintStream EventTypeRecorder = null;
    public Scanner EventTypeReader = null;

    static {
        if (SLEventExtraction.pipeline != null) {
            pipeline = SLEventExtraction.pipeline;
        } else {
            Properties props = new Properties();
            props.put("annotators", "tokenize, ssplit, pos, lemma,parse");
            pipeline = new StanfordCoreNLP(props);
        }
    }

    public SLEventTypeClassifier() {
        folder = JetTest.getConfigFile("Ace.EventModels.directory");
        try {
//            EventPruningNegtiveExampleRecorder = new PrintStream(new FileOutputStream(folder + "EventPruningNegtiveExample.txt"));
            WhichEventmodel = svm.svm_load_model(folder + "EventTypeModel.dat");
//            EventPruneModel = svm.svm_load_model(folder + "EventPruningModel.dat");
        } catch (IOException ex) {
            Logger.getLogger(SLEventTypeClassifier.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public SLEventTypeClassifier(int i) {
    }

//    String typefile="EventTypeLookUp.txt";
//    String typefile = "EventTypeLookUpPrune.txt";
//    String typefile="EventTypeLookUp_Ans.txt";
    String typefile = "EventTypeLookUp_train.txt";

    public AceEvent GetDirectAceEvent(String anchor, String sentence) {
//        if (!new File(folder + typefile).exists() || EventTypeRecorder != null) {
        if (true) {
            if (EventTypeRecorder == null) {
                try {
                    EventTypeRecorder = new PrintStream(new FileOutputStream(folder + typefile));
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(SLEventTypeClassifier.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            Annotation parsedsen = ParseSentence(sentence);
            LinkedList<Double> features = GetFeatures(anchor, parsedsen);
            if (features == null) {
                EventTypeRecorder.println("NULLfeature");
                return null;
            }
            svm_node[] x = new svm_node[features.size()];
            for (int j = 0; j < features.size(); j++) {
                x[j] = new svm_node();
                x[j].index = j + 1;
                x[j].value = features.get(j);
            }

            double d = svm.svm_predict(WhichEventmodel, x);
            String type = SLVectorModel.Event_types[(int) d];
            EventTypeRecorder.println(type);
            if (type.contains("NoEvent") || type.contains("Merge-Org") || type.contains("Declare-Bankruptcy")) {
                return null;
            }
            String[] split = type.split(":");
            return new AceEvent("classified", split[0], split[1]);
        } else {
            if (EventTypeReader == null) {
                try {
                    EventTypeReader = new Scanner(new FileInputStream(folder + typefile));
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(SLEventTypeClassifier.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            String type = EventTypeReader.next();
            if (type.contains("NoEvent") || type.equals("NULLfeature") || type.contains("Merge-Org") || type.contains("Declare-Bankruptcy")) {
                return null;
            }
            String[] split = type.split(":");
            return new AceEvent("id", split[0], split[1]);
        }
    }

    public void trainsvm() throws FileNotFoundException, JDOMException, IOException {
        LinkedList<LinkedList<Double>> X = new LinkedList<LinkedList<Double>>();
        LinkedList<Integer> Y = new LinkedList<Integer>();
        if (!new File(folder + "Traindata.dat").exists()) {

            String trainfilelist = "D:\\LDC2006D06\\LDC2006D06\\Data\\LDC2006T06_Original\\data\\English\\nw\\Trainfile.txt";
            Scanner trainlist = new Scanner(new FileInputStream(trainfilelist));
            while (trainlist.hasNext()) {
                String filename = trainlist.next();
                System.out.println(filename);
                File answer = new File("D:\\LDC2006D06\\LDC2006D06\\Data\\LDC2006T06_Original\\data\\English\\nw\\fp1\\" + filename + ".apf.xml");
                File docfile = new File("D:\\LDC2006D06\\LDC2006D06\\Data\\LDC2006T06_Original\\data\\English\\nw\\fp1\\" + filename + ".sgm");
                LinkedList<Event> events = ReadEvents(answer);
                LinkedList<String> sentences = SLCountParagraph.GetSentences(docfile, pipeline);
                LinkedList<String> NoEventSentences = new LinkedList<>();
                NoEventSentences.addAll(sentences);
                for (int i = 0; i < NoEventSentences.size(); i++) {
                    if (NoEventSentences.get(i).equals(".")) {
                        NoEventSentences.remove(i);
                        i--;
                    }
                }
                for (Event event : events) {
                    String sen = FindSentence_Of_Event(event, sentences, NoEventSentences);
                    if (sen == null) {
                        continue;
                    }
                    Annotation parsedsen = ParseSentence(sen);
                    LinkedList<Double> features = GetFeatures(event.trigger, parsedsen);
                    if (features == null) {
                        continue;
                    }
                    X.add(features);
                    Y.add(SLVectorModel.CheckPos(event.eventtype, SLVectorModel.Event_types));
                }

                LinkedList<String> SampledSentences = SLMath.Sample_M_from_N_(NoEventSentences, events.size() / 10);
                for (String negsen : SampledSentences) {
                    Annotation parsedsen = ParseSentence(negsen);
                    LinkedList<String> CandidateNegtiveTriggerList = GetCandidateTriggers(parsedsen);
                    if (CandidateNegtiveTriggerList.isEmpty()) {
                        continue;
                    }
                    String choosen_neg = CandidateNegtiveTriggerList.get((int) (Math.random() * 1000) % CandidateNegtiveTriggerList.size());
                    LinkedList<Double> neg_features = GetFeatures(choosen_neg, parsedsen);
                    if (neg_features == null) {
                        for (String s : CandidateNegtiveTriggerList) {
                            choosen_neg = s;
                            neg_features = GetFeatures(choosen_neg, parsedsen);
                            if (neg_features != null) {
                                break;
                            }
                        }
                        if (neg_features == null) {
                            continue;
                        }
                    }

                    X.add(neg_features);
                    Y.add(SLVectorModel.CheckPos("NoEvent", SLVectorModel.Event_types));
                }
            }
            assert X.size() == Y.size();
            WriteToFile(X, Y, folder + "Traindata.dat");
            WriteToGDBTFile(X, Y, folder + "GDBTTraindata.dat");
        } else {
            ReadFromFile(X, Y, folder + "Traindata.dat");
        }
        /**
         * The following is svm part
         */
        svm_problem prob = new svm_problem();
        prob.l = X.size();
        prob.y = new double[prob.l];

        svm_parameter param = new svm_parameter();

        // default values
        param.svm_type = svm_parameter.C_SVC;
        param.kernel_type = svm_parameter.POLY;
        param.degree = 2;
        param.gamma = 0.5;
        param.coef0 = 0;
        param.nu = 0.5;
        param.cache_size = 40;
        param.C = 10;
        param.eps = 1e-3;
        param.p = 0.1;
        param.shrinking = 0;
        param.probability = 0;
        param.nr_weight = 0;
        param.weight_label = new int[0];
        param.weight = new double[0];

        if (param.gamma == 0) {
            param.gamma = 0.5;
        }
        prob.x = new svm_node[prob.l][X.getFirst().size()];
        for (int i = 0; i < prob.l; i++) {
            for (int j = 0; j < X.getFirst().size(); j++) {
                prob.x[i][j] = new svm_node();
                prob.x[i][j].index = j + 1;
                prob.x[i][j].value = X.get(i).get(j);
            }
            prob.y[i] = Y.get(i);
        }
        // build model & classify
        svm_model model = svm.svm_train(prob, param);
        svm.svm_save_model(folder + "EventTypeModel.dat", model);
        int right = 0;
        for (int i = 0; i < prob.l; i++) {
            svm_node[] x = new svm_node[X.getFirst().size()];
            for (int j = 0; j < X.getFirst().size(); j++) {
                x[j] = new svm_node();
                x[j].index = j + 1;
                x[j].value = X.get(i).get(j);
            }
            double d = svm.svm_predict(model, x);
            if ((int) d == Y.get(i)) {
                right++;
            }
        }
        System.out.println("Training Precision: " + 1.0 * right / prob.l);
    }

    public static void main(String[] args) throws JDOMException, IOException {
        SLEventTypeClassifier sletc = new SLEventTypeClassifier(1);
//        sletc.trainsvm();
        sletc.testsvm();
//        sletc.trainEventPruningsvm();
    }

    private String FindSentence_Of_Event(Event event, LinkedList<String> sentences, LinkedList<String> NoEvent) {
        for (String s : sentences) {
            if (overlap(s, event.span) > 0.7 || overlap(event.span, s) > 0.7) {
                if (s.contains(event.trigger)) {
                    if (NoEvent.contains(s)) {
                        NoEvent.remove(s);
                    }
                    return s;
                }
            }
        }
        return null;
    }

    private String FindSentence_Of_Event(Event event, LinkedList<String> sentences) {
        for (String s : sentences) {
            if (overlap(s, event.span) > 0.7 || overlap(event.span, s) > 0.7) {
                if (s.contains(event.trigger)) {
                    return s;
                }
            }
        }
        return null;
    }

    public static Annotation ParseSentence(String sen) {
        Annotation document = new Annotation(sen);
        pipeline.annotate(document);
        return document;
    }

    public static IndexedWord ParseSentenceAndGetEntities(String anchor, LinkedList<SLEntity> entitylist, Annotation document, StanfordCoreNLP pipeline) {

        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);

        assert sentences.size() == 1;

        CoreMap cm = sentences.get(0);
        SemanticGraph dependencies = cm.get(SemanticGraphCoreAnnotations.CollapsedDependenciesAnnotation.class);
        List<SLEntity> le = BPGetNPs(dependencies);
        entitylist.addAll(le);

        return GetCorrespondingIndexedWord(anchor, cm);
    }

    public static IndexedWord GetCorrespondingIndexedWord(String anchor, CoreMap cm) {
        anchor = anchor.replaceAll("[.|,|\n|\"]", " ");
        String[] split = anchor.split("[ |']");
        IndexedWord iw = null;
        for (int i = split.length - 1; i >= 0; i--) {
            for (CoreLabel token : cm.get(TokensAnnotation.class)) {
                iw = new IndexedWord(token);

                if (split[i].contains(iw.word()) || iw.word().contains(split[i])) {
                    if (Math.abs(split[i].length() - iw.word().length()) <= 3) {
                        return iw;
                    } else if (iw.word().contains("-")) {
                        String[] split1 = iw.word().split("-");
                        boolean match = false;
                        for (int j = 0; j < split1.length; j++) {
                            if (split1[j].equals(split[i])) {
                                match = true;
                            }
                        }
                        if (match) {
                            return iw;
                        }
                    }
                }

                if (split[i].contains(iw.lemma())) {
                    if (Math.abs(split[i].length() - iw.lemma().length()) <= 2) {
                        return iw;
                    }
                }
            }
        }
        return null;
    }

    private void testsvm() throws IOException, JDOMException {
        LinkedList<LinkedList<Double>> X = new LinkedList<>();
        LinkedList<Integer> Y = new LinkedList<>();
        if (!new File(folder + "Testdata.dat").exists()) {
            PrintStream testRealdata = new PrintStream(new FileOutputStream(folder + "RealTestdata.txt"));
            String testfilelist = "D:\\LDC2006D06\\LDC2006D06\\Data\\LDC2006T06_Original\\data\\English\\nw\\Testfile.txt";
            Scanner testlist = new Scanner(new FileInputStream(testfilelist));
            while (testlist.hasNext()) {
                String filename = testlist.next();
                System.out.println(filename);
                File answer = new File("D:\\LDC2006D06\\LDC2006D06\\Data\\LDC2006T06_Original\\data\\English\\nw\\fp1\\" + filename.replace(".sgm", ".apf.xml"));
                File docfile = new File("D:\\LDC2006D06\\LDC2006D06\\Data\\LDC2006T06_Original\\data\\English\\nw\\fp1\\" + filename);
                LinkedList<Event> events = ReadEvents(answer);
                LinkedList<String> sentences = SLCountParagraph.GetSentences(docfile, pipeline);
                LinkedList<String> NoEventSentences = new LinkedList<>();
                NoEventSentences.addAll(sentences);
                for (int i = 0; i < NoEventSentences.size(); i++) {
                    if (NoEventSentences.get(i).equals(".")) {
                        NoEventSentences.remove(i);
                        i--;
                    }
                }
                for (Event event : events) {
                    String sen = FindSentence_Of_Event(event, sentences);
                    if (sen == null) {
                        continue;
                    }

                    Annotation parsedsen = ParseSentence(sen);
                    LinkedList<Double> features = GetFeatures(event.trigger, parsedsen);
                    if (features == null) {
                        continue;
                    }
                    testRealdata.println(event.trigger + "\t" + sen.replace("\n", " ") + "\t" + SLVectorModel.CheckPos(event.eventtype, SLVectorModel.Event_types));
                    X.add(features);
                    Y.add(SLVectorModel.CheckPos(event.eventtype, SLVectorModel.Event_types));
                }
                LinkedList<String> SampledSentences = SLMath.Sample_M_from_N_(NoEventSentences, events.size() / 10);
                for (String negsen : SampledSentences) {
                    Annotation parsedsen = ParseSentence(negsen);
                    LinkedList<String> CandidateNegtiveTriggerList = GetCandidateTriggers(parsedsen);
                    if (CandidateNegtiveTriggerList.isEmpty()) {
                        continue;
                    }
                    String choosen_neg = CandidateNegtiveTriggerList.get((int) (Math.random() * 1000) % CandidateNegtiveTriggerList.size());
                    LinkedList<Double> neg_features = GetFeatures(choosen_neg, parsedsen);
                    if (neg_features == null) {
                        for (String s : CandidateNegtiveTriggerList) {
                            choosen_neg = s;
                            neg_features = GetFeatures(choosen_neg, parsedsen);
                            if (neg_features != null) {
                                break;
                            }
                        }
                        if (neg_features == null) {
                            continue;
                        }
                    }

                    testRealdata.println(choosen_neg + "\t" + negsen.replace("\n", " ") + "\t" + SLVectorModel.CheckPos("NoEvent", SLVectorModel.Event_types));
                    X.add(neg_features);
                    Y.add(SLVectorModel.CheckPos("NoEvent", SLVectorModel.Event_types));
                }
            }
            assert X.size() == Y.size();
            WriteToFile(X, Y, folder + "Testdata.dat");
            WriteToGDBTFile(X, Y, folder + "GDBTTestdata.dat");
        } else {
            ReadFromFile(X, Y, folder + "Testdata.dat");
        }

        svm_model model = svm.svm_load_model(folder + "EventTypeModel.dat");
        int right = 0;
        for (int i = 0; i < X.size(); i++) {
            svm_node[] x = new svm_node[X.getFirst().size()];
            for (int j = 0; j < X.getFirst().size(); j++) {
                x[j] = new svm_node();
                x[j].index = j + 1;
                x[j].value = X.get(i).get(j);
            }
            double d = svm.svm_predict(model, x);
            if ((int) d == Y.get(i)) {
                right++;
            } else {
                int a = 0;
                a = a + 1;
            }
        }
        System.out.println("Testing Precision: " + 1.0 * right / X.size());

    }

    private void WriteToFile(LinkedList<LinkedList<Double>> X, LinkedList<Integer> Y, String file) {
        try {
            PrintStream ps = new PrintStream(new FileOutputStream(file));
            ps.println(X.size() + "\t" + X.getFirst().size());
            for (int i = 0; i < X.size(); i++) {
                for (int j = 0; j < X.getFirst().size(); j++) {
                    ps.print(X.get(i).get(j) + "\t");
                }
                ps.println();
            }
            ps.println();
            ps.println();
            for (int i = 0; i < Y.size(); i++) {
                ps.print(Y.get(i) + "\t");
            }
            ps.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(SLEventTypeClassifier.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void ReadFromFile(LinkedList<LinkedList<Double>> X, LinkedList<Integer> Y, String file) {
        try {
            Scanner input = new Scanner(new FileInputStream(file));
            int m = input.nextInt();
            int n = input.nextInt();
            for (int i = 0; i < m; i++) {
                X.add(new LinkedList<>());
                for (int j = 0; j < n; j++) {
                    double d = input.nextDouble();
                    X.get(i).add(d);
                }
            }
            for (int i = 0; i < m; i++) {
                int ii = input.nextInt();
                Y.add(ii);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(SLEventTypeClassifier.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static LinkedList<Double> GetFeatures(String anchor, Annotation document) {
        LinkedList<Double> res = new LinkedList<>();
        LinkedList<SLEntity> entities = new LinkedList<>();
        IndexedWord trigger = ParseSentenceAndGetEntities(anchor, entities, document, pipeline);
        if (trigger == null) {
            if (word2vec.containsKey(anchor.toLowerCase())) {
                res.addAll(word2vec.get(anchor.toLowerCase()));
            } else {
                return null;
            }
        } else if (word2vec.containsKey(trigger.word().toLowerCase())) {
            res.addAll(word2vec.get(trigger.word().toLowerCase()));
        } else if (word2vec.containsKey(trigger.lemma())) {
            res.addAll(word2vec.get(trigger.lemma()));
        } else {
            return null;
        }

        Lookup_All_wordvec(entities);
        LinkedList<Double> EntityHeadVector = SLMath.Vector_0(SLEventExtraction.dim);
        int valid_entity_num = 0;
        for (SLEntity ent : entities) {
            if (!ent.headwordvec.isEmpty()) {
                valid_entity_num++;
                EntityHeadVector = SLMath.Vector_add(EntityHeadVector, ent.headwordvec);
            }
        }
        if (valid_entity_num == 0) {
            return null;
        }
        EntityHeadVector = SLMath.Vector_divide_num(EntityHeadVector, valid_entity_num);
        res.addAll(EntityHeadVector);

        return res;
    }

    private LinkedList<String> GetCandidateTriggers(Annotation parsedsen) {
        LinkedList<String> res = new LinkedList<>();
        List<CoreMap> sentences = parsedsen.get(CoreAnnotations.SentencesAnnotation.class);

        assert sentences.size() == 1;

        CoreMap cm = sentences.get(0);
        for (CoreLabel token : cm.get(TokensAnnotation.class)) {
            IndexedWord iw = new IndexedWord(token);
            if (iw.tag().contains("NN") || iw.tag().contains("VB")) {
                res.add(iw.word());
            }
        }
        return res;
    }

    public void RecordEventPruningNegtiveExample(AceEvent event, String sentence) {
        Event e = new Event(event);
        Annotation document = ParseSentence(sentence);
        LinkedList<Double> features = GetEventPruningFeatures(e, document);
        if (features != null) {
            for (int i = 0; i < features.size(); i++) {
                EventPruningNegtiveExampleRecorder.print(features.get(i));
                if (i < features.size() - 1) {
                    EventPruningNegtiveExampleRecorder.print(" ");
                }
            }
            EventPruningNegtiveExampleRecorder.println();
        }
    }

    public LinkedList<Double> GetEventPruningFeatures(Event e, Annotation document) {
        LinkedList<Double> res = new LinkedList<>();
        LinkedList<SLEntity> entities = new LinkedList<>();
        IndexedWord trigger = ParseSentenceAndGetEntities(e.trigger, entities, document, pipeline);
        if (trigger == null) {
            if (word2vec.containsKey(e.trigger.toLowerCase())) {
                res.addAll(word2vec.get(e.trigger.toLowerCase()));
            } else {
                return null;
            }
        } else if (word2vec.containsKey(trigger.word().toLowerCase())) {
            res.addAll(word2vec.get(trigger.word().toLowerCase()));
        } else if (word2vec.containsKey(trigger.lemma())) {
            res.addAll(word2vec.get(trigger.lemma()));
        } else {
            return null;
        }

        Lookup_All_wordvec(entities);
        LinkedList<Double> EntityHeadVector = SLMath.Vector_0(SLEventExtraction.dim);
        int valid_entity_num = 0;
        for (EventArgument ea : e.arguments) {
            for (SLEntity sle : entities) {
                if (sle.content.contains(ea.data) || ea.data.contains(sle.content)) {
                    if (!sle.wordvec.isEmpty()) {
                        valid_entity_num++;
                        EntityHeadVector = SLMath.Vector_add(EntityHeadVector, sle.wordvec);
                    }
                }
            }
        }
        if (valid_entity_num == 0) {
            res.addAll(SLMath.Vector_0(SLEventExtraction.dim));
        } else {
            EntityHeadVector = SLMath.Vector_divide_num(EntityHeadVector, valid_entity_num);
//            LinkedList<Double> Vector_minus = SLMath.Vector_minus(res, EntityHeadVector);
            res.addAll(EntityHeadVector);
        }
        return res;
    }

    private void WriteToGDBTFile(LinkedList<LinkedList<Double>> X, LinkedList<Integer> Y, String file) {
        try {
            PrintStream ps = new PrintStream(new FileOutputStream(file));
            for (int i = 0; i < X.size(); i++) {
                for (int j = 0; j < X.getFirst().size(); j++) {
                    ps.print(X.get(i).get(j) + ",");
                }
                ps.print(Y.get(i));
                ps.println();
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(SLEventTypeClassifier.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
