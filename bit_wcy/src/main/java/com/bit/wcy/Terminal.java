package com.bit.wcy;

import com.alibaba.alink.pipeline.LocalPredictor;
import org.apache.flink.types.Row;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

import static com.bit.wcy.Gui.subData;

/**
 * Bit software.
 */
public class Terminal {
    public static void runTerminal(LocalPredictor predictor) {

        Scanner scanner = new Scanner(System.in);

        boolean isTypeChoose = false;
        int processType = -1;

        while (true) {
            if (!isTypeChoose) {
                System.out.println("**************************************************************");
                System.out.println("Please enter a number to choose the processing type.");
                System.out.println("**************************************************************");
                System.out.println("0: using model to predict one sample.");
                System.out.println("1: using model to predict more than one samples in a file.");
                System.out.println("**************************************************************");
                System.out.print("type: ");
                String input = scanner.nextLine();
                if (input.equals("exit") || input.equals("quit") || input.equals("q")) {
                    System.out.println("**************************************************************");
                    System.out.println("              Thanks for your using.");
                    System.out.println("**************************************************************");
                    break;
                } else if (input.equals("0") || input.equals("1")) {
                    processType = Integer.parseInt(input);
                    isTypeChoose = true;
                } else {
                    System.out.println("invalid data type, data type must be 0, 1. ");
                    continue;
                }
            }

            String info = "input sample: ";
            if (processType == 1 || processType == 3) {
                info = "input file path: ";
            }
            System.out.print(info);
            String input = scanner.nextLine();
            if (input.equals("exit") || input.equals("quit") || input.equals("q")) {
                isTypeChoose = false;
                continue;
            }
            try {
                switch (processType) {
                    case 0:
                        String[] contents = input.split(",");
                        Object[] data = new Object[4];
                        for (int i = 0; i < data.length; ++i) {
                            data[i] = Double.parseDouble(contents[i].trim());
                        }
                        System.out.println("output: " + predictor.map(Row.of(data)));
                        break;
                    case 1:
                        data = new Object[4];
                        List<String> lines = Files.readAllLines(Paths.get(input));
                        for (String line : lines) {
                            String[] cs = line.split(",");
                            for (int i = 0; i < data.length; ++i) {
                                data[i] = Double.parseDouble(cs[i].trim());
                            }
                            System.out.println(subData(data) + " output : " + predictor.map(Row.of(data)));
                        }
                        break;
                    default:

                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("invalid input string: " + input);
            }
        }
    }
}
