/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package slvectormodel;

import java.io.PrintStream;
import java.util.LinkedList;

/**
 *
 * @author v-lesha
 */
public class SLTrainData {

    int event_type;
    int role;
    LinkedList<Double> embedding;

    public SLTrainData() {
        event_type = -1;
        role = -1;
        embedding = new LinkedList<>();
    }

    void print(PrintStream ps) {
        ps.print(event_type + "\t");
        ps.print(role + "\t");
        for (int i = 0; i < embedding.size(); i++) {
            ps.print(embedding.get(i) + "\t");
        }
        ps.println();
    }
}
