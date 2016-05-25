package sleventextraction;

import edu.stanford.nlp.ling.CoreAnnotations;
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
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import static sleventextraction.SLAPFReader.ReadEvents;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author v-lesha
 */
public class SLCountParagraph {

    StanfordCoreNLP pipeline;

    public SLCountParagraph() {
        Properties props = new Properties();
        props.put("annotators", "tokenize, ssplit");
        pipeline = new StanfordCoreNLP(props);
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

}
