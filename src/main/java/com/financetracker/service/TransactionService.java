package com.financetracker.service;

import com.financetracker.model.Category;
import com.financetracker.model.Transaction;
import com.financetracker.model.TransactionType;
import com.financetracker.patterns.observer.BudgetObserver;
import com.financetracker.patterns.observer.BudgetSubject;
import com.financetracker.storage.StorageManager;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TransactionService — versi final dan ditingkatkan.
 * 
 * - Mengelola operasi CRUD transaksi.
 * - Mengelola budget bulanan (dengan persistence).
 * - Memberikan notifikasi budget via Observer Pattern.
 * - Memastikan filter, perhitungan, dan penyimpanan stabil dan aman.
 */
public class TransactionService implements BudgetSubject {

    private List<Transaction> transactions;
    private final StorageManager storageManager;
    private final List<BudgetObserver> observers;

    /** Budget bulanan dalam rupiah */
    private double monthlyBudget = 0.0;
    private boolean budgetNotificationSent = false;

    public TransactionService() {
        this.storageManager = StorageManager.getInstance();
        this.transactions = storageManager.loadTransactions();
        this.observers = new ArrayList<>();

        // Load budget dari storage jika sistem Anda mendukung
        Double savedBudget = storageManager.loadMonthlyBudget();
        this.monthlyBudget = savedBudget != null ? savedBudget : 2000000.0; // default 2 juta
    }

    // ============================================================
    //                      CRUD TRANSAKSI
    // ============================================================

    /**
     * Tambah transaksi baru.
     */
    public void addTransaction(Transaction transaction) {
        this.transactions.add(transaction);
        saveAndRecalculate();
    }

    /**
     * Hapus transaksi berdasarkan ID unik.
     */
    public void deleteTransaction(String id) {
        transactions.removeIf(tx -> tx.getId().equals(id));
        saveAndRecalculate();
    }

    /**
     * Mengembalikan seluruh transaksi dalam bentuk list baru (safe-copy).
     */
    public List<Transaction> getAllTransactions() {
        return new ArrayList<>(transactions);
    }

    /**
     * Filter transaksi berdasarkan kategori dan/atau rentang tanggal.
     */
    public List<Transaction> filterTransactions(Category category, LocalDate startDate, LocalDate endDate) {
        return transactions.stream()
                .filter(tx -> category == null || tx.getCategory() == category)
                .filter(tx -> startDate == null || !tx.getDate().isBefore(startDate))
                .filter(tx -> endDate == null || !tx.getDate().isAfter(endDate))
                .collect(Collectors.toList());
    }

    /**
     * Menyimpan, refresh data budget, dan memberi notifikasi bila perlu.
     */
    private void saveAndRecalculate() {
        storageManager.saveTransactions(transactions);
        checkBudgetStatus();
    }

    // ============================================================
    //                      BUDGET MANAGEMENT
    // ============================================================

    /**
     * Mendapatkan budget bulanan dalam rupiah.
     */
    public double getMonthlyBudget() {
        return monthlyBudget;
    }

    /**
     * Meng-set budget baru (disimpan permanen di StorageManager).
     */
    public void setMonthlyBudget(double monthlyBudget) {
        if (monthlyBudget < 0)
            throw new IllegalArgumentException("Budget tidak boleh negatif.");

        this.monthlyBudget = monthlyBudget;

        // persist budget
        storageManager.saveMonthlyBudget(monthlyBudget);

        // setelah ubah budget lakukan pengecekan ulang
        checkBudgetStatus();
    }

    /**
     * Menghitung total pengeluaran bulan berjalan.
     */
    public double getCurrentMonthSpending() {
        LocalDate today = LocalDate.now();
        Month currentMonth = today.getMonth();
        int currentYear = today.getYear();

        return transactions.stream()
                .filter(tx -> tx.getType() == TransactionType.EXPENSE)
                .filter(tx -> tx.getDate().getMonth() == currentMonth &&
                              tx.getDate().getYear() == currentYear)
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

    /**
     * Memeriksa apakah pengeluaran melewati budget.
     * - Notifikasi hanya dikirim 1 kali.
     * - Jika transaksi dihapus sehingga pengeluaran turun, notifikasi di-reset.
     */
    public void checkBudgetStatus() {
        if (monthlyBudget <= 0) {
            budgetNotificationSent = false;
            return;
        }

        double spending = getCurrentMonthSpending();
        double percentage = (spending / monthlyBudget) * 100;

        // Kirim notifikasi 1 kali ketika melewati budget
        if (percentage >= 100 && !budgetNotificationSent) {
            String message = String.format(
                    "⚠️ BUDGET WARNING!\n\n" +
                    "Pengeluaran bulan ini: Rp %,.2f\n" +
                    "Budget Anda: Rp %,.2f\n\n" +
                    "Pengeluaran telah melampaui batas!",
                    spending, monthlyBudget
            );
            notifyObservers(message);
            budgetNotificationSent = true;
        }

        // Reset flag jika kembali di bawah budget (misal karena penghapusan transaksi)
        if (percentage < 100) {
            budgetNotificationSent = false;
        }
    }

    // ============================================================
    //                      OBSERVER PATTERN
    // ============================================================

    @Override
    public void addObserver(BudgetObserver observer) {
        observers.add(observer);
    }

    @Override
    public void removeObserver(BudgetObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(String message) {
        for (BudgetObserver observer : observers) {
            observer.update(message);
        }
    }
}
