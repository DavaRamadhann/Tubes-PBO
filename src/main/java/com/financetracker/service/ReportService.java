package com.financetracker.service;

import com.financetracker.model.Transaction;
import com.financetracker.patterns.strategy.ReportStrategy;

import java.util.List;

/**
 * Context untuk Strategy Pattern.
 * Service ini menggunakan (HAS-A) ReportStrategy.
 * GUI akan berinteraksi dengan service ini, bukan langsung ke strategy-nya.
 */
public class ReportService {
    
    private ReportStrategy strategy;

    public ReportService() {
        // Default strategy (opsional)
        this.strategy = null;
    }

    public void setStrategy(ReportStrategy strategy) {
        this.strategy = strategy;
    }

    /**
     * Menjalankan strategi yang saat ini di-set.
     */
    public String generateReport(List<Transaction> transactions) {
        if (strategy == null) {
            return "Silakan pilih jenis laporan terlebih dahulu.";
        }
        return strategy.generateReport(transactions);
    }
}