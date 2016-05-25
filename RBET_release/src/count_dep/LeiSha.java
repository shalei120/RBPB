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
import java.util.LinkedList;
import java.util.Scanner;
import javafx.util.Pair;
import org.jdom2.JDOMException;

/**
 *
 * @author v-lesha
 */
public class LeiSha {

    public LeiSha() {
    }

    public static void main(String[] args) throws JDOMException, IOException {
        LeiSha ls = new LeiSha();
        ls.run();
    }

    private void run() throws FileNotFoundException, JDOMException, IOException {
        Scanner sgmlist = new Scanner(new FileInputStream("D:\\LDC2006D06\\LDC2006D06\\Data\\LDC2006T06_Original\\data\\English\\nw\\sgmlist.txt"));

        File answercorpus = new File("D:\\LDC2006D06\\LDC2006D06\\Data\\LDC2006T06_Original\\data\\English\\nw\\fp1\\");
        File testsgm = new File("D:\\EventExtraction\\");
        double aver_pre = 0, aver_rec = 0;
        File[] testfiles = testsgm.listFiles();
        int num = 0;
        while (sgmlist.hasNext()) {
            String filename = sgmlist.next();
            System.out.println(filename);
            filename = filename.substring(0, filename.length() - 3);
            File answer = new File("D:\\LDC2006D06\\LDC2006D06\\Data\\LDC2006T06_Original\\data\\English\\nw\\fp1\\" + filename + "apf.xml");
            LinkedList<Event> SLEvents = ReadLeiShaEvents("D:\\EventExtraction\\EventCandidate_" + filename + "txt");
            LinkedList<Event> events = Count_dep.ReadEvents(answer);
            if (events.size() < 1) {
                continue;
            }
            num++;
            Pair<Double, Double> PR = Error_Analysis_LeiSha(events, SLEvents);
            aver_pre += PR.getKey();
            aver_rec += PR.getValue();
        }
        aver_pre /= num;
        aver_rec /= num;
        System.out.println(aver_pre);
        System.out.println(aver_rec);
    }

    private LinkedList<Event> ReadLeiShaEvents(String f) throws FileNotFoundException {
        LinkedList<Event> events = new LinkedList<>();
        Scanner input = new Scanner(new FileInputStream(f));
        int num = input.nextInt();

        for (int i = 0; i < num; i++) {
            input.nextLine();
            Event e = new Event();
            String span = input.nextLine();
            span = span.replace("|", " ");
            e.span=span.substring(span.indexOf("***")+3);
          //  System.out.println(e.span);
            int argunum = input.nextInt();
            input.nextLine();
            for (int j = 0; j < argunum; j++) {
                String entity = input.nextLine();
                String[] split = entity.split("\t");
                EventArgument ea = new EventArgument();
                ea.data = split[0];
                e.arguments.add(ea);
            }
            events.add(e);
        }
        return events;
    }

    public static Pair<Double, Double> Error_Analysis_LeiSha(LinkedList<Event> events, LinkedList<Event> SLEvents) {
        int prec_correct = 0, rec_correct = 0;
        for (Event ans : events) {
            boolean right=false;
            for (Event sle : SLEvents) {
                if (containAnswer(sle, ans)) {
                    rec_correct++;
                    right=true;
                    break;
                }
            }
            if(!right){
                int a=0;
                a=a+1;
            }
        }
        for (Event sle : SLEvents) {
            for (Event ans : events) {
                if (containAnswer(sle, ans)) {
                    prec_correct++;
                    break;
                }
            }
        }
        double prec = 1.0 * prec_correct / SLEvents.size();
        double rec = 1.0 * rec_correct / events.size();
        return new Pair<>(prec, rec);
    }

    public static boolean containAnswer(Event sle, Event ans) {
        boolean res = false;
        if (sle.span.contains(ans.span)||ans.span.contains(sle.span)) {
            int num = 0;
            for (EventArgument eaans : ans.arguments) {
                for (EventArgument easl : sle.arguments) {
                    if (overlap(eaans.data, easl.data) > 0) {
                        num++;
                        break;
                    }
                }
            }
            res = num == ans.arguments.size();
        } else {
            res = false;
        }
        return res;
    }

    public static double overlap(String ans, String test) {
        String[] answord = ans.split("[-| |\n|'|\"|$]");
        String[] testword = test.split("[-| |\n|'|\"|$]");
        int[][] lcs = new int[answord.length + 1][testword.length + 1];
        for (int i = 1; i <= answord.length; i++) {
            for (int j = 1; j <= testword.length; j++) {
                if (answord[i - 1].equals(testword[j - 1])) {
                    lcs[i][j] = lcs[i - 1][j - 1] + 1;
                } else {
                    lcs[i][j] = Math.max(lcs[i - 1][j], lcs[i][j - 1]);
                }
            }
        }
        return 1.0 * lcs[answord.length][testword.length] / answord.length;
    }
}
