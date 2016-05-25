/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sleventextraction;

import AceJet.AceEvent;
import AceJet.AceMention;
import AceJet.Datum;
import Jet.JetTest;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author v-lesha
 */
public class SLMentionRecorder {

    public static final String folder = JetTest.getConfigFile("Ace.EventModels.directory");
    

    public static Map<String, Integer> Type2pos = new HashMap<String, Integer>() {
        {
            put("Life:Be-Born", 0);
            put("Life:Die", 1);
            put("Life:Marry", 2);
            put("Life:Divorce", 3);
            put("Life:Injure", 4);
            put("Transaction:Transfer-Ownership", 5);
            put("Transaction:Transfer-Money", 6);
            put("Movement:Transport", 7);
            put("Business:Start-Org", 8);
            put("Business:End-Org", 9);
            put("Business:Declare-Bankruptcy", 10);
            put("Business:Merge-Org", 11);
            put("Conflict:Attack", 12);
            put("Conflict:Demonstrate", 13);
            put("Contact:Meet", 14);
            put("Contact:Phone-Write", 15);
            put("Personnel:Start-Position", 16);
            put("Personnel:End-Position", 17);
            put("Personnel:Nominate", 18);
            put("Personnel:Elect", 19);
            put("Justice:Arrest-Jail", 20);
            put("Justice:Release-Parole", 21);
            put("Justice:Charge-Indict", 22);
            put("Justice:Trial-Hearing", 23);
            put("Justice:Sue", 24);
            put("Justice:Convict", 25);
            put("Justice:Sentence", 26);
            put("Justice:Fine", 27);
            put("Justice:Execute", 28);
            put("Justice:Extradite", 29);
            put("Justice:Acquit", 30);
            put("Justice:Pardon", 31);
            put("Justice:Appeal", 32);
        }
    };
    PrintStream EntityRecorder = null;
    public LinkedList<Datum> DataBuffer = new LinkedList<>();
    public LinkedList<AceMention> EntityBuffer = new LinkedList<>();
    public static int count = 0;
    public int linecount = 0, dataline = 0;

    public SLMentionRecorder() {
        try {
            EntityRecorder = new PrintStream(new FileOutputStream(folder + "TestEntity.txt"));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(SLMentionRecorder.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public SLMentionRecorder(int train) {
        try {
            EntityRecorder = new PrintStream(new FileOutputStream(folder + "TrainEntity.txt"));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(SLMentionRecorder.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public int posnum = 0;

    public void RecordFeatureToFile(String outcome, Datum feature, AceMention m) {
        feature.setOutcome(outcome);
        DataBuffer.add(feature);
        m.isArg = outcome;
        m.ground = outcome;
        EntityBuffer.add(m);
        if (outcome.equals("arg")) {
            posnum++;
        }
    }

    public void ClearBuffer(AceEvent event) {
        posnum = 0;
        int eventtypeindex = Type2pos.get(event.type + ":" + event.subtype);

//        for (Datum d : DataBuffer) {
//            String out = "";
//            if (d.getOutcome().equals("noArg")) {
//                out = "noArg";
//            } else {
//                out = "arg";
//            }
//            out += " ---- ";
//            String[] arr = d.toArray();
//            for (String s : arr) {
//                out += s.trim().replaceAll("[\n| ]", "_");
//                out += "\t";
//            }
////            if (d.getOutcome().equals("noArg")) {
////                out += "noArg";
////            } else {
////                out += "arg";
////            }
////            ps[eventtypeindex].print(out + "\n");
//        }
//        ps[eventtypeindex].print("\n");
        dataline += DataBuffer.size() + 1;
        DataBuffer.clear();

        for (AceMention m : EntityBuffer) {
            EntityRecorder.println(m.text);
        }
        EntityRecorder.println();
        EntityBuffer.clear();
    }

    public void ClearBufferNotPrint() {
        posnum = 0;
        DataBuffer.clear();
        EntityBuffer.clear();
    }

    public int GetBufferSize() {
        return DataBuffer.size();
    }

    public static SLEntity GetSLEntityMatch(AceMention mention, List<SLEntity> NPlist) {
        for (SLEntity sle : NPlist) {
            int over = StrictOverlap(sle.content, mention.text);
            if (Math.abs(over - sle.content.length()) <= 3 || Math.abs(over - mention.text.length()) <= 3) {
                return sle;
            } else if (1.0 * over / sle.content.length() > 0.7 || 1.0 * over / mention.text.length() > 0.7) {
                return sle;
            }
        }
        return null;
    }

    public static int StrictOverlap(String a, String b) {
        int[][] c = new int[a.length() + 1][b.length() + 1];
        int maxentry = -1;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                if (a.charAt(i - 1) == b.charAt(j - 1) || IsBlankChar(a.charAt(i - 1)) && IsBlankChar(b.charAt(j - 1))) {
                    c[i][j] = c[i - 1][j - 1] + 1;
                } else {
                    c[i][j] = 0;
                }
                if (maxentry < c[i][j]) {
                    maxentry = c[i][j];
                }
            }
        }
        return maxentry;
    }

    private static boolean IsBlankChar(char c) {
        return c == '\n' || c == ' ' || c == '\'' || c == '\t' || c == '\"';
    }

}
