package com.bit.wcy;

import com.alibaba.alink.pipeline.LocalPredictor;
import org.apache.flink.types.Row;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Bit software.
 */
public class Gui {
    public static void runGui(LocalPredictor predictor, String os) throws IOException {
        // 创建 JFrame 实例
        JFrame frame = new JFrame("BIT-MLPEM");

        int currentHeight = 0;
        frame.setLocation(0,0);
        currentHeight +=  70;

        ImageIcon bgimage = new ImageIcon("./blg.png");
        bgimage.setImage(bgimage.getImage().getScaledInstance(60,60, Image.SCALE_DEFAULT));
        JLabel bglabel = new JLabel(bgimage);
        bglabel.setLocation(0, 4);
        bglabel.setSize(new Dimension(60, 60));


        int frameWidth = 1024;// os.equals("--win") ? 900 : 880;
        int frameHeight = 768;
        // Setting the width and height of frame
        frame.setSize(new Dimension(frameWidth, frameHeight));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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
        JLabel sNameLabel = new JLabel("含能材料机器学习平台");
        sNameLabel.setBounds(65, 10, 330, 25);
        Font nfont = new Font("SansSerif", Font.BOLD, 24);

        sNameLabel.setFont(nfont);
        panel.add(sNameLabel);

        JLabel eNameLabel = new JLabel("Machine Learning Platform of Energetic Materials");
        eNameLabel.setBounds(65, 40, 330, 25);
        Font efont = new Font("SansSerif", Font.BOLD, 10);

        eNameLabel.setFont(efont);
        panel.add(eNameLabel);

        JLabel wNameLabel = new JLabel("氧化剂结构预测");
        wNameLabel.setBounds(840, 10, 330, 25);
        Font wfont = new Font("SansSerif", Font.BOLD, 24);

        wNameLabel.setFont(wfont);
        panel.add(wNameLabel);

        JLabel weNameLabel = new JLabel("Oxidizer Structure Predictor");
        weNameLabel.setBounds(840, 40, 330, 25);
        Font wefont = new Font("SansSerif", Font.BOLD, 10);

        weNameLabel.setFont(wefont);
        panel.add(weNameLabel);



        /* 这个方法定义了组件的位置。
         * setBounds(x, y, width, height)
         * x 和 y 指定左上角的新位置，由 width 和 height 指定新的大小。
         */
        JLabel userLabel = new JLabel("Input parameter: ");

        userLabel.setBounds(10, currentHeight + 10, 130, 25);
        userLabel.setFont(sfont);
        panel.add(userLabel);
        panel.add(bglabel);
        /*
         * 创建文本域用于用户输入
         */
        JTextField userText = new JTextField(100);
        userText.setBounds(140, currentHeight + 10, 730 + 144, 27);
        userText.setFont(sfont);
        panel.add(userText);



        ImageIcon bitImage = new ImageIcon("./shuimo.png");
        bitImage.setImage(bitImage.getImage().getScaledInstance(1004,410, Image.SCALE_DEFAULT));

        currentHeight += 100;

        JTextArea textArea = new JTextArea();
        JScrollPane scrollPane = new JScrollPane(textArea);
        //textArea.add(bitlabel);
        scrollPane.setLocation(10, currentHeight);
        scrollPane.setSize(new Dimension(860 + 144, 410));
        textArea.setLocation(10, currentHeight);
        textArea.setSize(new Dimension(860 + 144, 410));

        scrollPane.setBackground(new Color(0,0,0));
       // textArea.setOpaque(false);
        textArea.setFont(font);
        textArea.setForeground(new Color(0, 102, 60));
        textArea.setBackground(Color.WHITE);
        Border border = BorderFactory.createLineBorder(Color.LIGHT_GRAY);
        textArea.setBorder(BorderFactory.createCompoundBorder(border,BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        currentHeight += 420;
        JTextArea errorArea = new JTextArea(){
            final Image image = bitImage.getImage();
            public void paint(Graphics g) {
                setOpaque(false);
                g.drawImage(image, 702, 0, 302, 135, null, null);
                super.paint(g);

            }
        };
        JScrollPane errorPane = new JScrollPane(errorArea);
        errorPane.setLocation(10, currentHeight-5);
        errorPane.setSize(new Dimension(860 + 144, 135));
        errorArea.setLocation(10, currentHeight-5);
        errorArea.setSize(new Dimension(860 + 144, 135));

        errorArea.setForeground(new Color(174, 47, 0));
        errorArea.setFont(font);
        errorArea.setBorder(BorderFactory.createCompoundBorder(border,BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        int buttonHeight = 120;

        JButton loginButton = new JButton("预测 (Prediction)");
        loginButton.setBounds(10, buttonHeight, 240, 40);
        loginButton.setBackground(Color.BLACK);
        loginButton.setForeground(Color.WHITE);

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
                textArea.append("output: " + predictor.map(Row.of(data)) + "\n");
                textArea.append("--------------------------------------------------------------------------------------------------\n");

            } catch (Exception ex) {

                errorArea.append("\n _____error______ invalid input parameter value : " + input + "\n");
                errorArea.append(" _____error______ input parameter must be a vector with length 4. for example: \"1.2,3.23,5.66,6.78\"\n\n");
                ex.printStackTrace();
                return;
            }
            userText.setText("");

        });

        JButton samplesButton = new JButton("批预测 (Batch Prediction)");
        samplesButton.setFont(sfont);
        samplesButton.setBounds(270, buttonHeight, 240, 40);
        samplesButton.setBackground(new Color(174, 47, 0));
        samplesButton.setForeground(Color.WHITE);
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

                    textArea.append(subData(data) + " output : " + predictor.map(Row.of(data)));
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


        JFrame manu = new JFrame("Manual");
        // Setting the width and height of frame
        int menuWidth = os.equals("--win") ? 650 : 620;
        manu.setSize(menuWidth, 768);
        int xLocation = os.equals("--win") ? 1044 : 1094;
        manu.setLocation(xLocation, 0);
        manu.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);


        JButton helpButton = new JButton("Manual");
        helpButton.setFont(sfont);
        helpButton.setBackground(Color.GRAY);
        helpButton.setForeground(Color.WHITE);

        helpButton.setBounds(860, buttonHeight, 152, 40);
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


        JButton clearButton = new JButton("Clear Output");
        clearButton.setFont(sfont);
        clearButton.setBounds(530, buttonHeight, 145, 40);
        clearButton.setBackground(Color.GRAY);
        clearButton.setForeground(Color.WHITE);
        panel.add(clearButton);

        clearButton.addActionListener(e -> {
            frame.getContentPane().add(scrollPane);
            textArea.setText("");
            textArea.append("******************************************************************************\n" +
                    "Welcome to use MLPEM. This window contains the standard outputs of MLPEM. \n"
                    + "******************************************************************************\n");

        });

        JButton clearErrButton = new JButton("Clear Error");
        clearErrButton.setFont(sfont);
        clearErrButton.setBackground(Color.GRAY);
        clearErrButton.setForeground(Color.WHITE);
        clearErrButton.setBounds(695, buttonHeight, 145, 40);
        panel.add(clearErrButton);

        clearErrButton.addActionListener(e -> {
            frame.getContentPane().add(errorPane);
            errorArea.setText("");
            errorArea.append("******************************************************************************\n" +
                    "Welcome to use MLPEM. This window contains the error outputs of MLPEM.\n"
                    + "******************************************************************************\n");
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
