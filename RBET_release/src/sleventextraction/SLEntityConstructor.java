/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sleventextraction;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import static sleventextraction.SLEventExtraction.overlap;

/**
 *
 * @author v-lesha
 */
public class SLEntityConstructor {

    public static LinkedList<SLEntity> entitylist;

    static {
        entitylist = new LinkedList<>();
    }
    public StanfordCoreNLP pipeline;

    public SLEntityConstructor() {
        Properties props = new Properties();
        props.put("annotators", "tokenize, ssplit,pos,lemma, parse");
        pipeline = new StanfordCoreNLP(props);
    }

    public void Construct_by_String(String doc) {
        entitylist.clear();
        Annotation document = new Annotation(doc);
        pipeline.annotate(document);

        // these are all the sentences in this document
        // a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        for (CoreMap cm : sentences) {
            SemanticGraph dependencies = cm.get(SemanticGraphCoreAnnotations.CollapsedDependenciesAnnotation.class);
            List<SLEntity> le = SLEventExtraction.BPGetNPs(dependencies);
            for (int i = 0; i < le.size(); i++) {
                le.get(i).span = cm;
            }
            entitylist.addAll(le);
        }
    }

    public void Construct_by_doc(String filename) {
        try {
            entitylist.clear();
            File f = new File("D:\\LDC2006D06\\LDC2006D06\\Data\\LDC2006T06_Original\\data\\English\\nw\\fp1\\" + filename);
            LinkedList<SLEntity> res = new LinkedList<>();
            SAXBuilder builder = new SAXBuilder();
            Document doc = builder.build(f);
            Element foo = doc.getRootElement();
            String text = foo.getChild("BODY").getChild("TEXT").getText();

            int titleend = text.indexOf("\n\n");
            text = text.substring(titleend + 1).replace("\n\n", ".  ");

            Construct_by_String(text);
        } catch (JDOMException ex) {
            Logger.getLogger(SLEntityConstructor.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(SLEntityConstructor.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public static void main(String[] args) {

    }

    public String get(String get, String headText, String span) {
        String res = "";
        for (SLEntity sle : entitylist) {
            if (overlap(sle.span.toString(), span) > 0.7) {
                if (overlap(sle.content, headText) > 0) {
                    if (get.equalsIgnoreCase("predicate")) {
                        res = sle.predicate.lemma();
                    } else if (get.equalsIgnoreCase("grandpredicate")) {
                        if (sle.Grand_predicate != null) {
                            res = sle.Grand_predicate.lemma();
                        }
                    }
                    break;
                }
            }
        }
        return res;
    }
}
