package com.financetracker.model;

/**
 * Enum untuk kategori transaksi.
 */
public enum Category {
    GAJI("Gaji (Pemasukan)"),
    INVESTASI("Investasi (Pemasukan)"),
    MAKANAN("Makanan"),
    TRANSPORTASI("Transportasi"),
    TAGIHAN("Tagihan"),
    HIBURAN("Hiburan"),
    KESEHATAN("Kesehatan"),
    PENDIDIKAN("Pendidikan"),
    BELANJA("Belanja"),
    LAINNYA("Lainnya");

    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}