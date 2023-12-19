package com.alibaba.alink;

import com.alibaba.alink.pipeline.LocalPredictor;
import org.apache.flink.types.Row;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Bit software.
 */
public class GuiWith2Windows {

    public static String subData(Object[] data) {
        if (data.length > 6) {
            Object[] tmp = new Object[6];
            for (int i = 0; i < 5; ++i) {
                tmp[i] = data[i];
                tmp[5] = "... ...";
            }
            return Row.of(tmp).toString();
        } else {
            return Row.of(data).toString();
        }
    }

    public static void runGuiV2(LocalPredictor[] predictors) {
        // 创建 JFrame 实例
        JFrame frame = new JFrame("BIT");
        // Setting the width and height of frame
        frame.setSize(880, 192);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JFrame outFrame = new JFrame("output");
        outFrame.setLocation(0, 220);
        outFrame.setSize(880, 800);
        outFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        /* 创建面板，这个类似于 HTML 的 div 标签
         * 我们可以创建多个面板并在 JFrame 中指定位置
         * 面板中我们可以添加文本字段，按钮及其他组件。
         */
        JPanel panel = new JPanel();
        // 添加面板
        frame.add(panel);
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
        panel.add(userLabel);

        /*
         * 创建文本域用于用户输入
         */
        JTextField userText = new JTextField(100);
        userText.setBounds(130, 20, 740, 25);
        panel.add(userText);


        JTextArea textArea = new JTextArea();
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setLocation(0, 0);
        scrollPane.setSize(880, 810);
        scrollPane.setMaximumSize(new Dimension(880, 810));
        textArea.setLocation(0, 0);
        textArea.setSize(880, 810);
        textArea.setMaximumSize(new Dimension(880, 810));

        textArea.setForeground(Color.WHITE);
        textArea.setBackground(Color.BLACK);
        textArea.setVisible(true);
        JButton loginButton = new JButton("wcy预测");
        loginButton.setBounds(10, 70, 300, 25);
        panel.add(loginButton);

        loginButton.addActionListener(e -> {
            String input = userText.getText();
            String[] contents = input.split(",");
            Object[] data = new Object[4];
            for (int i = 0; i < data.length; ++i) {
                data[i] = Double.parseDouble(contents[i].trim());
            }
            try {
                outFrame.getContentPane().add(scrollPane);
                outFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                textArea.append("\n\n**********************************************\n");
                textArea.append("input   : " + subData(data) + "\n");
                textArea.append("output: " + predictors[0].map(Row.of(data)) + "\n");
                userText.setText("");
            } catch (Exception ex) {

                ex.printStackTrace();
            }
        });

        JButton samplesButton = new JButton("wcy批预测");
        samplesButton.setBounds(10, 105, 300, 25);
        panel.add(samplesButton);

        samplesButton.addActionListener(e -> {
            String input = userText.getText();
            Object[] data = new Object[4];
            List<String> lines = null;
            try {
                lines = Files.readAllLines(Paths.get(input));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            assert lines != null;
            textArea.append("\n\n**********************************************\n");
            textArea.append("input path : " + input + "\n");
            for (String line : lines) {
                String[] cs = line.split(",");
                for (int i = 0; i < data.length; ++i) {
                    data[i] = Double.parseDouble(cs[i].trim());
                }
                try {
                    outFrame.getContentPane().add(scrollPane);
                    outFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    textArea.append(line + " output : " + predictors[0].map(Row.of(data)));
                    textArea.append("\n");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            userText.setText("");
        });

        JButton dButton = new JButton("dkl预测");
        dButton.setBounds(330, 70, 300, 25);
        panel.add(dButton);
        dButton.addActionListener(e -> {
            String input = userText.getText();
            String[] contents = input.split(",");
            Object[] data = new Object[21];
            for (int i = 0; i < data.length; ++i) {
                data[i] = Double.parseDouble(contents[i].trim());
            }
            try {
                outFrame.getContentPane().add(scrollPane);
                outFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                textArea.append("\n\n**********************************************\n");
                textArea.append("input   : " + subData(data) + "\n");
                textArea.append("output: " + predictors[1].map(Row.of(data)) + "\n");
                userText.setText("");

            } catch (Exception ex) {

                ex.printStackTrace();
            }
        });

        JButton dSamplesButton = new JButton("dkl批预测");
        dSamplesButton.setBounds(330, 105, 300, 25);
        panel.add(dSamplesButton);

        dSamplesButton.addActionListener(e -> {
            String input = userText.getText();
            Object[] data = new Object[21];
            List<String> lines = null;
            try {
                lines = Files.readAllLines(Paths.get(input));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            assert lines != null;
            textArea.append("\n\n**********************************************\n");
            textArea.append("input path : " + input + "\n");

            for (String line : lines) {
                String[] cs = line.split(",");
                for (int i = 0; i < data.length; ++i) {
                    data[i] = Double.parseDouble(cs[i].trim());
                }
                try {
                    outFrame.getContentPane().add(scrollPane);
                    outFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    textArea.append(line + " output : " + predictors[1].map(Row.of(data)));
                    textArea.append("\n");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            userText.setText("");

        });


        JButton helpButton = new JButton("使用手册");
        helpButton.setBounds(650, 70, 100, 60);
        panel.add(helpButton);

        helpButton.addActionListener(e -> {
            List<String> lines = null;

            try {
                lines = Files.readAllLines(Paths.get("./README.md"));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            assert lines != null;
            textArea.append("\n\n");

            for (String line : lines) {
                try {
                    outFrame.getContentPane().add(scrollPane);
                    outFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    textArea.append(line);
                    textArea.append("\n");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });


        JButton clearButton = new JButton("清空屏幕");
        clearButton.setBounds(770, 70, 100, 60);
        panel.add(clearButton);

        clearButton.addActionListener(e -> textArea.setText(""));

        // 设置界面可见
        outFrame.setVisible(true);
        frame.setVisible(true);
    }
}
