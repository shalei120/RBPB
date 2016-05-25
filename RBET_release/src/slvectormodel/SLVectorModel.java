/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools "," Templates
 * and open the template in the editor.
 */
package slvectormodel;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import sleventextraction.*;

/**
 *
 * @author v-lesha
 */
public class SLVectorModel {

    /**
     * @param args the command line arguments
     */
    int vectordim;
    public static String[] Event_types = {
        "Life:Be-Born", "Life:Die", "Life:Marry", "Life:Divorce", "Life:Injure",
        "Transaction:Transfer-Ownership", "Transaction:Transfer-Money", "Movement:Transport", "Business:Start-Org", "Business:End-Org",
        "Business:Declare-Bankruptcy", "Business:Merge-Org", "Conflict:Attack", "Conflict:Demonstrate", "Contact:Meet",
        "Contact:Phone-Write", "Personnel:Start-Position", "Personnel:End-Position", "Personnel:Nominate", "Personnel:Elect",
        "Justice:Arrest-Jail", "Justice:Release-Parole", "Justice:Charge-Indict", "Justice:Trial-Hearing", "Justice:Sue",
        "Justice:Convict", "Justice:Sentence", "Justice:Fine", "Justice:Execute", "Justice:Extradite",
        "Justice:Acquit", "Justice:Pardon", "Justice:Appeal", "NoEvent:NoEvent"
    };
    public static String[] Roles = {
        "Person", "Place", "Buyer", "Seller", "Beneficiary",
        "Price", "Artifact", "Origin", "Destination", "Giver",
        "Recipient", "Money", "Org", "Agent", "Victim",
        "Instrument", "Entity", "Attacker", "Target", "Defendant",
        "Adjudicator", "Prosecutor", "Plaintiff", "Crime", "Position",
        "Sentence", "Vehicle", "Time-Within", "Time-Starting", "Time-Ending",
        "Time-Before", "Time-After", "Time-Holds", "Time-At-Beginning", "Time-At-End","NoRole"
    };
    public static Map<Integer, LinkedList<Integer>> Templates;
    StanfordCoreNLP pipeline;
    Map<String, Integer> dep2id;
    public static int halfwindow = 3;

    static {
        Templates = new HashMap<>();
        Templates = readTemplates();

    }

    public SLVectorModel() throws FileNotFoundException {
        PrintTemplates();
//        SLEventExtraction.word2vec = SLEventExtraction.readwordvec();
//        vectordim = SLEventExtraction.dim;
//        Properties props = new Properties();
//        props.put("annotators", "tokenize, ssplit,pos,lemma, parse");
//        pipeline = new StanfordCoreNLP(props);
        dep2id = new HashMap<>();
    }

    public static void main(String[] args) throws FileNotFoundException, JDOMException, IOException {
        // TODO code application logic here
        SLVectorModel slv = new SLVectorModel();
        slv.run();
    }

    private void run() throws FileNotFoundException, JDOMException, IOException {

        Scanner sgmlist = new Scanner(new FileInputStream("D:\\LDC2006D06\\LDC2006D06\\Data\\LDC2006T06_Original\\data\\English\\nw\\sgmlist.txt"));
        String corpusfolder = "D:\\LDC2006D06\\LDC2006D06\\Data\\LDC2006T06_Original\\data\\English\\nw\\fp1\\";
        //  String output = "D:\\EventExtraction\\EventCandidate_";
        double aver_pre = 0, aver_rec = 0;
        int filenum = 0;
        while (sgmlist.hasNext()) {
            String filename = sgmlist.next();
            System.out.println(filename);
            filename = filename.substring(0, filename.length() - 3);
            File corpus = new File(corpusfolder + filename + "sgm");
            LinkedList<SLEntity> Entities = GetEntities(corpus, pipeline);        // wordvec need to be corrected

            /**
             * Start to read annotated file
             */
            File traincorpus = new File(corpusfolder + filename + "apf.xml");
            LinkedList<Event> annotate = SLAPFReader.ReadEvents(traincorpus);
            PrintStream ps = new PrintStream(new FileOutputStream("D:\\vectormodel_train\\" + filename + "data"));
            for (Event anno : annotate) {
                for (EventArgument argument : anno.arguments) {
                    SLTrainData sltd = new SLTrainData();
                    sltd.event_type = CheckPos(anno.eventtype, Event_types);
                    sltd.role = CheckPos(argument.type, Roles);
                    for (int i = 0; i < Entities.size(); i++) {
                        if (SLEventExtraction.overlap(anno.span, Entities.get(i).span.toString()) >= 0.5 || SLEventExtraction.overlap(Entities.get(i).span.toString(), anno.span) >= 0.5) {
                            if (SLEventExtraction.overlap(Entities.get(i).content, argument.data) > 0) {
                                sltd.embedding.addAll(Entities.get(i).headwordvec);
                                sltd.embedding.addAll(Entities.get(i).wordvec);
                                LinkedList<Double> grand = Entities.get(i).Grandwordvec;
                                sltd.embedding.addAll(grand);
                                /**
                                 * lexical chain
                                 */
                                LinkedList<Double> lexical = LexicalChain(Entities.get(i));
                                sltd.embedding.addAll(lexical);
                                /**
                                 * dependency lookup table
                                 */
                                if (this.dep2id.containsKey(Entities.get(i).dep)) {
                                    Entities.get(i).dep_lookup.set(dep2id.get(Entities.get(i).dep), 1.0);
                                } else {
                                    System.out.println("New dependency relation found: " + Entities.get(i).dep + "\t Total:" + dep2id.size());
                                    this.dep2id.put(Entities.get(i).dep, dep2id.size());
                                    Entities.get(i).dep_lookup.set(dep2id.get(Entities.get(i).dep), 1.0);
                                }
                                sltd.embedding.addAll(Entities.get(i).dep_lookup);
                                break;
                            }
                        }
                    }
                    if (sltd.role != -1) {
                        if (sltd.embedding.size() == 881) {
                            sltd.print(ps);
                        }
                    }
                }

            }
        }
    }

    public static Map<Integer, LinkedList<Integer>> readTemplates() {
        Map<Integer, LinkedList<Integer>> res = new HashMap<>();
        try {
            Scanner input = new Scanner(new FileInputStream("D:\\Grade2_2\\MSRA\\RBET_release\\RBET_release\\data\\ace.template"));
            while (input.hasNextLine()) {
                String eventtemplate = input.nextLine();
                String[] split = eventtemplate.split(" ");
                int eventtypeid = CheckPos(split[0], Event_types);
                LinkedList<Integer> roles = new LinkedList<>();
                for (int i = 1; i < split.length; i++) {
                    if (!"{".equals(split[i]) && !"}".equals(split[i])) {
                        String r = split[i].substring(0, split[i].indexOf("["));
                        roles.add(CheckPos(r, Roles));
                    }
                }
                res.put(eventtypeid, roles);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(SLVectorModel.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("Templates load Completed!");
        return res;
    }

    public static int CheckPos(String t, String[] Event_types) {
        int pos = -1;
        for (int i = 0; i < Event_types.length; i++) {
            if (Event_types[i].contains(t)) {
                pos = i;
                break;
            }
        }
        return pos;
    }

    public static LinkedList<SLEntity> GetEntities(File documentfile, StanfordCoreNLP pipeline) throws JDOMException, IOException {
        LinkedList<SLEntity> res = new LinkedList<>();
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
        for (CoreMap cm : sentences) {
            SemanticGraph dependencies = cm.get(SemanticGraphCoreAnnotations.CollapsedDependenciesAnnotation.class);
            List<SLEntity> le = SLEventExtraction.BPGetNPs(dependencies);
            SLEventExtraction.Lookup_All_wordvec(le);
            for (int i = 0; i < le.size(); i++) {
                le.get(i).span = cm;
            }
            res.addAll(le);
        }
        return res;
    }

    private LinkedList<Double> LexicalChain(SLEntity e) {
        LinkedList<Double> res = new LinkedList<>();
        res = SLMath.Vector_0(SLEventExtraction.dim);
        CoreMap sentence = e.span;
        List<CoreLabel> cmwords = sentence.get(TokensAnnotation.class);
        List<IndexedWord> iwwords = new LinkedList<>();
        int pos = -1;
        for (int i = 0; i < cmwords.size(); i++) {
            IndexedWord iw = new IndexedWord(cmwords.get(i));
            iwwords.add(iw);
            if (iw == e.head) {
                pos = i;
            }
        }
        int count = 0;
        for (int i = pos - 1; i >= 0; i--) {
            if (iwwords.get(i).tag().contains("VB") || iwwords.get(i).tag().contains("JJ")) {
                count++;
                if (SLEventExtraction.word2vec.containsKey(iwwords.get(i).word())) {
                    res = SLMath.Vector_add(res, SLEventExtraction.word2vec.get(iwwords.get(i).word()));
                } else if (SLEventExtraction.word2vec.containsKey(iwwords.get(i).word().toLowerCase())) {
                    res = SLMath.Vector_add(res, SLEventExtraction.word2vec.get(iwwords.get(i).word().toLowerCase()));
                } else if (SLEventExtraction.word2vec.containsKey(iwwords.get(i).lemma())) {
                    res = SLMath.Vector_add(res, SLEventExtraction.word2vec.get(iwwords.get(i).lemma()));
                }
            }
            if (count >= halfwindow) {
                break;
            }
        }
        count = 0;
        for (int i = pos + 1; i < iwwords.size(); i++) {
            if (iwwords.get(i).tag().contains("VB") || iwwords.get(i).tag().contains("JJ")) {
                count++;
                if (SLEventExtraction.word2vec.containsKey(iwwords.get(i).word())) {
                    res = SLMath.Vector_add(res, SLEventExtraction.word2vec.get(iwwords.get(i).word()));
                } else if (SLEventExtraction.word2vec.containsKey(iwwords.get(i).word().toLowerCase())) {
                    res = SLMath.Vector_add(res, SLEventExtraction.word2vec.get(iwwords.get(i).word().toLowerCase()));
                } else if (SLEventExtraction.word2vec.containsKey(iwwords.get(i).lemma())) {
                    res = SLMath.Vector_add(res, SLEventExtraction.word2vec.get(iwwords.get(i).lemma()));
                }
            }
            if (count >= halfwindow) {
                break;
            }
        }
        return res;
    }

    private void PrintTemplates() {
        PrintStream ps = null;
        try {
            ps = new PrintStream(new FileOutputStream("C:\\Users\\v-lesha\\Documents\\MATLAB\\vectorbased\\template.txt"));
            for (int i = 0; i < Event_types.length; i++) {
                LinkedList<Integer> roles = Templates.get(i);
                if (roles == null) {
                    continue;
                }
                ps.print(i + "\t");
                assert roles.size() <= 6;
                while (roles.size() < 6) {
                    roles.add(-1);
                }
                for (int j = 0; j < 6; j++) {
                    ps.print(roles.get(j) + "\t");
                }
                ps.println();
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(SLVectorModel.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            ps.close();
        }
    }

}
