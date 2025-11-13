package com.financetracker.patterns.strategy;

import com.financetracker.model.Transaction;
import com.financetracker.model.TransactionType;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementasi Strategy: Laporan Harian.
 */
public class DailyReportStrategy implements ReportStrategy {
    @Override
    public String getReportName() {
        return "Laporan Harian";
    }

    @Override
    public String generateReport(List<Transaction> transactions) {
        LocalDate today = LocalDate.now();
        List<Transaction> dailyTx = transactions.stream()
                .filter(t -> t.getDate().equals(today))
                .collect(Collectors.toList());

        if (dailyTx.isEmpty()) {
            return "Tidak ada transaksi hari ini (" + today + ").";
        }

        double income = dailyTx.stream()
                .filter(t -> t.getType() == TransactionType.INCOME)
                .mapToDouble(Transaction::getAmount)
                .sum();
        double expense = dailyTx.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .mapToDouble(Transaction::getAmount)
                .sum();
        double net = income - expense;

        StringBuilder report = new StringBuilder();
        report.append(String.format("Laporan Harian (%s):\n", today));
        report.append("----------------------------\n");
        report.append(String.format("Total Pemasukan: Rp %,.2f\n", income));
        report.append(String.format("Total Pengeluaran: Rp %,.2f\n", expense));
        report.append(String.format("Total Bersih: Rp %,.2f\n\n", net));
        report.append("Detail Transaksi:\n");

        for (Transaction tx : dailyTx) {
            report.append(String.format("- (%s) %s: Rp %,.2f\n",
                    tx.getCategory(), tx.getDescription(), tx.getAmount()));
        }

        return report.toString();
    }
}