/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sleventextraction;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author v-lesha
 */
public class SLMath {

    public static void main(String[] args) {
        LinkedList<Double> a = new LinkedList<>();
        LinkedList<Double> b = new LinkedList<>();
        a.add(1.0);
        Vector_add(a, b);

    }

    public static LinkedList<Double> Vector_add(LinkedList<Double> a, LinkedList<Double> b) {
        if (a.size() != b.size()) {
            try {
                throw new Exception("Vector_add error");
            } catch (Exception ex) {
                Logger.getLogger(SLMath.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        LinkedList<Double> c = new LinkedList<>();
        for (int i = 0; i < a.size(); i++) {
            c.add(a.get(i) + b.get(i));
        }
        return c;
    }

    public static double[] Vector_add(char op, double[] a, double[] b) {
        if (a.length != b.length) {
            try {
                throw new Exception("Vector_add error");
            } catch (Exception ex) {
                Logger.getLogger(SLMath.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        double[] c = new double[a.length];
        for (int i = 0; i < a.length; i++) {
            if (op == '-') {
                c[i] = -a[i] - b[i];
            } else {
                c[i] = a[i] + b[i];
            }

        }
        return c;
    }

    public static LinkedList<Double> Vector_minus(LinkedList<Double> a, LinkedList<Double> b) {
        if (a.size() != b.size()) {
            try {
                throw new Exception("Vector_minus error");
            } catch (Exception ex) {
                Logger.getLogger(SLMath.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        LinkedList<Double> c = new LinkedList<>();
        for (int i = 0; i < a.size(); i++) {
            c.add(a.get(i) - b.get(i));
        }
        return c;
    }

    public static LinkedList<Double> Vector_0(int dim) {
        LinkedList<Double> c = new LinkedList<>();
        for (int i = 0; i < dim; i++) {
            c.add(0.0);
        }
        return c;
    }

    public static double[] Vector_1(int dim) {
        double[] c = new double[dim];
        for (int i = 0; i < dim; i++) {
            c[i] = 1.0;
        }
        return c;
    }

    public static LinkedList<Double> Vector_divide_num(LinkedList<Double> a, double b) {
        LinkedList<Double> c = new LinkedList<>();
        for (int i = 0; i < a.size(); i++) {
            c.add(a.get(i) / b);
        }
        return c;
    }

    public static LinkedList<Double> Vector_multiply_num(LinkedList<Double> a, double b) {
        LinkedList<Double> c = new LinkedList<>();
        for (int i = 0; i < a.size(); i++) {
            c.add(a.get(i) * b);
        }
        return c;
    }

    public static double Getcosdis(LinkedList<Double> v1, LinkedList<Double> v2) {
        return dot(v1, v2) / norm(v1) / norm(v2);
    }

    public static double dot(LinkedList<Double> v1, LinkedList<Double> v2) {
        double res = 0.0;
        for (int i = 0; i < v1.size(); i++) {
            res += v1.get(i) * v2.get(i);
        }
        return res;
    }

    public static double norm(LinkedList<Double> v1) {
        double res = 0.0;
        for (int i = 0; i < v1.size(); i++) {
            res += v1.get(i) * v1.get(i);
        }
        return Math.sqrt(res);

    }

    public static LinkedList<String> Sample_M_from_N_(LinkedList<String> list, int m) {
        if (m >= list.size()) {
            return list;
        } else {
            LinkedList<String> res = new LinkedList<>();
            int n = list.size();
            for (int i = 0; i < list.size(); i++) {
                if (Math.random() <= 1.0 * m / n) {
                    res.add(list.get(i));
                    m--;
                }
                n--;
            }
            return res;
        }
    }

    public static double Quadratic(int[] X, double[][] C) {
        double[] vec = new double[X.length];
        for (int i = 0; i < X.length; i++) {
            for (int j = 0; j < X.length; j++) {
                vec[i] += X[j] * C[i][j];
            }
        }
        return dot(vec, X);
    }

    public static double dot(double[] a, int[] b) {
        double res = 0;
        for (int i = 0; i < a.length; i++) {
            res += a[i] * b[i];
        }
        return res;
    }

    public static double[] gradQuadratic(double[] X, double[][] Connectivity) {
        double[][] simmi = new double[X.length][X.length];
        for (int i = 0; i < X.length; i++) {
            for (int j = 0; j < X.length; j++) {
                simmi[i][j] = Connectivity[i][j] + Connectivity[j][i];
            }
        }
        double res[] = new double[X.length];
        for (int i = 0; i < X.length; i++) {
            for (int j = 0; j < X.length; j++) {
                res[i] += simmi[i][j] * X[j];
            }
        }
        return res;
    }

    public static LinkedList<Double> Vector_minusAbs(LinkedList<Double> a, LinkedList<Double> b) {
        if (a.size() != b.size()) {
            try {
                throw new Exception("Vector_minus error");
            } catch (Exception ex) {
                Logger.getLogger(SLMath.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        LinkedList<Double> c = new LinkedList<>();
        for (int i = 0; i < a.size(); i++) {
            c.add(Math.abs(a.get(i) - b.get(i)));
        }
        return c;
    }

}
