package com.financetracker.patterns.strategy;

import com.financetracker.model.Transaction;

import java.util.List;

/**
 * Interface Strategy untuk Strategy Pattern.
 * Mendefinisikan operasi untuk menghasilkan laporan.
 */
public interface ReportStrategy {
    String getReportName();
    String generateReport(List<Transaction> transactions);
}