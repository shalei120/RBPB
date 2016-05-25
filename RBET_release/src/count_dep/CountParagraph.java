package count_dep;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import javafx.util.Pair;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author v-lesha
 */
public class CountParagraph {

    StanfordCoreNLP pipeline;

    public CountParagraph() {
        Properties props = new Properties();
        props.put("annotators", "tokenize, ssplit");
        pipeline = new StanfordCoreNLP(props);
    }

    public static void main(String[] args) throws JDOMException, IOException {
        CountParagraph cp = new CountParagraph();
        cp.runsen();
    }

    private void run() throws JDOMException, IOException {
        File answercorpus = new File("D:\\LDC2006D06\\LDC2006D06\\Data\\LDC2006T06_Original\\data\\English\\nw\\fp1\\");
        File testsgm = new File("D:\\ACEAlan\\out\\");
        double aver_pre = 0, aver_rec = 0;
        File[] testfiles = testsgm.listFiles();
        int totalevent = 0;
        int in_one_sentence = 0, neighbor_sentence = 0, same_paragraph = 0;
        for (File ft : testfiles) {
            File answer = new File("D:\\LDC2006D06\\LDC2006D06\\Data\\LDC2006T06_Original\\data\\English\\nw\\fp1\\" + ft.getName().substring(0, ft.getName().length() - 3) + "apf.xml");
            File documentfile = new File("D:\\LDC2006D06\\LDC2006D06\\Data\\LDC2006T06_Original\\data\\English\\nw\\fp1\\" + ft.getName().substring(0, ft.getName().length() - 3) + "sgm");

            LinkedList<Event> events = Count_dep.ReadEvents(answer);
            LinkedList<String> sentences = GetSentences(documentfile, pipeline);
            //LinkedList<String> paragraphs = GetParagraphs(documentfile);
            totalevent += events.size();
            for (Event e : events) {
                if (occur_in_one_sentence(e, sentences)) {
                    in_one_sentence++;
                }
                if (occur_in_neightbor_sentence(e, sentences)) {
                    neighbor_sentence++;
                }
            }
        }
        System.out.println(in_one_sentence + "/" + totalevent);
        System.out.println(neighbor_sentence + "/" + totalevent);
        System.out.println(totalevent + "/" + totalevent);
    }

    private void runsen() throws JDOMException, IOException {
        Scanner sgmlist = new Scanner(new FileInputStream("D:\\LDC2006D06\\LDC2006D06\\Data\\LDC2006T06_Original\\data\\English\\nw\\Testfile.txt"));
        double aver_pre = 0, aver_rec = 0;
        int total = 0;
        int in_one_sentence = 0;
        while (sgmlist.hasNext()) {
            String filename = sgmlist.next();
            File answer = new File("D:\\LDC2006D06\\LDC2006D06\\Data\\LDC2006T06_Original\\data\\English\\nw\\fp1\\" + filename.replace(".sgm", ".apf.xml"));
            File documentfile = new File("D:\\LDC2006D06\\LDC2006D06\\Data\\LDC2006T06_Original\\data\\English\\nw\\fp1\\" + filename);

            LinkedList<Event> events = Count_dep.ReadEvents(answer);
            LinkedList<String> sentences = GetSentences(documentfile, pipeline);
            //LinkedList<String> paragraphs = GetParagraphs(documentfile);
            total += sentences.size();
            for (String s : sentences) {
                if (occur_over2event(s, events)) {
                    in_one_sentence++;
                }
            }
        }
        System.out.println(in_one_sentence + "/" + total);
    }

    public static LinkedList<String> GetSentences(File documentfile, StanfordCoreNLP pipeline) throws JDOMException, IOException {
        LinkedList<String> sentencelist = new LinkedList<>();
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
            sentencelist.add(cm.toString());
        }

        return sentencelist;
    }

    private LinkedList<String> GetParagraphs(File documentfile) throws JDOMException, IOException {
        LinkedList<String> paralist = new LinkedList<>();
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(documentfile);
        Element foo = doc.getRootElement();
        String text = foo.getChild("BODY").getChild("TEXT").getText();
        String[] split = text.split("\n\n");
        for (int i = 0; i < split.length; i++) {
            paralist.add(split[i]);
        }
        return paralist;
    }

    private boolean occur_in_one_sentence(Event e, LinkedList<String> sentences) {
        boolean res = false;
        for (String sen : sentences) {
            int slots = 0;
            for (EventArgument ea : e.arguments) {
                if (sen.contains(ea.data)) {
                    slots++;
                }
            }
            if (slots == e.arguments.size()) {
                res = true;
                break;
            }
        }
        return res;
    }

    private boolean occur_in_neightbor_sentence(Event e, LinkedList<String> sentences) {
        boolean res = false;
        for (int i = 1; i < sentences.size() - 1; i++) {
            int slots = 0;
            for (EventArgument ea : e.arguments) {
                if (sentences.get(i - 1).contains(ea.data) || sentences.get(i).contains(ea.data) || sentences.get(i + 1).contains(ea.data)) {
                    slots++;
                }
            }
            if (slots == e.arguments.size()) {
                res = true;
                break;
            }
        }
        return res;
    }

    private boolean occur_over2event(String s, LinkedList<Event> events) {
        //boolean res = false;
        int num = 0;
        for (Event e : events) {
            int slots = 0;
            for (EventArgument ea : e.arguments) {
                if (s.contains(ea.data)) {
                    slots++;
                }
            }
            if (slots == e.arguments.size()) {
                num++;
            }
        }
        return num >= 2;
    }
}
