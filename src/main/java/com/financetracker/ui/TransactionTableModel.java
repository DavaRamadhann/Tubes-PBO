package com.financetracker.ui;

import com.financetracker.model.Transaction;

import javax.swing.table.AbstractTableModel;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;

/**
 * Model tabel kustom untuk JTable, agar JTable bisa
 * menampilkan data dari List<Transaction> secara langsung.
 */
public class TransactionTableModel extends AbstractTableModel {

    private final List<Transaction> transactions;
    private final String[] columnNames = {"ID", "Tanggal", "Deskripsi", "Tipe", "Kategori", "Jumlah (Rp)"};
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public TransactionTableModel() {
        this.transactions = new ArrayList<>();
    }

    /**
     * Mengatur ulang data di tabel dengan data baru.
     */
    public void setTransactions(List<Transaction> transactions) {
        this.transactions.clear();
        this.transactions.addAll(transactions);
        // Memberitahu JTable bahwa semua data telah berubah
        fireTableDataChanged();
    }

    /**
     * Mendapatkan objek Transaction pada baris tertentu.
     */
    public Transaction getTransactionAt(int rowIndex) {
        return transactions.get(rowIndex);
    }

    @Override
    public int getRowCount() {
        return transactions.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Transaction tx = transactions.get(rowIndex);
        switch (columnIndex) {
            case 0: return tx.getId();
            case 1: return tx.getDate().format(dateFormatter);
            case 2: return tx.getDescription();
            case 3: return tx.getType().toString();
            case 4: return tx.getCategory().toString();
            case 5: return String.format("%,.2f", tx.getAmount()); // Format mata uang
            default: return null;
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        // Kolom jumlah (Amount) diperlakukan sebagai String karena sudah diformat
        return String.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false; // Tabel tidak bisa diedit
    }
}