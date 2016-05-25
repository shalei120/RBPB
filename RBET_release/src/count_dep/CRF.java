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
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

/**
 *
 * @author v-lesha
 */
public class CRF {

    public static void main(String[] args) throws JDOMException, IOException {
        CRF crf = new CRF();
        crf.runACE();
    }

    private void transfer_into_inputfiles() throws JDOMException, IOException {
        Properties props = new Properties();
        props.put("annotators", "tokenize, ssplit, pos, lemma");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        File corpus = new File("D:\\LDC2006D06\\LDC2006D06\\Data\\LDC2006T06_Original\\data\\English\\nw\\fp1");
        File[] listFiles = corpus.listFiles();
        for (File f : listFiles) {
            if (f.getName().endsWith(".sgm")) {
                PrintStream ps = new PrintStream(new FileOutputStream("D:\\ACEAlan\\UIUCNERInput\\" + f.getName()));
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

    private void runACE() throws FileNotFoundException {
        File folder = new File("D:\\semie_vrelease\\data\\");
        File[] listFiles = folder.listFiles();
        double aver=0;
        for (File f : listFiles) {
            if (f.getName().endsWith("test.data")) {
                int correctnum = 0;
                int total = 0;
                Scanner ansinput = new Scanner(new FileInputStream(f));
                Scanner testinput = new Scanner(new FileInputStream("D:\\semie_vrelease\\output\\" + f.getName().substring(0, f.getName().length() - 10) + ".pred"));
                while (ansinput.hasNext()) {
                    total++;
                    for (int i = 0; i < 7; i++) {
                        ansinput.nextLine();
                    }
                    int rolenum = ansinput.nextInt();
                    String[] offsets = new String[rolenum];
                    String[] roles = new String[rolenum];
                    for (int i = 0; i < rolenum; i++) {
                        offsets[i] = ansinput.next();
                        roles[i] = ansinput.next().split("\\|")[0];
                    }
                    for (int i = 0; i < 5 + 1; i++) {
                        ansinput.nextLine();
                    }
                    int featurenum = ansinput.nextInt();
                    ansinput.nextLine();
                    String s;
                    for (int i = 0; i < featurenum * 2 + 1; i++) {
                        s= ansinput.nextLine();
                    }
                    int typenum = ansinput.nextInt();
                    ansinput.nextLine();

                    for (int i = 0; i < typenum; i++) {
                        ansinput.nextLine();
                    }

                    /**
                     * answer reading completed
                     */
                    testinput.nextLine();
                    testinput.nextLine();
                    testinput.nextLine();
                    String region;
                    LinkedList<String> regions = new LinkedList<>();
                    while (!(region = testinput.next()).contains("=")) {
                        region = testinput.next();
                        testinput.nextLine();
                        regions.add(region);
                    }
                    testinput.nextLine();

                    /**
                     * test reading completed
                     */
                    int keyrolenum = 0, matchnum = 0;
                    for (int i = 0; i < rolenum; i++) {
                       // if (Count_dep.Considered_roles.contains(roles[i]) || roles[i].equals("Place")) {
                            keyrolenum++;
                            for (String sreg : regions) {
                                if (Contains(sreg, "[" + offsets[i] + ")")) {
                                    matchnum++;
                                    break;
                                }
                      //      }
                        }
                    }
                    if (keyrolenum == matchnum) {
                        correctnum++;
                    }
                }
                aver += 1.0*correctnum/total;

            }
        }
        aver/=4;
        System.out.print(aver);
    }

    private boolean Contains(String s1, String s2) {
        int left1, left2, right1, right2;
        String[] s1a = s1.substring(1, s1.length() - 1).split(",");
        String[] s2a = s2.substring(1, s2.length() - 1).split(",");
        left1 = Integer.valueOf(s1a[0]);
        right1 = Integer.valueOf(s1a[1]);
        left2 = Integer.valueOf(s2a[0]);
        right2 = Integer.valueOf(s2a[1]);
        return left1 <= left2 && right1 >= right2 || left1 >= left2 && right1 <= right2;
    }
}
