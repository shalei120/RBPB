/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package count_dep;

import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import javafx.util.Pair;
import org.jdom2.*;
import org.jdom2.input.*;

/**
 *
 * @author v-lesha
 */
public class Count_dep {

    LinkedList<Event> events;
    LinkedList<Event> Grishmanevents;
    static int only_right = 1;
    static int state = 0;
    static double threshold = 1;

    public Pair<Double, Double> runACE() throws JDOMException, IOException {
        Scanner sgmlist = new Scanner(new FileInputStream("D:\\LDC2006D06\\LDC2006D06\\Data\\LDC2006T06_Original\\data\\English\\nw\\Testfile.txt"));
        String corpusfolder = "D:\\LDC2006D06\\LDC2006D06\\Data\\LDC2006T06_Original\\data\\English\\nw\\fp1\\";
        String Grishmanout = "D:\\LDC2006D06\\LDC2006D06\\Data\\LDC2006T06_Original\\data\\English\\nw\\out\\";
        double aver_pre = 0, aver_rec = 0;
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
            Pair<Double, Double> PR = Error_Analysis_Grishman(events, Grishmanevents);
            if (only_right == 0 || PR.getKey() != 0 && PR.getValue() != 0) {
                aver_pre += PR.getKey();
                aver_rec += PR.getValue();
                filenum++;
            }
        }
        aver_pre /= filenum;
        aver_rec /= filenum;//||answer.size()<=test.size()
        System.out.println("Precision: \t" + aver_pre);
        System.out.println("Recall   : \t" + aver_rec);
        return new Pair(aver_pre, aver_rec);
    }

    public Pair<Double, Double> runTwitter() throws JDOMException, IOException {
//        Scanner sgmlist = new Scanner(new FileInputStream("D:\\LDC2006D06\\LDC2006D06\\Data\\LDC2006T06_Original\\data\\English\\nw\\sgmlist.txt"));
//        String corpusfolder = "D:\\LDC2006D06\\LDC2006D06\\Data\\LDC2006T06_Original\\data\\English\\nw\\fp1\\";
//        String Grishmanout = "D:\\LDC2006D06\\LDC2006D06\\Data\\LDC2006T06_Original\\data\\English\\nw\\out\\";
        Scanner sgmlist = new Scanner(new FileInputStream("D:\\alaneitter\\Iannotated\\sgmlist.txt"));
        String corpusfolder = "D:\\alaneitter\\Iannotated\\";
        String Grishmanout = "D:\\alaneitter\\Iannotated\\out\\";
        double aver_pre = 0, aver_rec = 0;
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
            if (events.size() < 1) {
                continue;
            }
            Pair<Double, Double> PR = Count_dep.Error_Analysis_Grishman(events, Grishmanevents);
            // if (PR.getKey() != 0 && PR.getValue() != 0) {
            aver_pre += PR.getKey();
            aver_rec += PR.getValue();
            filenum++;
            //   }
        }
        aver_pre /= filenum;
        aver_rec /= filenum;
        return new Pair(aver_pre, aver_rec);

    }

    private void Countdependencies() {
        // creates a StanfordCoreNLP object, with POS tagging, lemmatization, NER, parsing, and coreference resolution 
        Properties props = new Properties();
        props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        int normal = 0;
        int[] tag = new int[10];
        for (int i = 0; i < events.size(); i++) {
            Event e = events.get(i);
            // read some text in the text variable
            String text = e.span;// Add your text here!

            // create an empty Annotation just with the given text
            Annotation document = new Annotation(text);

            // run all Annotators on this text
            pipeline.annotate(document);

            // these are all the sentences in this document
            // a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
            List<CoreMap> sentences = document.get(SentencesAnnotation.class);
            assert sentences.size() == 1;
            CoreMap sentence = sentences.get(0);

            List<CoreLabel> get = sentence.get(TokensAnnotation.class);
            for (CoreLabel cl : get) {
                if (cl.word().equals(e.trigger)) {
                    if (cl.tag().length() >= 2 && "NN".equals(cl.tag().substring(0, 2))) {
                        System.out.println("NN    " + cl.word() + " " + cl.tag() + " " + e.span + " " + e.filename);
                        tag[0]++;
                    } else if (cl.tag().length() >= 2 && "VB".equals(cl.tag().substring(0, 2))) {
                        System.out.println("VB    " + cl.word() + " " + cl.tag() + " " + e.span + " " + e.filename);
                        tag[1]++;
                    } else if (cl.tag().length() >= 2 && "JJ".equals(cl.tag().substring(0, 2))) {
                        tag[2]++;
                    } else if (cl.tag().length() >= 2 && "PR".equals(cl.tag().substring(0, 2))) {
                        tag[3]++;
                    } else if (cl.tag().length() >= 2 && "DT".equals(cl.tag().substring(0, 2))) {
                        tag[4]++;
                    } else {
                        //  System.out.println(cl.word() + " " + cl.tag()+" "+e.span+" "+e.filename);
                    }
                }
            }
            // this is the Stanford dependency graph of the current sentence
            SemanticGraph dependencies = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);

            LinkedList<Event> Extracted_Events = GetEvents(dependencies, sentence);

            for (Event ee : Extracted_Events) {
                if (ee.trigger.equals(e.trigger)) {
                    boolean allmatched = true;
                    for (EventArgument s : e.arguments) {
                        boolean ok = false;
                        for (EventArgument s2 : ee.arguments) {
                            if (s.data.contains(s2.data)) {
                                ok = true;
                                break;
                            }
                        }
                        if (!ok) {
                            allmatched = false;
                            break;
                        }
                    }
                    if (allmatched) {
                        normal++;
                        break;
                    } else {
                        System.out.println(e.trigger);
                        System.out.println(e.span + "  " + e.filename);
                        System.out.println("gold slots");
                        for (int goldargu = 0; goldargu < e.arguments.size(); goldargu++) {
                            System.out.print(goldargu + " " + e.arguments.get(goldargu) + "   ");
                        }
                        System.out.println();
                        System.out.println("extracted slots");
                        for (int argu = 0; argu < ee.arguments.size(); argu++) {
                            System.out.print(ee.arguments.get(argu) + "   ");
                        }
                        System.out.println();
                        System.out.println();
                    }
                }
            }

            if (i % 50 == 0) {
                System.out.println(1.0 * i / events.size());
            }
        }
        System.out.println(1.0 * normal / events.size());
//        System.out.println("NN: " + 1.0 * tag[0] / events.size());
//        System.out.println("VB: " + 1.0 * tag[1] / events.size());
//        System.out.println("JJ: " + 1.0 * tag[2] / events.size());
//        System.out.println("PRP: " + 1.0 * tag[3] / events.size());
//        System.out.println("DT: " + 1.0 * tag[4] / events.size());
    }

    public LinkedList<Event> GetEvents(SemanticGraph dependencies, CoreMap sentence) {
        LinkedList<Event> res = new LinkedList<>();
        LinkedList<IndexedWord> roots = new LinkedList<>();
        List<CoreLabel> words = sentence.get(TokensAnnotation.class);
        List<GrammaticalRelation> senserel = new LinkedList<>();
        senserel.add(GrammaticalRelation.valueOf("nsubj"));
        senserel.add(GrammaticalRelation.valueOf("dobj"));
        for (CoreLabel word : words) {
            if (word.tag().length() >= 2 && ("VB".equals(word.tag().substring(0, 2)) || "NN".equals(word.tag().substring(0, 2)))) {
                IndexedWord iword = new IndexedWord(word);
                roots.add(iword);
            }
        }
        for (IndexedWord word : roots) {
            Event e = new Event();
            e.trigger = word.word();
            try {
                Set<IndexedWord> children = dependencies.getChildren(word);
                children.stream().forEach((iw) -> {
                    e.arguments.add(new EventArgument(iw.word(), ""));
                });
                if (dependencies.inDegree(word) > 0) {
                    IndexedWord parent = dependencies.getParent(word);
                    if (parent.tag().length() >= 2 && "VB".equals(parent.tag().substring(0, 2))) {
                        Set<IndexedWord> children1 = dependencies.getChildrenWithRelns(parent, senserel);
                        children1.remove(word);
                        children1.stream().forEach((iw) -> {
                            e.arguments.add(new EventArgument(iw.word(), ""));
                        });
                    } else {
                        e.arguments.add(new EventArgument(dependencies.getParent(word).word(), ""));
                    }
                }
            } catch (java.lang.IllegalArgumentException error) {
                continue;
            }
            res.add(e);
        }
        return res;
    }

    public Count_dep() {
        events = new LinkedList<>();
        Grishmanevents = new LinkedList<>();
    }

    public static void main(String[] args) throws JDOMException, IOException {
        // TODO code application logic here
        Count_dep cd = new Count_dep();
        cd.runACE();
        //cd.runTwitter();
    }

    public static LinkedList<Event> ReadEvents(File f) throws JDOMException, IOException {
        LinkedList<Event> res = new LinkedList<>();
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(f);
        Element foo = doc.getRootElement();
        List<Element> one_document = foo.getChildren();
        for (Element one_document1 : one_document) {
            List<Element> ERE = one_document1.getChildren();
            for (Element e : ERE) {
                if ("event".equals(e.getName())) {
                    List<Element> mentions = e.getChildren("event_mention");
                    for (Element m : mentions) {
                        Event eve = new Event();
                        Element charseq;
                        Element ldcscpope = m.getChild("ldc_scope");
                        charseq = ldcscpope.getChild("charseq");
                        eve.span = charseq.getText().replace("\n", " ");
                        Element anchor = m.getChild("anchor");
                        charseq = anchor.getChild("charseq");
                        eve.trigger = charseq.getText();
                        if (eve.trigger.equalsIgnoreCase("saturday")) {
                            int a = 0;
                            a = a + 1;
                        }
                        eve.eventtype = e.getAttribute("SUBTYPE").getValue();
                        eve.eventlargetype=e.getAttribute("TYPE").getValue();
                        List<Element> arguments = m.getChildren("event_mention_argument");
                        for (Element argu : arguments) {
                            String argumentstr = argu.getChild("extent").getChild("charseq").getText();
                            if ("U.S".equals(argumentstr) || "U.N".equals(argumentstr) || "Feb".equals(argumentstr)) {
                                argumentstr += ".";
                            }
                            if (argumentstr.equalsIgnoreCase("Basra")) {
                                int a = 0;
                                a = a + 1;
                            }
                            eve.arguments.add(new EventArgument(argumentstr, argu.getAttributeValue("ROLE")));
                        }
                        eve.filename = f.getName();
                        res.add(eve);
                    }

                }
            }
        }
        return res;
    }

    public static LinkedList<Event> ReadGrishmanEvents(File f) throws JDOMException, IOException {
        LinkedList<Event> res = new LinkedList<>();
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(f);
        Element foo = doc.getRootElement();
        List<Element> one_document = foo.getChildren();
        for (Element one_document1 : one_document) {
            List<Element> ERE = one_document1.getChildren();
            for (Element e : ERE) {
                if ("event".equals(e.getName())) {
                    List<Element> mentions = e.getChildren("event_mention");
                    for (Element m : mentions) {
                        Event eve = new Event();
                        eve.filename = f.getName();
                        Element charseq;
                        Element anchor = m.getChild("anchor");
                        charseq = anchor.getChild("charseq");
                        eve.span = m.getChild("extent").getChild("charseq").getText();
                        eve.trigger = charseq.getText();
                        eve.eventtype = e.getAttribute("SUBTYPE").getValue();
                        List<Element> arguments = m.getChildren("event_mention_argument");
                        for (Element argu : arguments) {
                            eve.arguments.add(new EventArgument(argu.getChild("extent").getChild("charseq").getText(), argu.getAttributeValue("ROLE")));
                        }
                        //   eve.filename = f.getName();
                        res.add(eve);
                    }

                }

            }
        }
        return res;
    }

    public static Pair<Double, Double> Error_Analysis_Grishman(LinkedList<Event> events, LinkedList<Event> Grishmanevents) {
        int correctgold = 0, correctgrish = 0, rolemiss = 0;
        int gold = 0, grish = 0;
        int goldright = 0, grishright = 0;
        for (Event golde : events) {
            int triggerright = 0, argunum = 0, keyargunum = 0;
            boolean good = false;
            for (int i = 0; i < Grishmanevents.size(); i++) {
                Event grishe = Grishmanevents.get(i);
                if ((golde.trigger.contains(grishe.trigger) || grishe.trigger.contains(golde.trigger)) && golde.eventtype.equals(grishe.eventtype)) {
                    Pair<Integer, Integer> num = new Pair(0, 0);
                    triggerright = 1;
                    argunum = golde.arguments.size();
//                    if (Grishman_Argument_Match(golde.arguments, grishe.arguments, 0) == 0 && Grishman_Argument_Match(golde.arguments, grishe.arguments, 2) > 0) {
//                        int s = 0;
//                        s = s + 1;
//                        Grishman_Argument_Match(golde.arguments, grishe.arguments, 0);
//                        Grishman_Argument_Match(golde.arguments, grishe.arguments, 2);
//                    }
                    num = Grishman_Argument_Match_rec(golde.arguments, grishe.arguments, state);
                    keyargunum = num.getValue();
                    if (golde.arguments.isEmpty() || num.getKey() > 0) {
                        // if(grishe.eventtype.equals(golde.eventtype)){
                        correctgold += state == 1 || state == 3 ? num.getKey() : 1;
                        good = true;
                        break;
                    } else {
                        int a = 0;
                        a = a + 1;
                    }
                }
            }
            if (triggerright >= 0 && !good) {
                int a = 0;
                a = a + 1;
            }
            goldright += triggerright;
            gold += state == 3 ? keyargunum : (only_right == 1 ? argunum : golde.arguments.size());
        }
        for (Event grishe : Grishmanevents) {
            int triggerright = 0, argunum = 0, keyargunum = 0;
            for (Event golde : events) {
                Pair<Integer, Integer> num = new Pair(0, 0);
                if (golde.trigger.contains(grishe.trigger) || grishe.trigger.contains(golde.trigger)) {
                    triggerright = 1;
                    argunum = grishe.arguments.size();
                    num = Grishman_Argument_Match_prec(golde.arguments, grishe.arguments, state);
                    keyargunum = num.getValue();

                    if (golde.arguments.isEmpty() || num.getKey() > 0) {
                        //if(grishe.eventtype.equals(golde.eventtype)){
                        correctgrish += state == 1 || state == 3 ? num.getKey() : 1;
                        Grishman_Argument_Match_prec(golde.arguments, grishe.arguments, state);
                        break;
                    }

                }
            }

            grishright += triggerright;
            grish += state == 3 ? keyargunum : (only_right == 1 ? argunum : grishe.arguments.size());
        }
        double precision = 0, recall = 0;
        if (state == 0 || state == 2) {
            if (only_right == 0) {
                precision = 1.0 * correctgrish / Grishmanevents.size();
                recall = 1.0 * correctgold / events.size();
            } else {
                precision = grishright == 0 ? 0 : 1.0 * correctgrish / grishright;
                recall = goldright == 0 ? 0 : 1.0 * correctgold / goldright;
            }
        } else if (state == 1 || state == 3) {
            precision = grish == 0 ? 0 : 1.0 * correctgrish / grish;
            recall = gold == 0 ? 0 : 1.0 * correctgold / gold;
        }
//        System.out.println("Precision: \t" + precision);
//        System.out.println("Recall   : \t" + recall);
//        System.out.println("F1       : \t" + 2 * precision * recall / (precision + recall));
//        System.out.println("Rolemiss: \t" + 1.0 * rolemiss / correctgrish);
        return new Pair(precision, recall);
    }
    static final String[] Roles = {
        "Victim", "Target", "Instrument", "Person", "Agent", "Attacker"
    };
    static final LinkedList<String> Considered_roles = new LinkedList<>();

    static {
        Considered_roles.addAll(Arrays.asList(Roles));
    }

    public static Pair<Integer, Integer> Grishman_Argument_Match_rec(List<EventArgument> answer, List<EventArgument> test, int sta) {
        int matchnum = 0, hastime = 0;
        int state = sta;

        int res = 0;
        int totalkeyrole = 0;
        if (state == 0) {
            for (EventArgument ans : answer) {
//                if (ans.type.contains("Time")) {
//                    hastime = 1;
//                } else {
                for (EventArgument testarg : test) {
                    if (ans.data.contains(testarg.data) || testarg.data.contains(ans.data)) {
                        matchnum++;
                        break;
                    }
                }
                //    }
            }

            res = (1.0 * matchnum / (answer.size() - hastime)) >= threshold ? 1 : 0;
        } else if (state == 1) {
            for (EventArgument ans : answer) {
                for (EventArgument testarg : test) {
                    if (ans.data.contains(testarg.data) || testarg.data.contains(ans.data)) {
                        matchnum++;
                        break;
                    }
                }
            }
            res = matchnum;
        } else if (state == 2) {
            int total = 0;
            for (EventArgument ans : answer) {
                if (Considered_roles.contains(ans.type)) {
                    total++;
                    for (EventArgument testarg : test) {
                        if (ans.data.contains(testarg.data) || testarg.data.contains(ans.data)) {
                            matchnum++;
                            break;
                        }
                    }
                }
            }
            res = matchnum == total ? 1 : 0;
        } else if (state == 3) {
            for (EventArgument ans : answer) {
                if (Considered_roles.contains(ans.type)) {
                    totalkeyrole++;
                    for (EventArgument testarg : test) {
                        if (ans.data.contains(testarg.data) || testarg.data.contains(ans.data)) {
                            matchnum++;
                            break;
                        }
                    }
                } else {
                    for (EventArgument testarg : test) {
                        if (ans.data.contains(testarg.data) || testarg.data.contains(ans.data)) {
                            int sf = 0;
                            sf = sf + 1;
                            break;
                        }
                    }
                }
            }
            res = matchnum;
        }

        return new Pair(res, totalkeyrole);
    }

    public static Pair<Integer, Integer> Grishman_Argument_Match_prec(List<EventArgument> answer, List<EventArgument> test, int sta) {
        int matchnum = 0, hastime = 0;
        int state = sta;

        int res = 0;
        int totalkeyrole = 0;
        if (state == 0) {
            for (EventArgument ans : answer) {
                if (ans.type.contains("Time")) {
                    hastime = 1;
                } else {
                    for (EventArgument testarg : test) {
                        if (ans.data.contains(testarg.data) || testarg.data.contains(ans.data)) {
                            matchnum++;
                            break;
                        }
                    }
                }
            }
            res = (1.0 * matchnum / (test.size() - hastime)) >= threshold ? 1 : 0;
        } else if (state == 1) {
            for (EventArgument testarg : test) {
                for (EventArgument ans : answer) {
                    if (ans.data.contains(testarg.data) || testarg.data.contains(ans.data)) {
                        //    if (ans.type.equals(testarg.type)) {
                        matchnum++;
                        break;
                        //   }
                    }
                }
            }
            res = matchnum;
        } else if (state == 2) {
            int total = 0;
            for (EventArgument ans : answer) {
                if (Considered_roles.contains(ans.type)) {
                    total++;
                    for (EventArgument testarg : test) {
                        if (ans.data.contains(testarg.data) || testarg.data.contains(ans.data)) {
                            matchnum++;
                            break;
                        }
                    }
                }
            }
            res = matchnum == total ? 1 : 0;
        } else if (state == 3) {
            for (EventArgument ans : answer) {
                if (Considered_roles.contains(ans.type)) {
                    totalkeyrole++;
                    for (EventArgument testarg : test) {
                        if (ans.data.contains(testarg.data) || testarg.data.contains(ans.data)) {
                            matchnum++;
                            break;
                        }
                    }
                }
            }
            res = matchnum;
        }

        return new Pair(res, totalkeyrole);
    }

}
