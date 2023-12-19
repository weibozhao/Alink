package com.alibaba.alink;

import com.alibaba.alink.pipeline.LocalPredictor;
import org.apache.flink.types.Row;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

/**
 * Bit software.
 */
public class Gui {
    public static void runGui(LocalPredictor[] predictors, String os) throws MalformedURLException {
        // 创建 JFrame 实例
        JFrame frame = new JFrame("BIT");
        frame.setLocation(0,0);

        int frameWidth = os.equals("--win") ? 900 : 880;
        // Setting the width and height of frame
        frame.setSize(new Dimension(frameWidth, 1000));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMaximumSize(new Dimension(frameWidth, 1000));
        Font font = new Font("SansSerif", Font.BOLD, 16);
        Font sfont = new Font("SansSerif", Font.BOLD, 14);
        /* 创建面板，这个类似于 HTML 的 div 标签
         * 我们可以创建多个面板并在 JFrame 中指定位置
         * 面板中我们可以添加文本字段，按钮及其他组件。
         */
        JPanel panel = new JPanel();
        // 添加面板
        frame.add(panel);
        frame.setResizable(false);

        /*
         * 调用用户定义的方法并添加组件到面板
         */

        /* 布局部分我们这边不多做介绍
         * 这边设置布局为 null
         */
        panel.setLayout(null);

        // 创建 JLabel
        JLabel userLabel = new JLabel("Input parameter: ");
        /* 这个方法定义了组件的位置。
         * setBounds(x, y, width, height)
         * x 和 y 指定左上角的新位置，由 width 和 height 指定新的大小。
         */
        userLabel.setBounds(10, 20, 130, 25);
        userLabel.setFont(sfont);
        panel.add(userLabel);

        /*
         * 创建文本域用于用户输入
         */
        JTextField userText = new JTextField(100);
        userText.setBounds(140, 20, 730, 27);
        userText.setFont(sfont);
        panel.add(userText);

        JTextArea textArea = new JTextArea();
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setLocation(10, 140);
        scrollPane.setSize(new Dimension(860, 610));
        textArea.setLocation(10, 110);
        textArea.setSize(new Dimension(860, 610));

        textArea.setForeground(Color.YELLOW);
        textArea.setBackground(Color.BLACK);
        textArea.setFont(font);

        JTextArea errorArea = new JTextArea();
        JScrollPane errorPane = new JScrollPane(errorArea);
        errorPane.setLocation(10, 760);
        errorPane.setSize(new Dimension(860, 195));
        errorArea.setLocation(10, 760);
        errorArea.setSize(new Dimension(860, 195));

        errorArea.setForeground(Color.RED);
        errorArea.setBackground(Color.BLACK);
        errorArea.setFont(font);


        JButton loginButton = new JButton("wcy预测");
        loginButton.setBounds(10, 70, 240, 25);
        loginButton.setFont(sfont);

        panel.add(loginButton);

        loginButton.addActionListener(e -> {
            String input = userText.getText();

            try {
                Object[] data = new Object[4];
                String[] contents = input.split(",");
                if (contents.length != data.length) {
                    throw new RuntimeException("err in length");
                }
                for (int i = 0; i < data.length; ++i) {
                    data[i] = Double.parseDouble(contents[i].trim());
                }

                textArea.append("\n\n--------------------------------------------------------------------------------------------------\n");
                textArea.append("input   : " + subData(data) + "\n");
                textArea.append("output: " + predictors[0].map(Row.of(data)) + "\n");
                textArea.append("--------------------------------------------------------------------------------------------------\n");

            } catch (Exception ex) {

                errorArea.append("\n _____error______ invalid input parameter value : " + input + "\n");
                errorArea.append(" _____error______ input parameter must be a vector with length 4. for example: \"1.2,3.23,5.66,6.78\"\n\n");
                ex.printStackTrace();
                return;
            }
            userText.setText("");

        });

        JButton samplesButton = new JButton("wcy批预测");
        samplesButton.setFont(sfont);
        samplesButton.setBounds(10, 105, 240, 25);
        panel.add(samplesButton);

        samplesButton.addActionListener(e -> {
            String input = userText.getText();
            Object[] data = new Object[4];
            List<String> lines;
            try {
                lines = Files.readAllLines(Paths.get(input));
            } catch (IOException ex) {
                errorArea.append("\n _____error______ invalid input parameter value : " + input + "\n");
                errorArea.append(" _____error______ input parameter must be a valid path with vectors in it.\n\n");
                ex.printStackTrace();
                return;
            }
            textArea.append("\n\n--------------------------------------------------------------------------------------------------\n");
            textArea.append("input path : " + input + "\n");
            for (String line : lines) {
                try {
                    String[] cs = line.split(",");
                    if (cs.length != data.length) {
                        throw new RuntimeException("err in length");
                    }
                    for (int i = 0; i < data.length; ++i) {
                        data[i] = Double.parseDouble(cs[i].trim());
                    }

                    textArea.append(subData(data) + " output : " + predictors[0].map(Row.of(data)));
                    textArea.append("\n");
                } catch (Exception ex) {

                    errorArea.append("\n _____error______ : invalid input vector in the file. " + input + "\n");
                    errorArea.append(" _____error______ : input vector must be a vector with length 4.\n\n");
                    ex.printStackTrace();
                    return;
                }
            }
            textArea.append("--------------------------------------------------------------------------------------------------\n");
            userText.setText("");
        });

        JButton dButton = new JButton("dkl预测");
        dButton.setFont(sfont);
        dButton.setBounds(270, 70, 240, 25);
        panel.add(dButton);
        dButton.addActionListener(e -> {
            String input = userText.getText();
            try {
                Object[] data = new Object[21];
                String[] contents = input.split(",");
                if (contents.length != data.length) {
                    throw new RuntimeException("err in length!");
                }
                for (int i = 0; i < data.length; ++i) {
                    data[i] = Double.parseDouble(contents[i].trim());
                }


                textArea.append("\n\n--------------------------------------------------------------------------------------------------\n");
                textArea.append("input   : " + subData(data) + "\n");
                textArea.append("output: " + predictors[1].map(Row.of(data)) + "\n");
                textArea.append("--------------------------------------------------------------------------------------------------\n");


            } catch (Exception ex) {

                errorArea.append("\n _____error______ invalid input parameter value : " + input + "\n");
                errorArea.append(" _____error______ input parameter must be a vector with length 21.\n\n");
                ex.printStackTrace();
                return;
            }
            userText.setText("");
        });

        JButton dSamplesButton = new JButton("dkl批预测");
        dSamplesButton.setFont(sfont);
        dSamplesButton.setBounds(270, 105, 240, 25);
        panel.add(dSamplesButton);

        dSamplesButton.addActionListener(e -> {
            String input = userText.getText();
            Object[] data = new Object[21];
            List<String> lines;
            try {
                lines = Files.readAllLines(Paths.get(input));
            } catch (IOException ex) {

                errorArea.append("\n _____error______ invalid input parameter value : " + input + "\n");
                errorArea.append(" _____error______ input parameter must be a valid path with vectors in it.\n\n");
                ex.printStackTrace();
                return;
            }
            textArea.append("\n\n--------------------------------------------------------------------------------------------------\n");
            textArea.append("input path : " + input + "\n");

            for (String line : lines) {
                try {
                    String[] cs = line.split(",");
                    if (cs.length != data.length) {
                        throw new RuntimeException("err in length!");
                    }
                    for (int i = 0; i < data.length; ++i) {
                        data[i] = Double.parseDouble(cs[i].trim());
                    }

                    textArea.append(subData(data) + " output : " + predictors[1].map(Row.of(data)));
                    textArea.append("\n");
                } catch (Exception ex) {

                    errorArea.append("\n _____error______ : invalid input vector in the file. " + input + "\n");
                    errorArea.append(" _____error______ : input vector must be a vector with length 21.\n\n");
                    ex.printStackTrace();
                    return;
                }
            }
            textArea.append("--------------------------------------------------------------------------------------------------\n");
            userText.setText("");

        });

        JFrame manu = new JFrame("使用手册");
        // Setting the width and height of frame
        int menuWidth = os.equals("--win") ? 650 : 620;
        manu.setSize(menuWidth, 1000);
        int xLocation = os.equals("--win") ? 970 : 950;
        manu.setLocation(xLocation, 0);
        manu.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);


        JButton helpButton = new JButton("使用手册");
        helpButton.setFont(font);
        helpButton.setBounds(770, 70, 100, 60);
        panel.add(helpButton);


        JPanel jpanel = new JPanel();
        LayoutManager layout = new FlowLayout();
        jpanel.setLayout(layout);

        JEditorPane jEditorPane = new JEditorPane();
        jEditorPane.setEditable(false);
        File f = new File("README");
        String path = f.getAbsolutePath() + (os.equals("--win") ? "_GBK" : "_UTF8");

        System.out.println("file://" +  path + ".html");
        URL url= new URL("file:///" +  path + ".html");
        try {
            jEditorPane.setPage(url);
            jEditorPane.setBackground(Color.white);
        } catch (IOException e) {
            jEditorPane.setContentType("text/html");
            jEditorPane.setText("<html>Page not found.</html>");
        }

        JScrollPane jScrollPane = new JScrollPane(jEditorPane);
        jScrollPane.setPreferredSize(new Dimension(600,1000));

        jpanel.add(jScrollPane);
        manu.setResizable(false);
        manu.getContentPane().add(jpanel, BorderLayout.CENTER);






        helpButton.addActionListener(e -> {
            manu.setVisible(true);
        });


        JButton clearButton = new JButton("清空输出");
        clearButton.setFont(font);
        clearButton.setBounds(530, 70, 100, 60);
        panel.add(clearButton);

        clearButton.addActionListener(e -> {
            frame.getContentPane().add(scrollPane);
            textArea.setText("");
            textArea.append("**********************************************************************\n" +
                    "Welcome to use bit. This window contains the standard outputs of bit. \n"
                    + "**********************************************************************\n");

        });

        JButton clearErrButton = new JButton("清空错误");
        clearErrButton.setFont(font);
        clearErrButton.setBounds(650, 70, 100, 60);
        panel.add(clearErrButton);

        clearErrButton.addActionListener(e -> {
            frame.getContentPane().add(errorPane);
            errorArea.setText("");
            errorArea.append("**********************************************************************\n" +
                    "Welcome to use bit. This window contains the error outputs of bit.\n"
                    + "**********************************************************************\n");
        });

        // 设置界面可见
        frame.setVisible(true);
        clearButton.doClick();
        clearErrButton.doClick();
    }

    public static String subData(Object[] data) {
        if (data.length > 4) {
            Object[] tmp = new Object[5];
            for (int i = 0; i < 4; ++i) {
                tmp[i] = Float.parseFloat(data[i].toString());
                tmp[4] = "... ...";
            }
            return Row.of(tmp).toString();
        } else {
            return Row.of(data).toString();
        }
    }
}
