/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sleventextraction;

import AceJet.Ace;
import AceJet.AceEvent;
import AceJet.Datum;
import AceJet.EventTagger;
import AceJet.TrainEventTagger;
import Jet.JetTest;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.util.CoreMap;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import opennlp.maxent.GISModel;
import static sleventextraction.SLEventExtraction.Lookup_All_wordvec;
import static sleventextraction.SLEventExtraction.word2vec;
import static sleventextraction.SLEventTypeClassifier.GetCorrespondingIndexedWord;
import slvectormodel.SLVectorModel;

/**
 *
 * @author v-lesha
 */
public class SLRegularization {

    public PrintStream ConnectivityFeatureWriter;
    public GISModel ConnectivityModel = null;
    String folder;
    Map<String, Integer> dep2int = new HashMap<>();

    public SLRegularization(int train) {
        folder = JetTest.getConfigFile("Ace.EventModels.directory");
        try {
            ConnectivityFeatureWriter = new PrintStream(new FileOutputStream(folder + "ConnectivityFeature.txt"));
//            ConnectivityFeatureWriter = new PrintStream(new FileOutputStream(folder + "ConnectivityFeatureSVM.txt"));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(SLRegularization.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public SLRegularization() {        
        folder = JetTest.getConfigFile("Ace.EventModels.directory");
//        try {
            System.out.println(TrainEventTagger.class);
            if (new File(folder + "ConnectivityModel.log").exists()) {
                ConnectivityModel = EventTagger.loadClassifierModel(folder + "ConnectivityModel.log");
            }
//            ConnectivityFeatureWriter = new PrintStream(new FileOutputStream(folder + "ConnectivityFeatureTest.txt"));
//            ConnectivityFeatureWriter = new PrintStream(new FileOutputStream(folder + "ConnectivityFeatureTestSVM.txt"));
//        } catch (FileNotFoundException ex) {
//            Logger.getLogger(SLRegularization.class.getName()).log(Level.SEVERE, null, ex);
//        }
    }

    public void RecordConnectivityFeatureToFile(IndexedWord iwtrigger, List<SLEntity> ModiEntities, CoreMap senCM, SemanticGraph senSG, String anchor) {

        int iConnectedNum = 0;
        for (int i = 0; i < ModiEntities.size(); i++) {
            if (ModiEntities.get(i).isArg.equals("arg")) {
                for (int j = i + 1; j < ModiEntities.size(); j++) {
                    if (ModiEntities.get(j).isArg.equals("arg")) {
                        Datum d = null;
                        d = ConnectiveFeatures(iwtrigger, senCM, senSG, ModiEntities.get(i), ModiEntities.get(j), anchor);
                        d.setOutcome("yes");
                        ConnectivityFeatureWriter.println(d);
                        iConnectedNum++;
                    }
                }
            }
        }

        int curr = 0;
        for (int k = 0; k < iConnectedNum; k++) {
            loop:
            for (int i = 0; i < ModiEntities.size(); i++) {
                if (ModiEntities.get(i).isArg.equals("arg")) {
                    for (int j = curr; j < ModiEntities.size(); j++) {
                        if (ModiEntities.get(j).isArg.equals("noArg")) {
                            if (NoConflict(j, ModiEntities)) {
                                curr = j + 1;
                                Datum d = ConnectiveFeatures(iwtrigger, senCM, senSG, ModiEntities.get(i), ModiEntities.get(j), anchor);
                                d.setOutcome("no");
                                ConnectivityFeatureWriter.println(d);
                                break loop;
                            }
                        }
                    }
                }
            }
        }
    }

    public void RecordConnectivityFeatureToFileSVM(String trigger, List<SLEntity> ModiEntities, CoreMap senCM, SemanticGraph senSG) {
        Lookup_All_wordvec(ModiEntities);
        int iConnectedNum = 0;
        for (int i = 0; i < ModiEntities.size(); i++) {
            if (ModiEntities.get(i).isArg.equals("arg")) {
                for (int j = i + 1; j < ModiEntities.size(); j++) {
                    if (ModiEntities.get(j).isArg.equals("arg")) {

                        LinkedList<Double> d = ConnectiveFeaturesSVM(trigger, senCM, senSG, ModiEntities.get(i), ModiEntities.get(j));
                        if (d == null) {
                            continue;
                        }
                        for (double di : d) {
                            ConnectivityFeatureWriter.print(di + " ");
                        }
                        ConnectivityFeatureWriter.println("1");
                        iConnectedNum++;
                    }
                }
            }
        }

//        int curr = 0;
//        for (int k = 0; k < iConnectedNum; k++) {
//            loop:
//            for (int i = 0; i < ModiEntities.size(); i++) {
//                if (ModiEntities.get(i).isArg.equals("arg")) {
//                    for (int j = curr; j < ModiEntities.size(); j++) {
//                        if (ModiEntities.get(j).isArg.equals("noArg")) {
//                            curr = j + 1;
//                            LinkedList<Double> d = ConnectiveFeaturesSVM(trigger, senCM, senSG, ModiEntities.get(i), ModiEntities.get(j));
//                            if (d == null) {
//                                continue;
//                            }
//                            for (double di : d) {
//                                ConnectivityFeatureWriter.print(di + " ");
//                            }
//                            ConnectivityFeatureWriter.println("0");
//                            break loop;
//                        }
//                    }
//                }
//            }
//        }
    }

    public Datum ConnectiveFeatures(IndexedWord iwtrigger, CoreMap senCM, SemanticGraph senSG, SLEntity e1, SLEntity e2, String anchor) {
        Datum d = new Datum();
        if (e1.head == null || e2.head == null) {
            return null;
        }
        d.addFV("filename", Ace.currentDocPath);
        d.addFV("Trigger", anchor);
        d.addFV("deprel", e1.dep);
        d.addFV("deprel", e2.dep);
        d.addFV("head", e1.head.word());
        d.addFV("head", e2.head.word());
        d.addFV("entitytype", e1.entitytype);
        d.addFV("entitytype", e2.entitytype);
        d.addFV("entitysubtype", e1.entitysubtype);
        d.addFV("entitysubtype", e2.entitysubtype);
        d.addFV("mentiontype", e1.mentiontype);
        d.addFV("mentiontype", e2.mentiontype);

        if (e1.head.index() < iwtrigger.index() && e2.head.index() < iwtrigger.index() || e1.head.index() > iwtrigger.index() && e2.head.index() > iwtrigger.index()) {
            d.addFV("TriggerSameOrNot", "yes");
        } else {
            d.addFV("TriggerSameOrNot", "no");
        }
        int entitydis = Math.abs(e1.head.index() - e2.head.index());
        d.addFV("EntityDis", String.valueOf(entitydis));
        CoreMap span = senCM;
        SemanticGraph totaldep = senSG;
        List<SemanticGraphEdge> path = totaldep.getShortestUndirectedPathEdges(e1.head, e2.head);

        int entitydepdis = path == null ? 1000 : path.size();
        d.addFV("EntityDepDis", String.valueOf(entitydepdis));

        int predicatedis = Math.abs(e1.predicate.index() - e2.predicate.index());
        d.addFV("PredicateDis", String.valueOf(predicatedis));
        List<SemanticGraphEdge> prepath = totaldep.getShortestUndirectedPathEdges(e1.predicate, e2.predicate);
        int predepdis = prepath == null ? 1000 : prepath.size();
        if (predepdis == 0) {
            d.addFV("predicatePOS", e1.predicate.tag());
            if (e1.head.index() < e1.predicate.index() && e2.head.index() < e1.predicate.index() || e1.head.index() > e1.predicate.index() && e2.head.index() > e1.predicate.index()) {
                d.addFV("PredicateSameOrNot", "yes");
            } else {
                d.addFV("PredicateSameOrNot", "no");
            }
        }
        d.addFV("PredicateDepDis", String.valueOf(predepdis));

        return d;

    }

    public void trainMaxEnt() {
        SLICMaxent sicm = new SLICMaxent();
        sicm.FileConverter(folder + "ConnectivityFeature.txt", folder + "ConnectivityFeatureConverted.txt");
        TrainEventTagger.buildClassifierModel(folder + "ConnectivityFeatureConverted.txt", folder + "ConnectivityModel.log");
    }

    public LinkedList<Double> ConnectiveFeaturesSVM(String trigger, CoreMap senCM, SemanticGraph senSG, SLEntity e1, SLEntity e2) {
        if (trigger.contains("/")) {
            String[] split = trigger.split("/");
            trigger = split[0];
        }
        LinkedList<Double> res = new LinkedList<>();
        CoreMap cm = senCM;
        SemanticGraph dependencies = senSG;
        IndexedWord iwtrigger = GetCorrespondingIndexedWord(trigger, cm);
        if (iwtrigger == null) {
            if (word2vec.containsKey(trigger.toLowerCase())) {
                res.addAll(word2vec.get(trigger.toLowerCase()));
            } else {
                return null;
            }
        } else if (word2vec.containsKey(iwtrigger.word().toLowerCase())) {
            res.addAll(word2vec.get(iwtrigger.word().toLowerCase()));
        } else if (word2vec.containsKey(iwtrigger.lemma())) {
            res.addAll(word2vec.get(iwtrigger.lemma()));
        } else {
            return null;
        }
        if (e1.wordvec == null || e2.wordvec == null) {
            return null;
        }
        res.addAll(SLMath.Vector_minusAbs(e1.wordvec, e2.wordvec));
//        if (e1.headwordvec.isEmpty() || e2.headwordvec.isEmpty()) {
//            int a = 0;
//            a = a + 1;
//        }
//        res.addAll(SLMath.Vector_add(e1.headwordvec, e2.headwordvec));
//        res.addAll(e2.headwordvec);
//        if (!this.dep2int.containsKey(e1.dep)) {
//            dep2int.put(e1.dep, dep2int.size());
//        }
//        if (!this.dep2int.containsKey(e2.dep)) {
//            dep2int.put(e2.dep, dep2int.size());
//        }
//        res.add(1.0 * dep2int.get(e1.dep));
//        res.add(1.0 * dep2int.get(e2.dep));
        return res;
    }

    private boolean NoConflict(int pos, List<SLEntity> le) {
        for (int i = 0; i < le.size(); i++) {
            if (i != pos) {
                if ("arg".equals(le.get(i).isArg)) {
                    if (le.get(i).head.word().equals(le.get(pos).head.word())) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static class SLNode {

        int[] X;
        int iVarnum;
        double score = -1;

        String fingerprint;

        public SLNode(int[] xx, int iVarnum, double score) {
            this.iVarnum = iVarnum;
            X = new int[iVarnum];
            System.arraycopy(xx, 0, X, 0, iVarnum);
            this.fingerprint = GetFingerPrint(X);
            this.score = score;
        }

        private SLNode(SLNode sln) {
            this.score = sln.score;
            this.iVarnum = sln.iVarnum;
            X = new int[iVarnum];
            this.fingerprint = sln.fingerprint;
            System.arraycopy(sln.X, 0, X, 0, iVarnum);
        }

        private String GetFingerPrint(int[] X) {
            String s = "";
            for (int i = 0; i < X.length; i++) {
                if (X[i] == 0) {
                    s += '0';
                } else {
                    s += '1';
                }
            }
            return s;
        }

    }

    public static class SLOptimize {

        double[][] C;
        double[] P;
        int iVarnum;
        int constraintnum;
        int[][] roletypes;
        double[][] roleprobs, RPlookup;
        LinkedList<Integer> schema;

        public SLOptimize(int num, double[][] c, double[] p, AceEvent event, int[][] rt, double[][] rp) {
            this.iVarnum = num;
            C = new double[num][num];
            P = new double[num];
            for (int i = 0; i < c.length; i++) {
                System.arraycopy(c[i], 0, C[i], 0, C.length);
            }
            System.arraycopy(p, 0, P, 0, P.length);

            int typeid = SLVectorModel.CheckPos(event.subtype, SLVectorModel.Event_types);
            schema = SLVectorModel.Templates.get(typeid);
            if (schema == null) {
                int a = 0;
                a = a + 1;
            }
            roletypes = new int[iVarnum][5];
            roleprobs = new double[iVarnum][5];
            RPlookup = new double[iVarnum][35];
            for (int i = 0; i < this.iVarnum; i++) {
                for (int j = 0; j < 5; j++) {
                    roletypes[i][j] = rt[i][j];
                    roleprobs[i][j] = rp[i][j];
                    if (rt[i][j] == -1) {
                        continue;
                    }
                    RPlookup[i][rt[i][j]] = rp[i][j];
                }
            }
        }

        public int[] BeamSearch() {
            int[] X = new int[iVarnum];
            SLNode root = new SLNode(X, iVarnum, this.Evaluate(X));
            LinkedList<SLNode> FS = new LinkedList<>();
            FS.add(root);

            PriorityQueue<SLNode> q_old = new PriorityQueue<>(10, new Comparator<SLNode>() {
                @Override
                public int compare(SLNode s1, SLNode s2) {
                    if (s1.score > s2.score) {
                        return -1;
                    } else if (s1.score < s2.score) {
                        return 1;
                    } else {
                        return 0;
                    }
                }
            });
            for (int i = 0; i < iVarnum; i++) {
                Object[] arr = FS.toArray();
                FS.clear();
                for (Object osln : arr) {
                    SLNode sln = (SLNode) osln;
                    sln.X[i] = 0;
                    FS.add(new SLNode(sln.X, iVarnum, this.Evaluate(sln.X)));
//                    sln.X[i] = 1;
//                    FS.add(new SLNode(sln.X, iVarnum, this.Evaluate(sln.X)));
                    for (int j = 0; j < 5; j++) {
                        if (roleprobs[i][j] > 0.0001) {
//                            SLNode new_sln = new SLNode(sln);
                            sln.X[i] = this.roletypes[i][j] + 1;
                            FS.add(new SLNode(sln.X, iVarnum, this.Evaluate(sln.X)));
                        }
                    }
                }
                q_old.addAll(FS);
                FS.clear();
                int initial_size = q_old.size();
                for (int j = 0; j < initial_size && j < 10; j++) {
                    SLNode poll = q_old.poll();
                    FS.add(poll);
                }
                q_old.clear();
            }
            q_old.addAll(FS);
            SLNode peek = q_old.peek();
            return peek.X;
        }

        static double lambda1 = 0.1, lambda3 = 0.45, lambda4 = 0.45;

        public double Evaluate(int[] X) {
            double val = 0;// -SLMath.Quadratic(X, C) - lambda3 * SLMath.dot( P,X);
            for (int i = 0; i < this.iVarnum; i++) {
                for (int j = 0; j < this.iVarnum; j++) {
                    if (X[i] != 0 && X[j] != 0) {
                        val += lambda1 * C[i][j];
                    }
                }
            }
            for (int i = 0; i < this.iVarnum; i++) {
                if (X[i] != 0) {
                    val += lambda3 * P[i];
                }
            }
//            for (int i = 0; i < this.schema.size(); i++) {
//                int roleid = schema.get(i);
//                double dsum = 0;
//                for (int j = 0; j < this.iVarnum; j++) {
//                    if (X[j] == roleid + 1) {
//                        dsum += 1;
//                    }
//                }
//                dsum -= 1;
//                if(dsum==-1){
//                    val+=8;
//                }
////                val += lambda * Math.abs(Math.pow(10, -dsum) - 1);
//            }
            for (int i = 0; i < X.length; i++) {
                if (X[i] == 0) {
                    val += (lambda3 + lambda4) * 0.1;
                }
            }

            // the role 
            for (int i = 0; i < this.iVarnum; i++) {
                if (X[i] > 0) {
                    val += lambda4 * (RPlookup[i][X[i] - 1] * P[i]);
                }
            }
            return val;
        }

        public int[] SimulateAnnealing() {
            double r = 0.8;
            int[] X = new int[iVarnum];
            for (int i = 0; i < P.length; i++) {
                if (P[i] <= 2.3) {
                    X[i] = this.roletypes[i][0] + 1;
                }
            }
            SLNode s0 = new SLNode(X, this.iVarnum, this.Evaluate(X));
            double T = 100;
            double T_min = 10;
            while (T > T_min) {
                SLNode s_new = SANeighbor(s0);
                double dE = -s_new.score + s0.score;
                if (dE >= 0) {
                    s0 = s_new;
                } else if (Math.exp(dE / T) > Math.random()) {
                    s0 = s_new;
                }
                T = T * r;
            }
            return s0.X;
        }

        private SLNode SANeighbor(SLNode s0) {
            double prob[] = {0.4, 0.28, 0.13, 0.08, 0.07, 0.04};
            for (int i = 1; i < prob.length; i++) {
                prob[i] = prob[i] + prob[i - 1];
            }
            int X_new[] = new int[s0.iVarnum];
            System.arraycopy(s0.X, 0, X_new, 0, iVarnum);
            int changepos = (int) (Math.random() * 1000) % s0.iVarnum;
//            if (X_new[changepos] == 0) {
//                if (Math.random() > 0.5) {
//                    X_new[changepos] = 1;
//                }
//            } else {
//                if (Math.random() > 0.8) {
//                    X_new[changepos] = 0;
//                }
//            }
            double rr = Math.random();
            int ans = -1;
            for (int i = 0; i < prob.length; i++) {
                if (rr < prob[i]) {
                    ans = i;
                    break;
                }
            }
            if (ans == 0) {
                X_new[changepos] = 0;
            } else {
                while (this.roletypes[changepos][ans - 1] == -1) {
                    ans--;
                }
                X_new[changepos] = this.roletypes[changepos][ans - 1] + 1;
            }
            return new SLNode(X_new, iVarnum, this.Evaluate(X_new));
        }
    }

}
