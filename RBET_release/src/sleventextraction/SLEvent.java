/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sleventextraction;

import AceJet.AceEvent;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 *
 * @author v-lesha
 */
class SLEvent {

    static double GetSimilarity(SLEvent A, SLEvent B) {
        double predicatedis = SLMath.Getcosdis(A.averwordvec, B.averwordvec);
        double triggerdis = -1;
        if (A.hastriggervec && B.hastriggervec) {
            triggerdis = SLMath.Getcosdis(A.aver_trigger_wordvec, B.aver_trigger_wordvec);
        }
        return predicatedis > triggerdis ? predicatedis : triggerdis;
    }

    String span;
    String trigger, eventtype;
    LinkedList<SLEntity> arguments;
    String filename;
    LinkedList<Double> averwordvec, aver_trigger_wordvec;
    double confidence;
    boolean hastriggervec;
    double Min_cosdisRadius;
    Set<Integer> Entityid;

    public SLEvent() {
        arguments = new LinkedList<>();
        averwordvec = new LinkedList<>();
        aver_trigger_wordvec = new LinkedList<>();
        for (int i = 0; i < SLEventExtraction.dim; i++) {
            averwordvec.add(0.0);
            aver_trigger_wordvec.add(0.0);
        }
        Min_cosdisRadius = 1;
        hastriggervec = false;
        Entityid = new HashSet<>();
    }

    SLEvent(SLEntity e, int id) {
        this();
        arguments.add(e);
        averwordvec = e.wordvec;
        hastriggervec = e.hastriggervec;
        aver_trigger_wordvec = e.Triggers_aver_wordvec;
        Entityid.add(id);

    }

    SLEvent(SLEvent e) {
        this();
        arguments.addAll(e.arguments);
        for (int i = 0; i < averwordvec.size(); i++) {
            averwordvec.set(i, e.averwordvec.get(i));
        }
        Min_cosdisRadius = e.Min_cosdisRadius;
        hastriggervec = e.hastriggervec;
        for (int i = 0; i < averwordvec.size(); i++) {
            aver_trigger_wordvec.set(i, e.aver_trigger_wordvec.get(i));
        }
        Entityid.addAll(e.Entityid);
    }

    SLEvent(AceEvent event) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void addArgument(SLEntity e) {

        LinkedList<Double> evec = e.wordvec;
        LinkedList<Double> tvec = e.Triggers_aver_wordvec;
        for (int i = 0; i < SLEventExtraction.dim; i++) {
            averwordvec.set(i, (averwordvec.get(i) * arguments.size() + evec.get(i)) / (arguments.size() + 1));
            if (e.hastriggervec) {
                aver_trigger_wordvec.set(i, (aver_trigger_wordvec.get(i) * arguments.size() + tvec.get(i)) / (arguments.size() + 1));
            }
        }
        this.hastriggervec = hastriggervec | e.hastriggervec;
        arguments.add(e);
        Min_cosdisRadius = 1;
        for (int i = 0; i < arguments.size(); i++) {
            double per_cosdisRadius = -1;
            double cosdis = SLMath.Getcosdis(averwordvec, arguments.get(i).wordvec);
            per_cosdisRadius = per_cosdisRadius > cosdis ? per_cosdisRadius : cosdis;
            if (hastriggervec && arguments.get(i).hastriggervec) {
                double tricosdis = SLMath.Getcosdis(aver_trigger_wordvec, arguments.get(i).Triggers_aver_wordvec);
                per_cosdisRadius = per_cosdisRadius > tricosdis ? per_cosdisRadius : tricosdis;
            }
            Min_cosdisRadius = Min_cosdisRadius < per_cosdisRadius ? Min_cosdisRadius : per_cosdisRadius;
        }
    }

    public void addAllArgument(SLEvent eb) {
        this.Entityid.addAll(eb.Entityid);
        LinkedList<SLEntity> le = eb.arguments;
        LinkedList<LinkedList<Double>> evecs = new LinkedList<>();
        LinkedList<LinkedList<Double>> tvecs = new LinkedList<>();
        int valid_trigger = 0;
        for (int i = 0; i < le.size(); i++) {
            evecs.add(le.get(i).wordvec);
            tvecs.add(le.get(i).Triggers_aver_wordvec);
            this.hastriggervec = hastriggervec | le.get(i).hastriggervec;
            if (le.get(i).hastriggervec) {
                valid_trigger++;
            }
        }
        for (int i = 0; i < SLEventExtraction.dim; i++) {
            double newdim = averwordvec.get(i) * arguments.size();
            for (int j = 0; j < le.size(); j++) {
                newdim += evecs.get(j).get(i);
            }
            averwordvec.set(i, newdim / (arguments.size() + le.size()));

            double newtridim = aver_trigger_wordvec.get(i) * arguments.size();
            for (int j = 0; j < le.size(); j++) {
                if (le.get(j).hastriggervec) {
                    newtridim += tvecs.get(j).get(i);
                }
            }
            aver_trigger_wordvec.set(i, newtridim / (arguments.size() + valid_trigger));
        }
        arguments.addAll(le);
        Min_cosdisRadius = 1;
        for (int i = 0; i < arguments.size(); i++) {
            double per_cosdisRadius = -1;
            double cosdis = SLMath.Getcosdis(averwordvec, arguments.get(i).wordvec);
            per_cosdisRadius = per_cosdisRadius > cosdis ? per_cosdisRadius : cosdis;
            if (hastriggervec && arguments.get(i).hastriggervec) {
                double tricosdis = SLMath.Getcosdis(aver_trigger_wordvec, arguments.get(i).Triggers_aver_wordvec);
                per_cosdisRadius = per_cosdisRadius > tricosdis ? per_cosdisRadius : tricosdis;
            }
            Min_cosdisRadius = Min_cosdisRadius < per_cosdisRadius ? Min_cosdisRadius : per_cosdisRadius;
        }
    }

    @Override
    public String toString() {
        String s = "";
        s = trigger + "***" + span + "\n";
        s += arguments.size() + "\n";
        for (int i = 0; i < arguments.size(); i++) {
            s += arguments.get(i).toString() + "\n";
        }
        return s; //To change body of generated methods, choose Tools | Templates.
    }

}
