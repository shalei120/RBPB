/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sleventextraction;

import AceJet.AceEntity;
import AceJet.AceMention;
import AceJet.AceValue;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;
import java.util.LinkedList;
import java.util.List;
import static sleventextraction.SLEventTypeClassifier.ParseSentence;

/**
 *
 * @author v-lesha
 */
public class SLEntity {

    public String content;
    public IndexedWord predicate, Grand_predicate;
    public LinkedList<String> Triggers;
    // public LinkedList<Double> Triggerweight;
    public String dep;
    /**
     * wordvec : the wordvec of predicate \n headwordvec : the wordvec of head
     * word \n Grandwordvec : the wordvec of grand predicate
     */
    public LinkedList<Double> wordvec, headwordvec, Grandwordvec;
    public LinkedList<Double> Triggers_aver_wordvec;
    public boolean hastriggervec;
    public IndexedWord head;
    public CoreMap span;
    public LinkedList<Double> dep_lookup;
    public static int depnumber = 81;
    public LinkedList<IndexedWord> ContentIws;

    public String isArg;
    public double argProb;
    public String role;
    public double roleProb[] = new double[10];
    public String ground;
    public String entitytype;
    public String entitysubtype;
    public String mentiontype;

    public SLEntity() {
        wordvec = new LinkedList<>();
        Triggers = new LinkedList<>();
        headwordvec = new LinkedList<>();
        Grandwordvec = new LinkedList<>();
        Triggers_aver_wordvec = new LinkedList<>();
        hastriggervec = false;
        dep_lookup = new LinkedList<>();
        for (int i = 0; i < depnumber; i++) {
            dep_lookup.add(0.0);
        }

        for (int i = 0; i < SLEventExtraction.dim; i++) {
            Triggers_aver_wordvec.add(0.0);
        }
        ContentIws = new LinkedList<>();
    }

    public SLEntity(String content) {
        this.content = content;
        hastriggervec = false;
        dep = "";
    }

    public SLEntity(AceMention m, CoreMap senCM, SemanticGraph senSG) {
        this();
        isArg = m.isArg;
        argProb = m.argProb;
        role = m.role;
        if (m.getParent() instanceof AceJet.AceEntity) {
            this.entitytype = ((AceEntity) m.getParent()).type;
            this.entitysubtype = ((AceEntity) m.getParent()).subtype;
        } else if (m.getParent() instanceof AceJet.AceTimex) {
            this.entitytype = "";
            this.entitysubtype = "";
        } else if (m.getParent() instanceof AceJet.AceValue) {
            this.entitytype = ((AceValue) m.getParent()).type;
            this.entitysubtype = ((AceValue) m.getParent()).subtype;
        } else {
            this.entitytype = "";
            this.entitysubtype = "";
        }
        this.mentiontype = m.getType();

        System.arraycopy(m.roleProb, 0, roleProb, 0, m.roleProb.length);
        ground = m.ground;
        span = senCM;
        SemanticGraph totaldep = span.get(SemanticGraphCoreAnnotations.CollapsedDependenciesAnnotation.class);

        this.content = m.text.trim();
        if(m.text.charAt(0)=='\"'){
            this.content=m.text.substring(1).trim();
        }
        if ("s\nb".equals(this.content)) {
            this.content = "his brother";
        } else if (" f".equals(this.content)) {
            this.content = "foreign";
        } else if ("-l".equals(this.content)) {
            this.content = "US-led";
        } else if ("s a".equals(this.content)) {
            if (span.toString().contains("Arafat's administration")) {
                this.content = "Arafat's administration";
            } else if (span.toString().contains("bus attack")) {
                this.content = "bus attack";
            }
        } else if ("33-month".equals(this.content)) {
            this.content = "33-month-old";
        } else if ("U.S".equals(this.content)) {
            this.content = "U.S.";
        } else if ("four-day".equals(this.content)) {
            this.content = "four-day-old";
        } else if ("U.N".equals(this.content)) {
            this.content = "U.N.";
        } else if ("33-year".equals(this.content)) {
            this.content = "33-year-old";
        }
        Annotation document = ParseSentence(this.content);
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);

        CoreMap cm = sentences.get(0);
        int pathlength = -1, imin = 1000;
        for (int i = 0; i < senCM.get(TokensAnnotation.class).size(); i++) {
            IndexedWord debug = new IndexedWord(senCM.get(TokensAnnotation.class).get(i));
            boolean canmatch = true;
            for (int j = 0; j < cm.get(TokensAnnotation.class).size(); j++) {
                IndexedWord iw = new IndexedWord(senCM.get(TokensAnnotation.class).get(i + j));
                IndexedWord shortiw = new IndexedWord(cm.get(TokensAnnotation.class).get(j));
                if (!iw.word().equals(shortiw.word())) {
                    if (SLEventExtraction.overlap(iw.word(), shortiw.word()) <= 0||Double.isNaN(SLEventExtraction.overlap(iw.word(), shortiw.word()))) {
                        canmatch = false;
                        break;
                    }
                }
            }
            if (canmatch) {
                for (int j = 0; j < cm.get(TokensAnnotation.class).size(); j++) {
                    IndexedWord iw = new IndexedWord(senCM.get(TokensAnnotation.class).get(i + j));
                    this.ContentIws.add(iw);
                    try {
                        pathlength = totaldep.getPathToRoot(iw).size();
                    } catch (java.lang.IllegalArgumentException err) {
                        pathlength = 100;
                    }
                    if (imin > pathlength) {
                        imin = pathlength;
                        this.head = iw;
                    }
                }
                break;
            }
        }
        if (this.head == null) {
            return;
        }
            this.predicate = totaldep.getParent(this.head);
        if (this.predicate == null) {
            this.predicate = this.head;
        } else {
            IndexedWord curr = head;
            dep = totaldep.getEdge(predicate, curr).getRelation().getShortName();
            if (totaldep.getEdge(predicate, curr).getRelation().getSpecific() != null) {
                dep += "_" + totaldep.getEdge(predicate, curr).getRelation().getSpecific();
            }
        }

    }

    @Override
    public String toString() {
        String s = "";
        if (predicate != null) {
            s = content + "\t" + dep + "\t" + predicate.word() + "\t";
            if (Grand_predicate != null) {
                s += Grand_predicate.word();
            }
        } else {
            s = content;
        }
        return s;
    }
  
}
