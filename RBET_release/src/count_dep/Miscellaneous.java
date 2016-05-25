/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package count_dep;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

/**
 *
 * @author v-lesha
 */
public class Miscellaneous {

    public Miscellaneous() {
    }

    public static void main(String[] args) throws Exception {
        Miscellaneous m = new Miscellaneous();
        m.MergeACE();
    }

    private void MergeACE() throws FileNotFoundException, JDOMException, IOException {
        String[] corpusfolders = {
            "D:\\LDC2006D06\\LDC2006D06\\Data\\LDC2006T06_Original\\data\\English\\bn\\fp1\\",
            "D:\\LDC2006D06\\LDC2006D06\\Data\\LDC2006T06_Original\\data\\English\\nw\\fp1\\",
            "D:\\LDC2006D06\\LDC2006D06\\Data\\LDC2006T06_Original\\data\\English\\wl\\fp1\\"
        };
        PrintStream ps = new PrintStream(new FileOutputStream("D:\\wordvec\\ACE.txt"));
        for (int i = 0; i < corpusfolders.length; i++) {
            File folder = new File(corpusfolders[i]);
            File[] listFiles = folder.listFiles();
            for (File f : listFiles) {
                if (f.getName().contains(".sgm")) {
                    SAXBuilder builder = new SAXBuilder();
                    Document doc = builder.build(f);
                    Element foo = doc.getRootElement();
                    String text = foo.getChild("BODY").getChild("TEXT").getText();
                   // int titleend = text.indexOf("\n\n");
                   // text = text.substring(titleend + 1);
                    ps.println(text);
                }
            }
        }
        ps.close();
    }

}
