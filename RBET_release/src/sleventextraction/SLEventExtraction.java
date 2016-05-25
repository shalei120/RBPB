/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sleventextraction;

import Jet.JetTest;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.BasicDependenciesAnnotation;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedDependenciesAnnotation;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.util.CoreMap;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.util.Pair;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

/**
 *
 * @author v-lesha
 */
public class SLEventExtraction {

    static {
        sentences = new LinkedList<>();
        sentenceCoreMaps = new LinkedList<>();
        sentenceSemanticGraphs = new LinkedList<>();
        if (SLEventTypeClassifier.pipeline != null) {
            pipeline = SLEventTypeClassifier.pipeline;
        } else {
            Properties props = new Properties();
            props.put("annotators", "tokenize, ssplit,pos,lemma, parse");
            pipeline = new StanfordCoreNLP(props);
        }
    }

    public static void parseFile(String textFile) {
        try {
            sentences.clear();
            sentenceCoreMaps.clear();
            sentenceSemanticGraphs.clear();
            SAXBuilder builder = new SAXBuilder();
            Document doc = builder.build(textFile);
            Element foo = doc.getRootElement();
            String text = foo.getChild("BODY").getChild("TEXT").getText();
            int titleend = text.indexOf("\n\n");
            text = text.substring(titleend + 1).replace("\n\n", ".  ").replace("...", " ").replace("..", ".");
            Annotation document = new Annotation(text);
            pipeline.annotate(document);
            List<CoreMap> coremaps = document.get(CoreAnnotations.SentencesAnnotation.class);
            for (CoreMap cm : coremaps) {
                sentences.add(cm.toString());
                sentenceCoreMaps.add(cm);
                sentenceSemanticGraphs.add(cm.get(BasicDependenciesAnnotation.class));
            }
        } catch (JDOMException ex) {
            Logger.getLogger(SLEventExtraction.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(SLEventExtraction.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * @param args the command line arguments
     */
    static StanfordCoreNLP pipeline;
    public static int dim;
    public static Map<String, LinkedList<Double>> word2vec;
    public static LinkedList<String> sentences;
    public static LinkedList<CoreMap> sentenceCoreMaps;
    public static LinkedList<SemanticGraph> sentenceSemanticGraphs;

    public static int GetsenPos(String text) {
        for (int i = 0; i < sentences.size(); i++) {
            String s = sentences.get(i);
            if (overlap(text, s) > 0.8 || overlap(s, text) > 0.8) {
                return i;
            }
        }
        return -1;
    }

    public static void ConstructWordVec() throws IOException {
        System.out.println("Starting Parsing SL...");

        try {
            word2vec = readwordvec();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(SLEventExtraction.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void run() throws FileNotFoundException, JDOMException, IOException {
        Scanner sgmlist = new Scanner(new FileInputStream("D:\\LDC2006D06\\LDC2006D06\\Data\\LDC2006T06_Original\\data\\English\\nw\\sgmlist.txt"));
        String corpusfolder = "D:\\LDC2006D06\\LDC2006D06\\Data\\LDC2006T06_Original\\data\\English\\nw\\fp1\\";
        String output = "D:\\EventExtraction\\EventCandidate_";
        double aver_pre = 0, aver_rec = 0;
        int filenum = 0;
        while (sgmlist.hasNext()) {
            String filename = sgmlist.next();
            System.out.println(filename);
            filename = filename.substring(0, filename.length() - 3);
            File corpus = new File(corpusfolder + filename + "sgm");
            LinkedList<SLEvent> Events = GetEvents(corpus, pipeline, "D:\\PatternEvent\\Pattern_" + filename + "txt");
            PrintEvents(output + filename + "txt", Events);
        }
    }

    public static void main(String[] args) throws FileNotFoundException, JDOMException, IOException {
        // TODO code application logic here
        SLEventExtraction sle = new SLEventExtraction();
        sle.run();

    }

    public static LinkedList<SLEvent> GetEvents(File documentfile, StanfordCoreNLP pipeline, String pefile) throws JDOMException, IOException {
        LinkedList<SLEvent> eventlist = new LinkedList<>();
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(documentfile);
        Element foo = doc.getRootElement();
        String text = foo.getChild("BODY").getChild("TEXT").getText();

        int titleend = text.indexOf("\n\n");
        text = text.substring(titleend + 1).replace("\n\n", ".  ");

        Annotation document = new Annotation(text);

        // run all Annotators on this text
        pipeline.annotate(document);

        // these are all the sentences in this document
        // a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        PrintStream patternps = new PrintStream(new FileOutputStream(pefile));
        for (CoreMap cm : sentences) {
            SemanticGraph dependencies = cm.get(CollapsedDependenciesAnnotation.class);
            List<SLEntity> le = BPGetNPs(dependencies);
//            LinkedList<SLEvent> pevents = UsePatterns(cm.toString());
//            for (SLEvent slee : pevents) {
//                patternps.println(slee);
//            }
//            AddTriggers(le, pevents);
            Lookup_All_wordvec(le);
            if (le.size() < 2) {
                continue;
            }
            List<SLEvent> events = Cluster4events(le);
            if (events.size() >= 2) {
                events = FuzzyCluster(events, le);
            }
            for (int i = 0; i < events.size(); i++) {
                String sp = cm.toString();
                String[] split = sp.split("[\\n| ]");
                events.get(i).span = String.join("|", split);
//                if (events.get(i).arguments.size() < 2) {
//                    events.remove(i);
//                    i--;
//                }
            }
            eventlist.addAll(events);
        }
        return eventlist;
    }

    public static List<SLEntity> BPGetNPs(SemanticGraph dependencies) {
        List<SLEntity> res = new LinkedList<>();
        Set<IndexedWord> done_tokens = new HashSet<>();
        Queue<IndexedWord> roots = new LinkedList(dependencies.getRoots());
        Map<IndexedWord, Pair<String, IndexedWord>> modified_dep = new HashMap<>();
        while (!roots.isEmpty()) {
            IndexedWord curr = roots.poll();
            roots.addAll(dependencies.getChildren(curr));
            if ((curr.tag().contains("NN") || curr.tag().contains("CD") || curr.tag().contains("PRP")
                    || curr.tag().contains("JJ") || curr.tag().contains("RB") || curr.tag().contains("FW") || curr.tag().contains("DT")
                    || dependencies.getParent(curr) != null && "csubj".equals(dependencies.getEdge(dependencies.getParent(curr), curr).getRelation().getShortName())
                    || dependencies.getParent(curr) != null && "nsubj".equals(dependencies.getEdge(dependencies.getParent(curr), curr).getRelation().getShortName())
                    || dependencies.getParent(curr) != null && "nsubjpass".equals(dependencies.getEdge(dependencies.getParent(curr), curr).getRelation().getShortName()))
                    && !done_tokens.contains(curr)) {
                SLEntity e = new SLEntity();
                e.head = curr;
                e.predicate = dependencies.getParent(curr);
                if (e.predicate == null) {                // e is root
                    e.predicate = e.head;
                    e.content = GetNPstring(curr, dependencies, done_tokens);
                    e.dep = "self";
                } else {
                    e.content = GetNPstring(curr, dependencies, done_tokens);
                    try {
                        e.dep = dependencies.getEdge(e.predicate, curr).getRelation().getShortName();
                        if (dependencies.getEdge(e.predicate, curr).getRelation().getSpecific() != null) {
                            e.dep += "_" + dependencies.getEdge(e.predicate, curr).getRelation().getSpecific();
                        }
                        if ("appos".equals(e.dep) || "conj_and".equals(e.dep)) {
                            if (modified_dep.containsKey(e.predicate)) {
                                e.dep = modified_dep.get(e.predicate).getKey();
                                e.predicate = modified_dep.get(e.predicate).getValue();
                                modified_dep.put(curr, new Pair<>(e.dep, e.predicate));
                            } else {
                                e.dep = dependencies.getEdge(dependencies.getParent(e.predicate), e.predicate).getRelation().getShortName();
                                if (dependencies.getEdge(dependencies.getParent(e.predicate), e.predicate).getRelation().getSpecific() != null) {
                                    e.dep += "_" + dependencies.getEdge(dependencies.getParent(e.predicate), e.predicate).getRelation().getSpecific();
                                }
                                e.predicate = dependencies.getParent(e.predicate);
                                modified_dep.put(curr, new Pair<>(e.dep, e.predicate));
                            }
                        } else if (e.predicate.tag().contains("NN") || e.predicate.tag().contains("PRP") || e.predicate.tag().contains("CD") || e.predicate.tag().contains("RB")) {
                            if (modified_dep.containsKey(e.predicate)) {
                                e.predicate = modified_dep.get(e.predicate).getValue();
                                modified_dep.put(curr, new Pair<>(e.dep, e.predicate));
                            } else {
                                if (dependencies.getParent(e.predicate) != null) {
                                    e.predicate = dependencies.getParent(e.predicate);
                                    modified_dep.put(curr, new Pair<>(e.dep, e.predicate));
                                }
                            }
                        }
                    } catch (java.lang.NullPointerException err) {
                        continue;
                    }
                }
                if (modified_dep.containsKey(e.predicate)) {
                    e.Grand_predicate = modified_dep.get(e.predicate).getValue();
                } else {
                    e.Grand_predicate = dependencies.getParent(e.predicate);
                    if (e.Grand_predicate != null) {
                        if (e.Grand_predicate.tag().contains("NN") || e.Grand_predicate.tag().contains("PRP") || e.Grand_predicate.tag().contains("CD") || e.Grand_predicate.tag().contains("RB")) {
                            if (modified_dep.containsKey(e.Grand_predicate)) {
                                e.Grand_predicate = modified_dep.get(e.Grand_predicate).getValue();
                            } else {
                                if (dependencies.getParent(e.Grand_predicate) != null) {
                                    e.Grand_predicate = dependencies.getParent(e.Grand_predicate);
                                }
                            }
                        }
                    }
                }
                res.add(e);
            }
        }
        return res;
    }

    public static String GetNPstring(IndexedWord curr, SemanticGraph dependencies, Set<IndexedWord> done_tokens) {
        String res = "";
        LinkedList<IndexedWord> children = new LinkedList<>();
        children.addAll(dependencies.getChildrenWithReln(curr, GrammaticalRelation.valueOf("amod")));
        children.addAll(dependencies.getChildrenWithReln(curr, GrammaticalRelation.valueOf("det")));
        children.addAll(dependencies.getChildrenWithReln(curr, GrammaticalRelation.valueOf("nn")));
        children.addAll(dependencies.getChildrenWithReln(curr, GrammaticalRelation.valueOf("num")));
        children.addAll(dependencies.getChildrenWithReln(curr, GrammaticalRelation.valueOf("poss")));
        children.add(curr);
        Collections.sort(children, new Comparator<IndexedWord>() {

            @Override
            public int compare(IndexedWord o1, IndexedWord o2) {
                return o1.index() - o2.index();
            }
        });
        int start = children.indexOf(curr);
        int end = start;
        while (start > 0 && children.get(start - 1).index() == children.get(start).index() - 1) {
            start--;
        }
        while (end < children.size() - 1 && children.get(end + 1).index() == children.get(end).index() + 1) {
            end++;
        }
        for (int i = start; i <= end; i++) {
            done_tokens.add(children.get(i));
            res += children.get(i).word();
            if (i != end) {
                res += ' ';
            }
        }

        return res;
    }

    public static List<SLEvent> Cluster4events(List<SLEntity> le) {
        double THRESHOLD = 0.9;

        LinkedList<SLEvent> res = new LinkedList<>(), record;
        for (int i = 0; i < le.size(); i++) {
            res.add(new SLEvent(le.get(i), i));
        }

        while (true) {
            record = new LinkedList<>();

            for (int i = 0; i < res.size(); i++) {
                try {
                    SLEvent add = new SLEvent(res.get(i));
                    record.add(add);
                } catch (java.lang.NullPointerException err) {
                    int a = 0;
                    a = a + 1;
                }
            }

            double[][] cosdis = new double[res.size()][res.size()];
            for (int i = 0; i < res.size(); i++) {
                for (int j = i + 1; j < res.size(); j++) {
                    cosdis[i][j] = SLEvent.GetSimilarity(res.get(i), res.get(j));
                }
            }
            Pair<Integer, Integer> merge = GetMaxCos(cosdis, res.size());
            try {
                res.get(merge.getKey()).addAllArgument(res.get(merge.getValue()));

                res.remove(merge.getValue().intValue());
            } catch (java.lang.IndexOutOfBoundsException err) {
                err.printStackTrace();
            }

            boolean stop = false;
            for (int i = 0; i < res.size(); i++) {
                if (res.get(i).Min_cosdisRadius < THRESHOLD) {
                    stop = true;
                    break;
                }
            }
            if (res.size() < 2) {
                break;
            }
            if (stop) {
                res = record;
                break;
            }

        }
        return res;

    }

    public static Map<String, LinkedList<Double>> readwordvec() throws FileNotFoundException {
        Map<String, LinkedList<Double>> wordvec = new HashMap<>();
        String word2vec = JetTest.getConfigFile("Word2Vec");
        Scanner input = new Scanner(new FileInputStream(word2vec));

        int num = input.nextInt();
        dim = input.nextInt();
        for (int i = 0; i < num; i++) {
            String word = input.next();
            LinkedList<Double> vector = new LinkedList<Double>();
            for (int j = 0; j < dim; j++) {
                double d = input.nextDouble();
                vector.add(d);
            }
            wordvec.put(word, vector);
        }
        System.out.println("Wordvector load Completed!");
        return wordvec;

    }

    public static Pair<Integer, Integer> GetMaxCos(double[][] cosdis, int num) {
        int x = -1, y = -1;
        double dmax = -100;
        for (int i = 0; i < num; i++) {
            for (int j = i + 1; j < num; j++) {
                if (cosdis[i][j] > dmax) {
                    x = i;
                    y = j;
                    dmax = cosdis[i][j];
                }
            }
        }
        return new Pair<>(x, y);
    }

    private void PrintEvents(String output, LinkedList<SLEvent> events) {
        try {
            PrintStream ps = new PrintStream(new FileOutputStream(output));
            ps.println(events.size());
            for (int i = 0; i < events.size(); i++) {
                ps.println(events.get(i));
            }
            ps.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(SLEventExtraction.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static double overlap(String ans, String test) {
        String[] answord = ans.split("[-|_| |\n|'|\"|$|,|(|)|.]");
        String[] testword = test.split("[-|_| |\n|'|\"|$|,|(|)|.]");
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

    public static double StrictOverlap(String ans, String test) {
        String[] answord = ans.split("[-|_| |\n|'|\"|$|,|(|)|.]");
        String[] testword = test.split("[-|_| |\n|'|\"|$|,|(|)|.]");
        int[][] lcs = new int[answord.length + 1][testword.length + 1];
        int imax = -1;
        for (int i = 1; i <= answord.length; i++) {
            for (int j = 1; j <= testword.length; j++) {
                if (answord[i - 1].equals(testword[j - 1])) {
                    lcs[i][j] = lcs[i - 1][j - 1] + 1;
                } else {
                    lcs[i][j] = 0;
                }
                if (lcs[i][j] > imax) {
                    imax = lcs[i][j];
                }
            }
        }
        return 1.0 * imax / answord.length;
    }

    public static void Lookup_All_wordvec(List<SLEntity> le) {
        for (int i = 0; i < le.size(); i++) {
            if (le.get(i).predicate == null) {
//                JOptionPane.showMessageDialog(null, le.get(i).content);
                le.get(i).wordvec = word2vec.get("</s>");
                le.get(i).headwordvec = word2vec.get("</s>");
                continue;
            }
            if (word2vec.containsKey(le.get(i).predicate.word())) {
                le.get(i).wordvec = word2vec.get(le.get(i).predicate.word());
            } else if (word2vec.containsKey(le.get(i).predicate.word().toLowerCase())) {
                le.get(i).wordvec = word2vec.get(le.get(i).predicate.word().toLowerCase());
            } else if (word2vec.containsKey(le.get(i).predicate.lemma())) {
                le.get(i).wordvec = word2vec.get(le.get(i).predicate.lemma());
            } else {
                le.get(i).wordvec = word2vec.get("</s>");
            }
            if (le.get(i).Grand_predicate != null) {
                if (word2vec.containsKey(le.get(i).Grand_predicate.word())) {
                    le.get(i).Grandwordvec = word2vec.get(le.get(i).Grand_predicate.word());
                } else if (word2vec.containsKey(le.get(i).Grand_predicate.word().toLowerCase())) {
                    le.get(i).Grandwordvec = word2vec.get(le.get(i).Grand_predicate.word().toLowerCase());
                } else if (word2vec.containsKey(le.get(i).Grand_predicate.lemma())) {
                    le.get(i).Grandwordvec = word2vec.get(le.get(i).Grand_predicate.lemma());
                }
            }

            /**
             * check for the vector of the head word
             */
            if ("U.S.".equals(le.get(i).head.word())) {
                le.get(i).headwordvec = word2vec.get("us");
            } else if ("U.N.".equals(le.get(i).head.word())) {
                le.get(i).headwordvec = word2vec.get("un");
            } else if ("Feb.".equals(le.get(i).head.word())) {
                le.get(i).headwordvec = word2vec.get("feb");
            } else {
                if (word2vec.containsKey(le.get(i).head.word())) {
                    le.get(i).headwordvec = word2vec.get(le.get(i).head.word());
                } else if (word2vec.containsKey(le.get(i).head.word().toLowerCase())) {
                    le.get(i).headwordvec = word2vec.get(le.get(i).head.word().toLowerCase());
                } else if (word2vec.containsKey(le.get(i).head.lemma())) {
                    le.get(i).headwordvec = word2vec.get(le.get(i).head.lemma());
                } else {
                    le.get(i).headwordvec = word2vec.get("</s>");
                }
            }
//            le.get(i).Cal_trigger_wordvec();
        }
    }

    public static List<SLEvent> FuzzyCluster(List<SLEvent> events, List<SLEntity> le) {
        List<SLEvent> softclusters = new LinkedList<>();
        for (int i = 0; i < events.size(); i++) {
            softclusters.add(new SLEvent(events.get(i)));
        }
        LinkedList<SLEvent> soloevents = new LinkedList<>();
        for (int i = 0; i < le.size(); i++) {
            soloevents.add(new SLEvent(le.get(i), i));
        }

        LinkedList<Pair<Integer, Integer>> record = new LinkedList<>();

        for (int i = 0; i < soloevents.size(); i++) {
            PriorityQueue<Pair<Integer, Double>> scores = new PriorityQueue<>((Object o1, Object o2) -> {
                Pair<Integer, Double> p1 = (Pair<Integer, Double>) o1;
                Pair<Integer, Double> p2 = (Pair<Integer, Double>) o2;
                return p1.getValue() > p2.getValue() ? -1 : 1;                     // descending
            });
            for (int j = 0; j < softclusters.size(); j++) {
                double simi = SLEvent.GetSimilarity(soloevents.get(i), softclusters.get(j));
                scores.add(new Pair<>(j, simi));
            }
            Pair<Integer, Double> mostlikely = scores.poll();
            assert softclusters.get(mostlikely.getKey()).Entityid.contains(i);

            Pair<Integer, Double> secondlikely = scores.poll();
            if (mostlikely.getValue() - secondlikely.getValue() <= 0.3) {
                if (!softclusters.get(secondlikely.getKey()).Entityid.contains(i)) {
                    record.add(new Pair<>(i, secondlikely.getKey()));
                }
            }
        }
        for (Pair<Integer, Integer> p : record) {
            softclusters.get(p.getValue()).addAllArgument(soloevents.get(p.getKey()));
        }

        return softclusters;
    }

}
