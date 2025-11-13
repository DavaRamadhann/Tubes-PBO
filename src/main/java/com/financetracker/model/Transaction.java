package com.financetracker.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Model data yang merepresentasikan satu transaksi.
 */
public class Transaction {
    private final String id;
    private final LocalDate date;
    private final String description;
    private final double amount;
    private final TransactionType type;
    private final Category category;

    public Transaction(String id, LocalDate date, String description, double amount, TransactionType type, Category category) {
        this.id = id;
        this.date = date;
        this.description = description;
        this.amount = amount;
        this.type = type;
        this.category = category;
    }

    // Getters
    public String getId() { return id; }
    public LocalDate getDate() { return date; }
    public String getDescription() { return description; }
    public double getAmount() { return amount; }
    public TransactionType getType() { return type; }
    public Category getCategory() { return category; }

    @Override
    public String toString() {
        return "Transaction{" +
                "id='" + id + '\'' +
                ", date=" + date +
                ", description='" + description + '\'' +
                ", amount=" + amount +
                ", type=" + type +
                ", category=" + category +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}