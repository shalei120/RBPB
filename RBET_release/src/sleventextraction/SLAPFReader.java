/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sleventextraction;

import java.io.File;
import java.io.IOException;
import java.util.*;
import org.jdom2.*;
import org.jdom2.input.*;

/**
 *
 * @author v-lesha
 */
public class SLAPFReader {

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
                        eve.eventlargetype = e.getAttribute("TYPE").getValue();
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
}
