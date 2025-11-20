package com.financetracker.ui;

import com.financetracker.model.Category;
import com.financetracker.model.Transaction;
import com.financetracker.model.TransactionType;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExpensePieChartPanel extends JPanel {

    private Map<Category, Double> categoryTotals;
    private double totalExpense;

    public ExpensePieChartPanel(List<Transaction> transactions) {
        calculateData(transactions);
        setPreferredSize(new Dimension(500, 400));
        setBackground(Color.WHITE);
    }

    private void calculateData(List<Transaction> transactions) {
        categoryTotals = new HashMap<>();
        totalExpense = 0;

        for (Transaction t : transactions) {
            if (t.getType() == TransactionType.EXPENSE) {
                categoryTotals.put(t.getCategory(),
                        categoryTotals.getOrDefault(t.getCategory(), 0.0) + t.getAmount());
                totalExpense += t.getAmount();
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (totalExpense == 0) {
            g.drawString("Belum ada data pengeluaran.", getWidth() / 2 - 80, getHeight() / 2);
            return;
        }

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        
        // Area untuk chart
        int chartDiameter = Math.min(width, height) - 100;
        int chartX = (width - chartDiameter) / 2;
        int chartY = (height - chartDiameter) / 2 - 20;

        // Area untuk legenda
        int legendX = 20;
        int legendY = height - 60;

        double currentAngle = 90; // Mulai dari atas

        // Warna untuk kategori (hardcoded simple palette)
        Color[] colors = {
            new Color(255, 99, 132),   // Merah
            new Color(54, 162, 235),   // Biru
            new Color(255, 206, 86),   // Kuning
            new Color(75, 192, 192),   // Hijau Teal
            new Color(153, 102, 255),  // Ungu
            new Color(255, 159, 64),   // Orange
            new Color(201, 203, 207)   // Abu
        };

        int colorIndex = 0;
        int legendCol = 0;
        int legendRow = 0;

        for (Map.Entry<Category, Double> entry : categoryTotals.entrySet()) {
            Category cat = entry.getKey();
            Double amount = entry.getValue();
            
            // Hitung sudut slice
            double angle = (amount / totalExpense) * 360;

            // Gambar Slice
            g2d.setColor(colors[colorIndex % colors.length]);
            g2d.fill(new Arc2D.Double(chartX, chartY, chartDiameter, chartDiameter, currentAngle, angle, Arc2D.PIE));

            // Gambar Legenda
            drawLegend(g2d, cat.name(), amount, colors[colorIndex % colors.length], legendX + (legendCol * 150), legendY + (legendRow * 20));
            
            // Update posisi
            currentAngle += angle;
            colorIndex++;
            
            // Grid layout sederhana untuk legenda
            legendCol++;
            if (legendCol > 2) {
                legendCol = 0;
                legendRow++;
            }
        }
        
        // Gambar Total di tengah (opsional, buat donut chart)
        // g2d.setColor(Color.WHITE);
        // g2d.fillOval(chartX + chartDiameter/4, chartY + chartDiameter/4, chartDiameter/2, chartDiameter/2);
    }

    private void drawLegend(Graphics2D g2, String category, double amount, Color color, int x, int y) {
        g2.setColor(color);
        g2.fillRect(x, y, 15, 15);
        
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("Arial", Font.PLAIN, 12));
        
        double percent = (amount / totalExpense) * 100;
        String text = String.format("%s (%.1f%%)", category, percent);
        g2.drawString(text, x + 20, y + 12);
    }
}
