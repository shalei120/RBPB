/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package count_dep;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Scanner;
import javafx.util.Pair;
import org.jdom2.JDOMException;

/**
 *
 * @author v-lesha
 */
public class Summary {

    LinkedList<Event> events = new LinkedList<>(), Grishmanevents = new LinkedList<>(), srlevents = new LinkedList<>();

    void analysis() throws FileNotFoundException, JDOMException, IOException {
        Scanner sgmlist = new Scanner(new FileInputStream("D:\\LDC2006D06\\LDC2006D06\\Data\\LDC2006T06_Original\\data\\English\\nw\\sgmlist.txt"));
        String corpusfolder = "D:\\LDC2006D06\\LDC2006D06\\Data\\LDC2006T06_Original\\data\\English\\nw\\fp1\\";
        String Grishmanout = "D:\\LDC2006D06\\LDC2006D06\\Data\\LDC2006T06_Original\\data\\English\\nw\\out\\";
        String srlout = "D:\\ACEAlan\\SRLout\\";
        double aver_pre = 0, aver_rec = 0;
        int filenum = 0;
        int[][] GS = new int[3][3];
        Count_dep.threshold = 0.1;

        while (sgmlist.hasNext()) {
            events.clear();
            Grishmanevents.clear();
            srlevents.clear();

            String filename = sgmlist.next();
            filename = filename.substring(0, filename.length() - 3);
            File answer = new File(corpusfolder + filename + "apf.xml");
            File grishman = new File(Grishmanout + filename + "sgm.apf");
            File srl = new File(srlout + filename + "sgm.output");
            Grishmanevents = Count_dep.ReadGrishmanEvents(grishman);
            events = Count_dep.ReadEvents(answer);
            srlevents = SRL.ReadSRLEvents(srl);
            //  Countdependencies();
            if (events.size() < 3) {
                continue;
            }
            for (Event ans : events) {
                LinkedList<Event> grishman_triggerright = new LinkedList<>();
                LinkedList<Event> srl_triggerright = new LinkedList<>();
                boolean goodgrish = false, goodsrl = false;
                for (Event grish : Grishmanevents) {
                    if (grish.trigger.contains(ans.trigger) || ans.trigger.contains(grish.trigger)) {
                        Pair<Integer, Integer> miss = Count_dep.Grishman_Argument_Match_rec(ans.arguments, grish.arguments, 0);
                        if (ans.arguments.isEmpty() || miss.getKey() > 0) {
                            grishman_triggerright.add(grish);
                            Pair<Integer, Integer> num = Count_dep.Grishman_Argument_Match_rec(ans.arguments, grish.arguments, 2);
                            if (ans.arguments.isEmpty() || num.getKey() > 0) {
                                goodgrish = true;
                                break;
                            }
                        }
                    }
                }
                for (Event s : srlevents) {
                    if (s.trigger.contains(ans.trigger) || ans.trigger.contains(s.trigger)) {
                        Pair<Integer, Integer> miss = Count_dep.Grishman_Argument_Match_rec(ans.arguments, s.arguments, 0);
                        if (ans.arguments.isEmpty() || miss.getKey() > 0) {
                            srl_triggerright.add(s);
                            Pair<Integer, Integer> num = Count_dep.Grishman_Argument_Match_rec(ans.arguments, s.arguments, 2);
                            if (ans.arguments.isEmpty() || num.getKey() > 0) {
                                goodsrl = true;
                                break;
                            }
                        }
                    }
                }
                if (goodgrish && goodsrl) {
                    GS[0][0]++;
                } else if (!goodgrish && grishman_triggerright.isEmpty() && goodsrl) {
                    GS[0][1]++;
                } else if (!goodgrish && !grishman_triggerright.isEmpty() && goodsrl) {
                    GS[0][2]++;
                } else if (goodgrish && !goodsrl && srl_triggerright.isEmpty()) {
                    GS[1][0]++;
                } else if (!goodgrish && grishman_triggerright.isEmpty() && !goodsrl && srl_triggerright.isEmpty()) {
                    GS[1][1]++;
                } else if (!goodgrish && !grishman_triggerright.isEmpty() && !goodsrl && srl_triggerright.isEmpty()) {
                    GS[1][2]++;
                } else if (goodgrish && !goodsrl && !srl_triggerright.isEmpty()) {
                    GS[2][0]++;
                } else if (!goodgrish && grishman_triggerright.isEmpty() && !goodsrl && !srl_triggerright.isEmpty()) {
                    GS[2][1]++;
                } else if (!goodgrish && !grishman_triggerright.isEmpty() && !goodsrl && !srl_triggerright.isEmpty()) {
                    GS[2][2]++;
                }
            }
        }
        int sum = 0;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                sum += GS[i][j];
            }
        }
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                System.out.printf("%.3f", 1.0 * GS[i][j] / sum);
                if (j != 2) {
                    System.out.print("\t");
                } else {
                    System.out.println();
                }
            }
        }
    }

    public static void main(String[] args) throws JDOMException, IOException {
        Summary sum = new Summary();
        //sum.analysis();
        sum.CalTable();
    }

    public void CalTable() throws JDOMException, IOException {
        double[][] result = new double[4][21];
        int[][] paras = {
            {0, 0, 0}, {1, 1, -1}, {0, 1, 1}, {3, 1, -1}, {2, 1, -1}, {0, 0, 1}, {2, 0, -1}
        };
        Count_dep cd = new Count_dep();
        AlanRitter ar = new AlanRitter();
        SRL srl = new SRL();
        for (int i = 0; i < 7; i++) {
            Count_dep.state = paras[i][0];
            Count_dep.only_right = paras[i][1];
            Count_dep.threshold = paras[i][2];
            Pair<Double, Double> runACE = cd.runACE();
            Pair<Double, Double> runTwitter = cd.runTwitter();
            result[0][i] = runACE.getKey();
            result[1][i] = runACE.getValue();
            result[2][i] = runTwitter.getKey();
            result[3][i] = runTwitter.getValue();
        }
        for (int i = 0; i < 7; i++) {
            Count_dep.state = paras[i][0];
            Count_dep.only_right = paras[i][1];
            Count_dep.threshold = paras[i][2];
            Pair<Double, Double> runACE = ar.runGrishman();
            Pair<Double, Double> runTwitter = ar.runAlanRitter();
            result[0][i + 7] = runACE.getKey();
            result[1][i + 7] = runACE.getValue();
            result[2][i + 7] = runTwitter.getKey();
            result[3][i + 7] = runTwitter.getValue();
        }
        for (int i = 0; i < 7; i++) {
            Count_dep.state = paras[i][0];
            Count_dep.only_right = paras[i][1];
            Count_dep.threshold = paras[i][2];
            Pair<Double, Double> runACE = srl.runACE();
            Pair<Double, Double> runTwitter = srl.runAlanRitter();
            result[0][i + 7 * 2] = runACE.getKey();
            result[1][i + 7 * 2] = runACE.getValue();
            result[2][i + 7 * 2] = runTwitter.getKey();
            result[3][i + 7 * 2] = runTwitter.getValue();
        }
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 21; j++) {
                System.out.printf("%.3f", result[i][j]);
                if (j == 20) {
                    System.out.println();
                } else {
                    System.out.print('\t');
                }
            }
        }

    }
}
