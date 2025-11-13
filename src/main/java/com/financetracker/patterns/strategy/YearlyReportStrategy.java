package com.financetracker.patterns.strategy;

import com.financetracker.model.Transaction;
import com.financetracker.model.TransactionType;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementasi Strategy: Laporan Tahunan.
 */
public class YearlyReportStrategy implements ReportStrategy {
    @Override
    public String getReportName() {
        return "Laporan Tahunan";
    }

    @Override
    public String generateReport(List<Transaction> transactions) {
        int currentYear = LocalDate.now().getYear();

        List<Transaction> yearlyTx = transactions.stream()
                .filter(t -> t.getDate().getYear() == currentYear)
                .collect(Collectors.toList());

        if (yearlyTx.isEmpty()) {
            return "Tidak ada transaksi tahun ini (" + currentYear + ").";
        }

        // Agregasi per bulan
        Map<String, Double> incomePerMonth = yearlyTx.stream()
                .filter(t -> t.getType() == TransactionType.INCOME)
                .collect(Collectors.groupingBy(
                        t -> t.getDate().getMonth().toString(),
                        Collectors.summingDouble(Transaction::getAmount)
                ));

        Map<String, Double> expensePerMonth = yearlyTx.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .collect(Collectors.groupingBy(
                        t -> t.getDate().getMonth().toString(),
                        Collectors.summingDouble(Transaction::getAmount)
                ));

        double totalIncome = incomePerMonth.values().stream().mapToDouble(Double::doubleValue).sum();
        double totalExpense = expensePerMonth.values().stream().mapToDouble(Double::doubleValue).sum();

        StringBuilder report = new StringBuilder();
        report.append(String.format("Laporan Tahunan (%d):\n", currentYear));
        report.append("----------------------------\n");
        report.append(String.format("Total Pemasukan: Rp %,.2f\n", totalIncome));
        report.append(String.format("Total Pengeluaran: Rp %,.2f\n", totalExpense));
        report.append(String.format("Total Bersih: Rp %,.2f\n\n", (totalIncome - totalExpense)));
        report.append("Ringkasan per Bulan:\n");

        for (java.time.Month month : java.time.Month.values()) {
            double income = incomePerMonth.getOrDefault(month.toString(), 0.0);
            double expense = expensePerMonth.getOrDefault(month.toString(), 0.0);
            if(income > 0 || expense > 0) {
                 report.append(String.format("- %s: Pemasukan Rp %,.2f | Pengeluaran Rp %,.2f | Bersih Rp %,.2f\n",
                    month, income, expense, (income - expense)));
            }
        }

        return report.toString();
    }
}