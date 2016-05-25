///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package rbet_release;
//
//import AceJet.Ace;
//import AceJet.TrainEventTagger;
//import java.io.IOException;
//
///**
// *
// * @author v-lesha
// */
//public class RBET_release {
//
//    /**
//     * @param args the command line arguments
//     * @args[0]: train or test
//     * @args[1]: property file
//     * @args[2]: the list of test file
//     * @args[3]: the folder of test files
//     * @args[4]: the output folder
//     * @throws java.io.IOException
//     */
//    public static void main(String[] args) throws IOException {
//        // TODO code application logic here
////        args = new String[5];
////        args[0] = "-train";
////        args[1] = "C:\\Users\\v-lesha\\Documents\\NetBeansProjects\\RBET_release\\props\\ace11chunker.properties";
////        args[2] = "D:\\LDC2006D06\\LDC2006D06\\Data\\LDC2006T06_Original\\data\\English\\nw\\Trainfile.txt";
////        args[3] = "D:\\LDC2006D06\\LDC2006D06\\Data\\LDC2006T06_Original\\data\\English\\nw\\fp1\\";
////        args[4] = "C:\\Users\\v-lesha\\Documents\\NetBeansProjects\\RBET_release\\Trainout\\";
//        if (args.length != 5) {
//            PrintErrMsg();
//            System.exit(1);
//        }
//        switch (args[0]) {
//            case "-train":
//                Ace.Testing = false;
//                String[] train_args = new String[4];
//                for (int i = 0; i < 4; i++) {
//                    train_args[i] = args[i + 1];
//                }
//                TrainEventTagger.main(train_args);
//                break;
//            case "-test":
//                String[] test_args = new String[4];
//                for (int i = 0; i < 4; i++) {
//                    test_args[i] = args[i + 1];
//                }
//                Ace.run(test_args);
//                break;
//            default:
//                PrintErrMsg();
//                break;
//        }
//    }
//
//    private static void PrintErrMsg() {
//        System.err.print("Input format error!\n"
//                + "\n"
//                + "Correct format:\n"
//                + "	RBET_release -train properties trainfilelist traindocumentDir ModeloutputDir\n"
//                + "or\n"
//                + "	RBET_release -test properties testfilelist testdocumentDir testoutputDir\n");
//
//    }
//
//}
