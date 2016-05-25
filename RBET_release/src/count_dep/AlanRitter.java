/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package count_dep;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import javafx.util.Pair;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

/**
 *
 * @author v-lesha
 */
public class AlanRitter {

    public LinkedList<Event> ReadAlanRitterEvents(File f) throws FileNotFoundException {
        LinkedList<Event> res = new LinkedList<>();
        Scanner input = new Scanner(new FileInputStream(f));
        while (input.hasNextLine()) {
            LinkedList<String> triggerlist = new LinkedList<>();
            LinkedList<EventArgument> argumentlist = new LinkedList<>();
            String sentence = input.nextLine();
            Scanner strin = new Scanner(sentence);
            while (strin.hasNext()) {
                String token = strin.next();
                if (token.contains("ENTITY")) {
                    String[] split = token.split("/");
                    argumentlist.add(new EventArgument(split[0], ""));
                } else if (token.contains("EVENT")) {
                    String[] split = token.split("/");
                    triggerlist.add(split[0]);
                }
            }
            for (String phrase : triggerlist) {
                Event e = new Event();
                e.trigger = phrase;
                e.arguments = argumentlist;
                res.add(e);
            }
        }
        return res;
    }

    public Pair<Double,Double> runAlanRitter() throws FileNotFoundException, JDOMException, IOException {
        File answer = new File("D:\\alaneitter\\Iannotated\\Alan.apf.xml");
        File alanritter = new File("D:\\alaneitter\\Iannotated\\shalei.txt");
        double aver_pre = 0, aver_rec = 0;
        LinkedList<Event> AREvents = ReadAlanRitterEvents(alanritter);
        LinkedList<Event> events = Count_dep.ReadEvents(answer);
        Pair<Double, Double> PR = Count_dep.Error_Analysis_Grishman(events, AREvents);
        aver_pre = PR.getKey();
        aver_rec = PR.getValue();
        return new Pair(aver_pre,aver_rec);

    }

    public Pair<Double,Double> runGrishman() throws FileNotFoundException, JDOMException, IOException {
        File answercorpus = new File("D:\\LDC2006D06\\LDC2006D06\\Data\\LDC2006T06_Original\\data\\English\\nw\\fp1\\");
        File testsgm = new File("D:\\ACEAlan\\out\\");
        double aver_pre = 0, aver_rec = 0;
        File[] testfiles = testsgm.listFiles();
        int num = 0;
        for (File ft : testfiles) {
            File answer = new File("D:\\LDC2006D06\\LDC2006D06\\Data\\LDC2006T06_Original\\data\\English\\nw\\fp1\\" + ft.getName().substring(0, ft.getName().length() - 3) + "apf.xml");
            LinkedList<Event> AREvents = ReadAlanRitterEvents(ft);
            LinkedList<Event> events = Count_dep.ReadEvents(answer);
            if (events.size() < 1) {
                continue;
            }
            num++;
            Pair<Double, Double> PR = Count_dep.Error_Analysis_Grishman(events, AREvents);
            aver_pre += PR.getKey();
            aver_rec += PR.getValue();
        }
        aver_pre /= num;
        aver_rec /= num;
        return new Pair(aver_pre,aver_rec);

    }

 

    public void ACE2Alan() throws JDOMException, IOException {
        Properties props = new Properties();
        props.put("annotators", "tokenize, ssplit");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        File corpus = new File("D:\\LDC2006D06\\LDC2006D06\\Data\\LDC2006T06_Original\\data\\English\\nw\\fp1");
        File[] listFiles = corpus.listFiles();
        for (File f : listFiles) {
            if (f.getName().endsWith(".sgm")) {
                PrintStream ps = new PrintStream(new FileOutputStream("D:\\ACEAlan\\" + f.getName()));
                SAXBuilder builder = new SAXBuilder();
                Document doc = builder.build(f);
                Element foo = doc.getRootElement();
                String text = foo.getChild("BODY").getChild("TEXT").getText();
                Annotation document = new Annotation(text);
                pipeline.annotate(document);
                List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
                for (CoreMap cm : sentences) {
                    String str = cm.toString();
                    String str2 = str.replace('\n', ' ');
                    ps.println(str2);
                }
                ps.close();
            }
        }

    }

    public static void main(String[] args) throws JDOMException, IOException {

        //Scanner input = new Scanner(new FileInputStream("D:\\alaneitter\\Iannotated\\shalei.txt"));
        AlanRitter ar = new AlanRitter();
        ar.runGrishman();
        System.out.println();
        ar.runAlanRitter();
    }

}
