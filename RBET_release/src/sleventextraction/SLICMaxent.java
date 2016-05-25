/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sleventextraction;

import AceJet.Datum;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import opennlp.maxent.BasicEventStream;
import opennlp.maxent.GIS;
import opennlp.maxent.GISModel;
import opennlp.maxent.PlainTextByLineDataStream;
import opennlp.maxent.io.GISModelWriter;
import opennlp.maxent.io.SuffixSensitiveGISModelWriter;
import opennlp.model.EventStream;

/**
 *
 * @author v-lesha
 */
public class SLICMaxent {

//    String featureFileName = "D:\\Modeldata\\ConnectivityFeature.txt";
//    String testFileName = "D:\\Modeldata\\ConnectivityFeatureTest.txt";
//    String modelFileName = "D:\\Modeldata\\ConnectivityModel.log";
    String featureFileName = "D:\\Grade3_2\\Chinese\\cpb\\cpb\\maxentfeature.txt";
    String testFileName = "D:\\Modeldata\\ConnectivityFeatureTest.txt";
    String modelFileName = "D:\\Grade3_2\\Chinese\\cpb\\cpb\\ConnectivityModel.log";

//    public SLICMaxent(int use) {
//        
//    }
    public void run() {
//        String featureFileName = "D:\\Modeldata\\1.txt";
        boolean USE_SMOOTHING = false;
        boolean PRINT_MESSAGES = true;
        double SMOOTHING_OBSERVATION = 0.1;

        try {
            FileReader datafr = new FileReader(new File(this.featureFileName));
            EventStream es = new BasicEventStream(new PlainTextByLineDataStream(datafr));
            GIS.SMOOTHING_OBSERVATION = SMOOTHING_OBSERVATION;
            GISModel model = GIS.trainModel(es, 1000, 4, USE_SMOOTHING, PRINT_MESSAGES);
            File outputFile = new File(modelFileName);
            GISModelWriter writer = new SuffixSensitiveGISModelWriter(model, outputFile);
            writer.persist();
            cal(model);
//            Predict(model, this.featureConverted);
//            Predict(model, this.AnswerConverted);
        } catch (Exception e) {
            System.err.print("Unable to create model due to exception: ");
            System.err.println(e);
        }
    }
    String featureConverted = "D:\\Modeldata\\ConnectivityFeatureConverted.txt";
    String AnswerConverted = "D:\\Modeldata\\AnswerConverted.txt";

    public static Datum DatumConverter(Datum d) {
        for (int i = 0; i < d.features.size(); i++) {
            if (d.features.get(i).contains("PredicateDepDis=")) {
                int eq = d.features.get(i).indexOf("=");
                String s = d.features.get(i).substring(eq + 1);
                int num = Integer.valueOf(s);
                d.features.set(i, "PredicateDepDis=" + (num == 0));
//                        d.features.set(i, "");
            } else if (d.features.get(i).contains("EntityDepDis=")) {
                d.features.set(i, "");
            } else if (d.features.get(i).contains("Trigger=")) {
                d.features.set(i, "");
            } else if (d.features.get(i).contains("entitytype=")) {
                d.features.set(i, "");
            } else if (d.features.get(i).contains("entitysubtype=")) {
                d.features.set(i, "");
            } else if (d.features.get(i).contains("mentiontype=")) {
                d.features.set(i, "");
            } else if (d.features.get(i).contains("EntityDis=")) {
                int eq = d.features.get(i).indexOf("=");
                String s = d.features.get(i).substring(eq + 1);
                int num = Integer.valueOf(s);
                d.features.set(i, "EntityDis=" + num / 8);
            } else if (d.features.get(i).contains("PredicateDis=")) {
                d.features.set(i, "");
            } else if (d.features.get(i).contains("deprel=")) {
                d.features.set(i, "");
            } else if (d.features.get(i).contains("head=")) {
//                        f1.add(d.features.get(i));
                d.features.set(i, "");
            } else if (d.features.get(i).contains("filename=")) {
                d.features.set(i, "");
            }
        }
        return d;
    }

    public void FileConverter(String name, String cov) {
        try {
            Scanner in = new Scanner(new FileInputStream(name));
            PrintStream ps = new PrintStream(new FileOutputStream(cov));
            while (in.hasNextLine()) {
                String line = in.nextLine();
                Datum d = new Datum(line);
                LinkedList<String> f1 = new LinkedList<>();
                d = DatumConverter(d);
//                    AddCombine(f1, d, "headcom");
                ps.println(d.toString());
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(SLICMaxent.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void Predict(GISModel model, String testfile) throws FileNotFoundException {
        Scanner in = new Scanner(new FileInputStream(testfile));
        int ia = 0, ib = 0, ic = 0, id = 0;
        while (in.hasNextLine()) {
            String line = in.nextLine();
            Datum d = new Datum(line);
            double prob = model.eval(d.toArray())[model.getIndex("yes")];
            String out = "";
            if (prob > 0.85) {
                out = "yes";
            } else {
                out = "no";
            }
            if ("yes".equals(out) && "yes".equals(d.getOutcome())) {
                ia++;
            } else if ("no".equals(out) && "yes".equals(d.getOutcome())) {
                ib++;
            } else if ("yes".equals(out) && "no".equals(d.getOutcome())) {
                System.out.println(d + " " + prob);
                ic++;
            } else if ("no".equals(out) && "no".equals(d.getOutcome())) {
                id++;
            }
        }
        System.out.println("aa bb:" + ia + " " + ib);
        System.out.println("cc dd:" + ic + " " + id);
        System.out.println(1.0 * ia / (ia + ic) + " " + 1.0 * ia / (ia + ib));
        System.out.println(1.0 * id / (ib + id) + " " + 1.0 * id / (ic + id));

        System.out.println("precision: " + 1.0 * (ia + id) / (ia + ib + ic + id));
        System.out.println();
    }

    public static void main(String[] args) {
        SLICMaxent sicm = new SLICMaxent();
//        sicm.FileConverter(sicm.featureFileName, sicm.featureConverted);
//        sicm.FileConverter(sicm.testFileName, sicm.AnswerConverted);
        sicm.run();
    }

    private void AddCombine(LinkedList<String> f1, Datum d, String name) {
        String s1 = f1.get(0).split("=")[1];
        String s2 = f1.get(1).split("=")[1];
        d.addFV(name, s1 + "_" + s2);
        d.addFV(name + "2", s2 + "_" + s1);
    }

    private void cal(GISModel model) {
        String sen = "保护 外商 投资 企业 合法 权益 六 项 规定";
        String pos = "VV NN VV NN JJ NN CD M NN";
        String srl = "O S-ARG0 rel S-arg1 O O O O O";
        String[] words = sen.split(" ");
        String[] poses = pos.split(" ");
        String[] srls = srl.split(" ");
        double[][] C = new double[words.length][words.length];
        for (int i = 0; i < words.length; i++) {
            C[i][i] = 1;
            for (int j = i + 1; j < words.length; j++) {
                Datum d = new Datum();
                d.addFV("trigger", "投资");
                d.addFV("ArgumentDistance", String.valueOf(Math.abs(i - j)));
                d.addFV("EntitySameSide", String.valueOf((i - 2) * (j - 2) > 0));
                d.addFV("Ap_pos", poses[i]);
                d.addFV("Bp_pos", poses[j]);
//                d.addFV("Bsrl", String.valueOf(srls[j].contains("ARG")));
                C[i][j] = model.eval(d.toArray())[model.getIndex("yes")];
                C[j][i] = C[i][j];
            }
        }
        for (int i = 0; i < words.length; i++) {
            for (int j = 0; j < words.length; j++) {
                System.out.print(C[i][j]);
                System.out.print("\t");
            }
            System.out.println();
        }
    }
}
