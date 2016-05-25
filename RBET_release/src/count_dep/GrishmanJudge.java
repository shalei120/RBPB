/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package count_dep;

import static count_dep.Count_dep.ReadEvents;
import static count_dep.Count_dep.ReadGrishmanEvents;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Scanner;
import javafx.util.Pair;
import org.jdom2.JDOMException;
import static sleventextraction.SLEventExtraction.overlap;

/**
 *
 * @author v-lesha
 */
public class GrishmanJudge {

    public static boolean Me = false;

    LinkedList<Event> events;
    LinkedList<Event> Grishmanevents;

    public GrishmanJudge() {
        events = new LinkedList<>();
        Grishmanevents = new LinkedList<>();

    }

    private int GetEventTypeCode_CRF_Form(String type) {
        if (type.equals("Attack")) {
            return 0;
        } else if (type.equals("Meet")) {
            return 1;
        } else if (type.equals("Die")) {
            return 2;
        } else if (type.equals("Transport")) {
            return 3;
        } else {
            return -1;
        }
    }

    public void Count_F1_In_semiCRF_Form() throws JDOMException, IOException {
        Scanner sgmlist = new Scanner(new FileInputStream("D:\\LDC2006D06\\LDC2006D06\\Data\\LDC2006T06_Original\\data\\English\\nw\\Testfile.txt"));
        String corpusfolder = "D:\\LDC2006D06\\LDC2006D06\\Data\\LDC2006T06_Original\\data\\English\\nw\\fp1\\";
        String Grishmanout = "D:\\LDC2006D06\\LDC2006D06\\Data\\LDC2006T06_Original\\data\\English\\nw\\nopatternout\\";
//        String Grishmanout = "D:\\LDC2006D06\\LDC2006D06\\Data\\LDC2006T06_Original\\data\\English\\nw\\out\\";
        int ans_role_count[] = new int[4];
        int extracted_role_count[] = new int[4];
        int ansright_role_count[] = new int[4];
        int extractedright_role_count[] = new int[4];

        int filenum = 0;
        while (sgmlist.hasNext()) {
            events.clear();
            Grishmanevents.clear();
            String filename = sgmlist.next();
            filename = filename.substring(0, filename.length() - 3);
            File answer = new File(corpusfolder + filename + "apf.xml");
            File grishman = new File(Grishmanout + filename + "sgm.apf");
            Grishmanevents = ReadGrishmanEvents(grishman);
            events = ReadEvents(answer);
            for (Event ans : events) {
                int code = GetEventTypeCode_CRF_Form(ans.eventtype);
                if (code != -1) {
                    ans_role_count[code] += ans.arguments.size();
                    for (EventArgument ansa : ans.arguments) {
                        loop:
                        for (Event grish : Grishmanevents) {
//                            if (overlap(ans.span, grish.span) > 0.7 || overlap(grish.span, ans.span) > 0.7) {
                            if (grish.eventtype.equals(ans.eventtype)) {
                                for (EventArgument grisha : grish.arguments) {
                                    if ((overlap(grisha.data, ansa.data) > 0 || overlap(ansa.data, grisha.data) > 0) && grisha.type.equals(ansa.type)) {
                                        ansright_role_count[code]++;
                                        break loop;
                                    }
                                }
//                                }
                            }
                        }
                    }
                }
            }
            for (Event grish : Grishmanevents) {
                int code = GetEventTypeCode_CRF_Form(grish.eventtype);
                if (code != -1) {
                    extracted_role_count[code] += grish.arguments.size();
                    for (EventArgument grisha : grish.arguments) {
                        loop:
                        for (Event ans : events) {
//                            if (overlap(ans.span, grish.span) > 0.7 || overlap(grish.span, ans.span) > 0.7) {
                            if (grish.eventtype.equals(ans.eventtype)) {
                                for (EventArgument ansa : ans.arguments) {
                                    if ((overlap(grisha.data, ansa.data) > 0 || overlap(ansa.data, grisha.data) > 0) && grisha.type.equals(ansa.type)) {
                                        extractedright_role_count[code]++;
                                        break loop;
                                    }
//                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        double prec[] = new double[4];
        double rec[] = new double[4];
        double F[] = new double[4];
        for (int i = 0; i < 4; i++) {
            prec[i] = 1.0 * extractedright_role_count[i] / extracted_role_count[i];
            rec[i] = 1.0 * ansright_role_count[i] / ans_role_count[i];
            F[i] = 2 * prec[i] * rec[i] / (prec[i] + rec[i]);
        }
        System.out.println("Attack F1 = " + F[0]);
        System.out.println("Meet F1 = " + F[1]);
        System.out.println("Die F1 = " + F[2]);
        System.out.println("Transport F1 = " + F[3]);
    }

    public double runACE() throws JDOMException, IOException {
        Scanner sgmlist = new Scanner(new FileInputStream("D:\\Grade2_3\\LDC2006D06\\Data\\LDC2006T06_Original\\data\\English\\nw\\Testfile.txt"));
        String corpusfolder = "D:\\Grade2_3\\LDC2006D06\\Data\\LDC2006T06_Original\\data\\English\\nw\\fp1\\";
        String Grishmanout = "D:\\Grade2_3\\LDC2006D06\\Data\\LDC2006T06_Original\\data\\English\\trainout\\";
//        String Grishmanout = "D:\\LDC2006D06\\LDC2006D06\\Data\\LDC2006T06_Original\\data\\English\\nw\\out\\";
        double aver_pre_TIC = 0, aver_rec_TIC = 0;
        double aver_pre_RI = 0, aver_rec_RI = 0;
        double aver_pre_RIC = 0, aver_rec_RIC = 0;

        int filenum = 0;
        while (sgmlist.hasNext()) {

            events.clear();
            Grishmanevents.clear();
            String filename = sgmlist.next();
            filename = filename.substring(0, filename.length() - 3);
            File answer = new File(corpusfolder + filename + "apf.xml");
            File grishman = new File(Grishmanout + filename + "sgm.apf");
            Grishmanevents = ReadGrishmanEvents(grishman);
            events = ReadEvents(answer);
            //  Countdependencies();
            if (events.size() < 3) {
                continue;
            }
//            if (filename.equals("APW_ENG_20030519.0367.")) {
            Pair<Double, Double> TIC = Trigger_id_cl(events, Grishmanevents);
            Pair<Double, Double> RI = Argument_id(events, Grishmanevents);
            Pair<Double, Double> RIC = Argument_id_cl(events, Grishmanevents);

//            Error3(events, Grishmanevents);
            if (TIC.getKey() != 0 && TIC.getValue() != 0 && RI.getKey() != 0 && RI.getValue() != 0 && RIC.getKey() != 0 && RIC.getValue() != 0) {
                aver_pre_TIC += TIC.getKey();
                aver_rec_TIC += TIC.getValue();
                aver_pre_RI += RI.getKey();
                aver_rec_RI += RI.getValue();
                aver_pre_RIC += RIC.getKey();
                aver_rec_RIC += RIC.getValue();
                filenum++;
            }
//            }
//            break;
        }
        aver_pre_TIC /= filenum;
        aver_rec_TIC /= filenum;
        aver_pre_RI /= filenum;
        aver_rec_RI /= filenum;
        aver_pre_RIC /= filenum;
        aver_rec_RIC /= filenum;
        System.out.println("Trigger id+cl Precision: \t" + aver_pre_TIC);
        System.out.println("Trigger id+cl Recall   : \t" + aver_rec_TIC);
        System.out.println("Argument id Precision: \t" + aver_pre_RI);
        System.out.println("Argument id Recall   : \t" + aver_rec_RI);
        System.out.println("Argument id+cl Precision: \t" + aver_pre_RIC);
        System.out.println("Argument id+cl Recall   : \t" + aver_rec_RIC);
        //return new Pair(aver_pre, aver_rec);
        return 2.0 * aver_pre_RIC * aver_rec_RIC / (aver_pre_RIC + aver_rec_RIC);
    }

    public static void main(String[] args) throws JDOMException, IOException {
        GrishmanJudge.Me = true;
        GrishmanJudge gj = new GrishmanJudge();
        gj.runACE();
//        gj.Count_F1_In_semiCRF_Form();
    }

    private Pair<Double, Double> Trigger_id_cl(LinkedList<Event> events, LinkedList<Event> Grishmanevents) {
        int prec_correct = 0, rec_correct = 0;
        for (Event ans : events) {
            if (ans.trigger.contains("critical")) {
                int a = 0;
                a = a + 1;
            }
//            System.out.println(ans.trigger);
            for (Event sle : Grishmanevents) {
                if (overlap(ans.span, sle.span) > 0.7 || overlap(sle.span, ans.span) > 0.7) {
                    if (sle.eventtype.equals(ans.eventtype) && (sle.trigger.contains(ans.trigger) || ans.trigger.contains(sle.trigger))) {
                        rec_correct++;
//                        System.out.println(ans.trigger);
                        break;
                    }
                }
            }
        }
        for (Event sle : Grishmanevents) {
            for (Event ans : events) {
                if (overlap(ans.span, sle.span) > 0.7 || overlap(sle.span, ans.span) > 0.7) {
                    if (sle.eventtype.equals(ans.eventtype) && (sle.trigger.contains(ans.trigger) || ans.trigger.contains(sle.trigger))) {
                        prec_correct++;
                        break;
                    }
                }
            }
        }
        double prec = 1.0 * prec_correct / Grishmanevents.size();
        double rec = 1.0 * rec_correct / events.size();
        System.out.println(events.size() + "\t" + Grishmanevents.size() + "\t" + prec_correct + "\t" + rec_correct);
        return new Pair<>(prec, rec);
    }

    private Pair<Double, Double> Argument_id(LinkedList<Event> events, LinkedList<Event> Grishmanevents) {
        int prec_correct = 0, rec_correct = 0;
        int ansargument_num = 0, grishargument_num = 0;
        for (Event ans : events) {
            ansargument_num += ans.arguments.size();
            for (EventArgument ansa : ans.arguments) {
                boolean find = false;
//                System.out.println(ansa.data + "\t" + ans.trigger);
                for (int i = 0; i < Grishmanevents.size() && !find; i++) {
                    for (EventArgument slea : Grishmanevents.get(i).arguments) {
                        if (overlap(ans.span, Grishmanevents.get(i).span) > 0.7 || overlap(Grishmanevents.get(i).span, ans.span) > 0.7) {
                            if (Grishmanevents.get(i).eventtype.equals(ans.eventtype)) {
//                                if (slea.data.contains(ansa.data) || ansa.data.contains(slea.data)) {
                                if (overlap(slea.data, ansa.data) > 0 || overlap(ansa.data, slea.data) > 0) {
                                    find = true;
                                    rec_correct++;
//                                    System.out.println(ansa.data + "\t" + Grishmanevents.get(i).trigger);
                                    break;
                                }
                            }
                        }
                    }
                }
                if (!find) {
                    int a = 0;
                    a = a + 1;
                }
            }
        }
        for (Event sle : Grishmanevents) {
            grishargument_num += sle.arguments.size();
            for (EventArgument slea : sle.arguments) {
                boolean find = false;
                for (int i = 0; i < events.size() && !find; i++) {
                    for (EventArgument ansa : events.get(i).arguments) {
                        if (overlap(events.get(i).span, sle.span) > 0.7 || overlap(sle.span, events.get(i).span) > 0.7) {
                            if (sle.eventtype.equals(events.get(i).eventtype)) {
                                if (overlap(slea.data, ansa.data) > 0 || overlap(ansa.data, slea.data) > 0) {
                                    prec_correct++;
                                    find = true;
                                    break;
                                }
                            }
                        }
                    }
                }
                if (!find) {
                    int a = 0;
                    a = a + 1;
                }
            }
        }
        double prec = 1.0 * prec_correct / grishargument_num;
        double rec = 1.0 * rec_correct / ansargument_num;
//        System.out.println(ansargument_num + "\t" + grishargument_num + "\t" + prec_correct + "\t" + rec_correct);
        return new Pair<>(prec, rec);
    }

    private Pair<Double, Double> Argument_id_cl(LinkedList<Event> events, LinkedList<Event> Grishmanevents) {
        int prec_correct = 0, rec_correct = 0;
        int ansargument_num = 0, grishargument_num = 0;
        double threshold = 0.7;
        for (Event ans : events) {
            ansargument_num += ans.arguments.size();
            for (EventArgument ansa : ans.arguments) {
                boolean find = false;
                for (int i = 0; i < Grishmanevents.size() && !find; i++) {
                    Event sle = Grishmanevents.get(i);
                    for (EventArgument slea : sle.arguments) {
                        if (overlap(ans.span, sle.span) > threshold || overlap(sle.span, ans.span) > threshold) {
                            if (sle.eventtype.equals(ans.eventtype)) {
                                if ((overlap(slea.data, ansa.data) > 0 || overlap(ansa.data, slea.data) > 0) && slea.type.equals(ansa.type)) {
                                    rec_correct++;
                                    find = true;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        for (Event sle : Grishmanevents) {
            grishargument_num += sle.arguments.size();
            for (EventArgument slea : sle.arguments) {
                boolean find = false;
                for (int i = 0; i < events.size() && !find; i++) {
                    Event ans = events.get(i);
                    for (EventArgument ansa : ans.arguments) {
                        if (overlap(ans.span, sle.span) > threshold || overlap(sle.span, ans.span) > threshold) {
                            if (sle.eventtype.equals(ans.eventtype)) {
                                if ((overlap(slea.data, ansa.data) > 0 || overlap(ansa.data, slea.data) > 0) && slea.type.equals(ansa.type)) {
                                    prec_correct++;
                                    find = true;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        double prec = 1.0 * prec_correct / grishargument_num;
        double rec = 1.0 * rec_correct / ansargument_num;
        return new Pair<>(prec, rec);
    }

    private void Error1(LinkedList<Event> events, LinkedList<Event> Grishmanevents) {
        int count = 0, total = 0;
        for (Event ans : events) {
            loop:
            for (Event sle : Grishmanevents) {
                if (overlap(ans.span, sle.span) > 0.7 || overlap(sle.span, ans.span) > 0.7) {
                    if (ans.trigger.contains(sle.trigger) || sle.trigger.contains(ans.trigger)) {
                        total++;
                        for (EventArgument ansa : ans.arguments) {
                            boolean match = false;
                            for (EventArgument slea : sle.arguments) {
                                if (sle.eventtype.equals(ans.eventtype) && (slea.data.contains(ansa.data) || ansa.data.contains(slea.data)) && slea.type.equals(ansa.type)) {
                                    match = true;
                                    break;
                                }
                            }
                            if (!match) {
                                count++;
                                break loop;
                            }
                        }
                        break;
                    }
                }
            }
        }
        System.out.println(count + "\t" + total);
    }

    private void Error2(LinkedList<Event> events, LinkedList<Event> Grishmanevents) {
        int count = 0, total = 0;
        for (Event sle : Grishmanevents) {
            boolean eventright = false;
            loop:
            for (Event ans : events) {
                if (overlap(ans.span, sle.span) > 0.7 || overlap(sle.span, ans.span) > 0.7) {
                    if (ans.trigger.contains(sle.trigger) || sle.trigger.contains(ans.trigger)) {
                        eventright = true;
                        total++;
                        for (EventArgument slea : sle.arguments) {
                            boolean match = false;
                            for (EventArgument ansa : ans.arguments) {
                                if (sle.eventtype.equals(ans.eventtype) && (slea.data.contains(ansa.data) || ansa.data.contains(slea.data)) && slea.type.equals(ansa.type)) {
                                    match = true;
                                    break;
                                }
                            }
                            if (!match) {
                                count++;
                                break loop;
                            }
                        }
                        break;
                    }
                }
            }
        }
    }

    private void Error3(LinkedList<Event> events, LinkedList<Event> Grishmanevents) {
        int count = 0;
        int event_not_right = 0, slot_not_right = 0, totalargument = 0, right = 0;
        int slot_error = 0, slot_more = 0;
        for (Event sle : Grishmanevents) {
            totalargument += sle.arguments.size();
            int maxmatch = -1;
            Event anchor = null;
            loop:
            for (Event ans : events) {
                if (overlap(ans.span, sle.span) > 0.7 || overlap(sle.span, ans.span) > 0.7) {
                    if (ans.eventtype.equals(sle.eventtype)) {
//                    if (ans.trigger.contains(sle.trigger) || sle.trigger.contains(ans.trigger)) {
                        int Match = this.MatchArguments(sle, ans, false);
                        maxmatch = maxmatch > Match ? maxmatch : Match;
                        anchor = ans;
                    }
                }
            }
            if (maxmatch == -1) {
                event_not_right += sle.arguments.size();
            } else {
//                slot_error += anchor.arguments.size() - maxmatch;
//                slot_more += sle.arguments.size() - anchor.arguments.size();
                slot_not_right += sle.arguments.size() - maxmatch;
                right += maxmatch;
//                this.MatchArguments(sle, anchor, true);
            }
        }
        System.out.println(event_not_right + "\t" + slot_not_right + "\t" + right + "\t" + totalargument);
    }

    private int MatchArguments(Event test, Event ans, boolean print) {
        int right = 0;
        for (EventArgument slea : test.arguments) {
            for (EventArgument ansa : ans.arguments) {
                if (test.eventtype.equals(ans.eventtype) && (slea.data.contains(ansa.data) || ansa.data.contains(slea.data))) {
                    right++;
                    if (print) {
                        System.out.println(ansa.data);
                    }
                    break;
                }
            }
        }
        if (print) {
            System.out.println();
        }
        return right;
    }

    private Pair<Double, Double> Argument_id_iftyperight(LinkedList<Event> events, LinkedList<Event> Grishmanevents) {
        int prec_correct = 0, rec_correct = 0;
        int ansargument_num = 0, grishargument_num = 0;
        int totaltyperight = 0, totaltyperighttest = 0;
        for (Event ans : events) {
            ansargument_num += ans.arguments.size();
            for (EventArgument ansa : ans.arguments) {
                boolean find = false, findtrigger = false;
                loop:
                for (int i = 0; i < Grishmanevents.size() && !find; i++) {
                    if (ans.eventtype.equals(Grishmanevents.get(i).eventtype)) {
                        findtrigger = true;
                        for (EventArgument slea : Grishmanevents.get(i).arguments) {
                            if (overlap(ans.span, Grishmanevents.get(i).span) > 0.7 || overlap(Grishmanevents.get(i).span, ans.span) > 0.7) {
                                if (Grishmanevents.get(i).eventtype.equals(ans.eventtype) && (slea.data.contains(ansa.data) || ansa.data.contains(slea.data))) {
                                    find = true;
                                    rec_correct++;
                                    //    System.out.println(ansa.data);
                                    break loop;
                                }
                            }
                        }
                    }
                }
                if (findtrigger) {
                    totaltyperight++;
                }
            }
        }
        for (Event sle : Grishmanevents) {
            grishargument_num += sle.arguments.size();
            for (EventArgument slea : sle.arguments) {
                boolean find = false, findtrigger = false;
                loop:
                for (int i = 0; i < events.size() && !find; i++) {
                    if (events.get(i).eventtype.equals(sle.eventtype)) {
                        findtrigger = true;
                        for (EventArgument ansa : events.get(i).arguments) {
                            if (overlap(events.get(i).span, sle.span) > 0.7 || overlap(sle.span, events.get(i).span) > 0.7) {
                                if (sle.eventtype.equals(events.get(i).eventtype) && (slea.data.contains(ansa.data) || ansa.data.contains(slea.data))) {
                                    prec_correct++;
                                    find = true;
                                    break loop;
                                }
                            }
                        }
                    }
                }
                if (findtrigger) {
                    totaltyperighttest++;
                }
            }
        }
        double prec = 1.0 * prec_correct / totaltyperighttest;
        double rec = 1.0 * rec_correct / totaltyperight;
//        System.out.println(ansargument_num);// + "\t" + grishargument_num + "\t" + prec_correct + "\t" + rec_correct);
        return new Pair<>(prec, rec);
    }

}
