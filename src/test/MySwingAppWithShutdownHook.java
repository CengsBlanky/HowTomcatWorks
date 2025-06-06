package test;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import javax.swing.*;

public class MySwingAppWithShutdownHook extends JFrame {
    JButton exitButton = new JButton();
    JTextArea jTextArea1 = new JTextArea();
    String dir = System.getProperty("user.dir");
    String filename = "temp.txt";

    public MySwingAppWithShutdownHook() {
        exitButton.setText("Exit");
        exitButton.setBounds(new Rectangle(304, 248, 76, 37));
        exitButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                exitButton_actionPerformed(e);
            }
        });
        this.getContentPane().setLayout(null);
        jTextArea1.setText("Click the Exit button to quit");
        jTextArea1.setBounds(new Rectangle(9, 7, 371, 235));
        this.getContentPane().add(exitButton, null);
        this.getContentPane().add(jTextArea1, null);
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.setBounds(0, 0, 400, 330);
        this.setVisible(true);
        initialize();
    }

    private void initialize() {
        // add shutdown hook
        MyShutdownHook shutdownHook = new MyShutdownHook();
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        // create a temp file
        File file = new File(dir, filename);
        try {
            System.out.println("Creating temporary file");
            file.createNewFile();
        } catch (IOException e) {
            System.out.println("Failed creating temporary file.");
        }
    }

    private void shutdown() {
        // delete the temp file
        File file = new File(dir, filename);
        if (file.exists()) {
            System.out.println("Deleting temporary file.");
            file.delete();
        }
    }

    void exitButton_actionPerformed(ActionEvent e) {
        shutdown();
        System.exit(0);
    }

    public static void main(String[] args) {
        MySwingAppWithShutdownHook mySwingApp = new MySwingAppWithShutdownHook();
    }

    private class MyShutdownHook extends Thread {
        public void run() {
            shutdown();
        }
    }
}