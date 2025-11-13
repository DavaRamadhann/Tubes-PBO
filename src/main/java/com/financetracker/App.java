package com.financetracker;

import com.financetracker.ui.MainFrame;

import javax.swing.*;
import java.io.File;

/**
 * Kelas entri utama untuk aplikasi Personal Finance Tracker.
 * Memastikan direktori /data ada sebelum memulai GUI.
 */
public class App {
    public static void main(String[] args) {
        // Pastikan direktori 'data' ada
        File dataDir = new File("data");
        if (!dataDir.exists()) {
            System.out.println("Membuat direktori 'data'...");
            dataDir.mkdir();
        }

        // Jalankan GUI di Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}