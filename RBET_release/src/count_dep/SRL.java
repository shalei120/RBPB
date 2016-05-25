/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package count_dep;

import java.io.*;
import java.util.*;
import javafx.util.Pair;
import org.jdom2.JDOMException;

/**
 *
 * @author v-lesha
 */
public class SRL {

    public static LinkedList<Event> ReadSRLEvents(File f) throws FileNotFoundException {
        LinkedList<Event> res = new LinkedList<>();
        Scanner input = new Scanner(new FileInputStream(f));
        LinkedList<String> sentence = new LinkedList<>();
        while (input.hasNextLine()) {
            String line = input.nextLine();
            if (line.length() == 0) {
                int col = sentence.get(0).split("\t").length;
                int row = sentence.size();
                String[][] SRLtable = new String[row][col];
                for (int i = 0; i < row; i++) {
                    String[] split = sentence.get(i).split("\t");
                    System.arraycopy(split, 0, SRLtable[i], 0, col);
                }
                sentence.clear();
                int eventid = 1;
                for (int i = 0; i < row; i++) {
                    if (!"_".equals(SRLtable[i][10])) {
                        Event e = new Event();
                        e.trigger = SRLtable[i][1];
                        for (int j = 0; j < row; j++) {
                            if (!"_".equals(SRLtable[j][10 + eventid])) {
                                boolean has_dep = false;
                                if (SRLtable[j][4].equals("IN")) {
                                    for (int k = 0; k < row; k++) {
                                        if (Integer.valueOf(SRLtable[k][8]) == Integer.valueOf(SRLtable[j][0])){// && SRLtable[k][7].contains("NN")) {
                                            has_dep = true;
                                            if (SRLtable[j][10 + eventid].contains("TMP")) {
                                                e.arguments.add(new EventArgument(SRLtable[k][1], "Time"));
                                            } else {
                                                e.arguments.add(new EventArgument(SRLtable[k][1], ""));
                                            }
                                            break;
                                        }
                                    }
                                }
                                if (!has_dep) {
                                    if (SRLtable[j][10 + eventid].contains("TMP")) {
                                        e.arguments.add(new EventArgument(SRLtable[j][1], "Time"));
                                    } else {
                                        e.arguments.add(new EventArgument(SRLtable[j][1], ""));
                                    }
                                }
                            }
                        }
                        res.add(e);
                        eventid++;
                    }
                }
            } else {
                sentence.add(line);
            }

        }
        return res;
    }

    public Pair<Double,Double> runAlanRitter() throws FileNotFoundException, JDOMException, IOException {
        File answer = new File("D:\\alaneitter\\Iannotated\\Alan.apf.xml");
        File alanritter = new File("D:\\alaneitter\\Alan.output");
        double aver_pre = 0, aver_rec = 0;
        LinkedList<Event> SRLEvents = ReadSRLEvents(alanritter);
        LinkedList<Event> events = Count_dep.ReadEvents(answer);
        Pair<Double, Double> PR = Count_dep.Error_Analysis_Grishman(events, SRLEvents);
        aver_pre = PR.getKey();
        aver_rec = PR.getValue();
        return new Pair(aver_pre,aver_rec);
    }

    public Pair<Double,Double> runACE() throws FileNotFoundException, JDOMException, IOException {
        File corpus = new File("D:\\ACEAlan\\SRLout\\");
        File[] listFiles = corpus.listFiles();
        int num = 0;
        double aver_pre = 0, aver_rec = 0;
        for (File f : listFiles) {
            if (f.getName().endsWith("output")) {
                File answer = new File("D:\\LDC2006D06\\LDC2006D06\\Data\\LDC2006T06_Original\\data\\English\\nw\\fp1\\" + f.getName().substring(0, f.getName().length() - 10) + "apf.xml");
                LinkedList<Event> testevents = ReadSRLEvents(f);
                LinkedList<Event> events = Count_dep.ReadEvents(answer);
                if (events.size() < 1) {
                    continue;
                }
                num++;
                Pair<Double, Double> PR = Count_dep.Error_Analysis_Grishman(events, testevents);
                aver_pre += PR.getKey();
                aver_rec += PR.getValue();
            }
        }
        aver_pre /= num;
        aver_rec /= num;
        
        System.out.println("Precision: \t" + aver_pre);
        System.out.println("Recall   : \t" + aver_rec);
        return new Pair(aver_pre,aver_rec);

    }

    public static void main(String[] args) throws JDOMException, IOException {
        SRL srl = new SRL();
        srl.runACE();
       // srl.runAlanRitter();
    }
}
