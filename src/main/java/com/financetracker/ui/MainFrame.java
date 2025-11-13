package com.financetracker.ui;

import com.financetracker.factory.TransactionFactory;
import com.financetracker.model.Category;
import com.financetracker.model.Transaction;
import com.financetracker.model.TransactionType;
import com.financetracker.patterns.strategy.*;
import com.financetracker.service.NotificationService;
import com.financetracker.service.OpenAIService;
import com.financetracker.service.ReportService;
import com.financetracker.service.TransactionService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * GUI Utama aplikasi (JFrame).
 * Mengintegrasikan semua service dan komponen UI.
 */
public class MainFrame extends JFrame {

    // Services
    private TransactionService transactionService;
    private ReportService reportService;
    private OpenAIService openAIService;

    // Komponen GUI Utama
    private JTable transactionTable;
    private TransactionTableModel tableModel;
    private JProgressBar budgetProgressBar;
    private JLabel budgetLabel;

    // Komponen Input
    private JTextField dateField;
    private JTextField descriptionField;
    private JTextField amountField;
    private JComboBox<TransactionType> typeComboBox;
    private JComboBox<Category> categoryComboBox;
    private JButton addButton;

    // Komponen Filter
    private JComboBox<Category> filterCategoryComboBox;
    private JTextField filterStartDateField;
    private JTextField filterEndDateField;
    private JButton filterButton;
    private JButton clearFilterButton;
    private JButton deleteButton;

    // Komponen Laporan & AI
    private JComboBox<ReportStrategy> reportComboBox;
    private JButton reportButton;
    private JButton aiAdviceButton;
    private JButton setBudgetButton;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public MainFrame() {
        initServices();
        initUI();
        loadInitialData();
    }

    /**
     * Inisialisasi semua service backend.
     */
    private void initServices() {
        // Service utama untuk data
        transactionService = new TransactionService();
        
        // Service untuk laporan (Strategy)
        reportService = new ReportService();
        
        // Service untuk AI
        openAIService = new OpenAIService();

        // Setup Observer Pattern
        // NotificationService (Observer) mendaftar ke TransactionService (Subject)
        NotificationService notificationLogger = new NotificationService();
        transactionService.addObserver(notificationLogger);
    }

    /**
     * Inisialisasi seluruh komponen GUI.
     */
    private void initUI() {
        setTitle("Personal Finance Tracker");
        setSize(1200, 800);
        setMinimumSize(new Dimension(1000, 600));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(240, 240, 240));

        // Padding untuk frame utama
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));

        // Panel Atas: Input dan Filter
        JPanel topPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        topPanel.add(createInputPanel());
        topPanel.add(createFilterPanel());
        topPanel.setOpaque(false);
        add(topPanel, BorderLayout.NORTH);

        // Panel Tengah: Tabel Transaksi
        add(createTablePanel(), BorderLayout.CENTER);

        // Panel Bawah: Budget, Laporan, dan AI
        add(createBottomPanel(), BorderLayout.SOUTH);
    }

    /**
     * Membuat Panel Input Transaksi Baru.
     */
    private JPanel createInputPanel() {
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        inputPanel.setBorder(new TitledBorder("Tambah Transaksi Baru"));
        inputPanel.setOpaque(false);

        dateField = new JTextField(LocalDate.now().format(dateFormatter), 10);
        descriptionField = new JTextField(20);
        amountField = new JTextField(10);
        typeComboBox = new JComboBox<>(TransactionType.values());
        categoryComboBox = new JComboBox<>(Category.values());
        addButton = new JButton("Tambah");

        inputPanel.add(new JLabel("Tgl (Y-M-D):"));
        inputPanel.add(dateField);
        inputPanel.add(new JLabel("Deskripsi:"));
        inputPanel.add(descriptionField);
        inputPanel.add(new JLabel("Jumlah:"));
        inputPanel.add(amountField);
        inputPanel.add(new JLabel("Tipe:"));
        inputPanel.add(typeComboBox);
        inputPanel.add(new JLabel("Kategori:"));
        inputPanel.add(categoryComboBox);
        inputPanel.add(addButton);

        // Action Listener
        addButton.addActionListener(e -> addTransaction());

        return inputPanel;
    }

    /**
     * Membuat Panel Filter Transaksi.
     */
    private JPanel createFilterPanel() {
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filterPanel.setBorder(new TitledBorder("Filter dan Aksi"));
        filterPanel.setOpaque(false);

        // Tambahkan "Semua" ke ComboBox Kategori Filter
        Category[] filterCategories = new Category[Category.values().length + 1];
        filterCategories[0] = null; // null merepresentasikan "Semua Kategori"
        System.arraycopy(Category.values(), 0, filterCategories, 1, Category.values().length);
        
        filterCategoryComboBox = new JComboBox<>(filterCategories);
        filterCategoryComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value == null) {
                    setText("Semua Kategori");
                }
                return this;
            }
        });

        filterStartDateField = new JTextField(10);
        filterEndDateField = new JTextField(10);
        filterButton = new JButton("Filter");
        clearFilterButton = new JButton("Reset");
        deleteButton = new JButton("Hapus Terpilih");
        deleteButton.setBackground(new Color(220, 50, 50));
        deleteButton.setForeground(Color.WHITE);
        deleteButton.setEnabled(false); // Aktif saat baris dipilih

        filterPanel.add(new JLabel("Kategori:"));
        filterPanel.add(filterCategoryComboBox);
        filterPanel.add(new JLabel("Tgl Mulai:"));
        filterPanel.add(filterStartDateField);
        filterPanel.add(new JLabel("Tgl Akhir:"));
        filterPanel.add(filterEndDateField);
        filterPanel.add(filterButton);
        filterPanel.add(clearFilterButton);
        filterPanel.add(Box.createHorizontalStrut(20)); // Spasi
        filterPanel.add(deleteButton);

        // Action Listeners
        filterButton.addActionListener(e -> filterTransactions());
        clearFilterButton.addActionListener(e -> loadInitialData());
        deleteButton.addActionListener(e -> deleteTransaction());

        return filterPanel;
    }


    /**
     * Membuat Panel Tabel di tengah.
     */
    private JScrollPane createTablePanel() {
        tableModel = new TransactionTableModel();
        transactionTable = new JTable(tableModel);

        transactionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        transactionTable.setFillsViewportHeight(true);
        transactionTable.setRowHeight(25);
        transactionTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 14));
        
        // Sembunyikan kolom ID (biasanya tidak perlu dilihat pengguna)
        transactionTable.removeColumn(transactionTable.getColumnModel().getColumn(0));

        // Listener untuk mengaktifkan tombol delete
        transactionTable.getSelectionModel().addListSelectionListener(e -> {
            deleteButton.setEnabled(transactionTable.getSelectedRow() >= 0);
        });

        JScrollPane scrollPane = new JScrollPane(transactionTable);
        scrollPane.setBorder(new TitledBorder("Daftar Transaksi"));
        return scrollPane;
    }

    /**
     * Membuat Panel Bawah (Budget, Laporan, AI).
     */
    private JPanel createBottomPanel() {
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setOpaque(false);

        // Panel Budget
        JPanel budgetPanel = new JPanel();
        budgetPanel.setLayout(new BoxLayout(budgetPanel, BoxLayout.Y_AXIS));
        budgetPanel.setBorder(new TitledBorder("Budget Bulanan"));
        
        budgetLabel = new JLabel("Pengeluaran: Rp 0,00 / Rp 0,00 (0%)");
        budgetLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        budgetProgressBar = new JProgressBar(0, 100);
        budgetProgressBar.setStringPainted(true);
        budgetProgressBar.setForeground(Color.GREEN); // Default
        
        setBudgetButton = new JButton("Set Budget");
        setBudgetButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        setBudgetButton.addActionListener(e -> setBudget());

        budgetPanel.add(budgetLabel);
        budgetPanel.add(Box.createVerticalStrut(5));
        budgetPanel.add(budgetProgressBar);
        budgetPanel.add(Box.createVerticalStrut(5));
        budgetPanel.add(setBudgetButton);

        // Panel Laporan & AI
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        actionPanel.setBorder(new TitledBorder("Laporan dan Analisis"));

        // Inisialisasi strategy untuk ComboBox
        ReportStrategy[] strategies = {
            new DailyReportStrategy(),
            new MonthlyReportStrategy(),
            new YearlyReportStrategy()
            // Tambahkan strategy lain di sini jika ada
        };
        reportComboBox = new JComboBox<>(strategies);
        reportComboBox.setRenderer(new DefaultListCellRenderer() {
             @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ReportStrategy) {
                    setText(((ReportStrategy) value).getReportName());
                }
                return this;
            }
        });

        reportButton = new JButton("Buat Laporan");
        aiAdviceButton = new JButton("Dapatkan Saran Keuangan (AI)");
        aiAdviceButton.setBackground(new Color(60, 180, 75));
        aiAdviceButton.setForeground(Color.WHITE);
        
        actionPanel.add(new JLabel("Jenis Laporan:"));
        actionPanel.add(reportComboBox);
        actionPanel.add(reportButton);
        actionPanel.add(Box.createHorizontalStrut(20));
        actionPanel.add(aiAdviceButton);

        // Action Listeners
        reportButton.addActionListener(e -> generateReport());
        aiAdviceButton.addActionListener(e -> getAIAdvice());

        bottomPanel.add(budgetPanel, BorderLayout.WEST);
        bottomPanel.add(actionPanel, BorderLayout.CENTER);

        return bottomPanel;
    }

    // --- Logika Aksi ---

    /**
     * Memuat data awal ke tabel dan memperbarui budget.
     */
    private void loadInitialData() {
        refreshTable(transactionService.getAllTransactions());
        refreshBudget();
    }

    /**
     * Memperbarui tabel dengan list transaksi yang diberikan.
     */
    private void refreshTable(List<Transaction> transactions) {
        tableModel.setTransactions(transactions);
        // Sembunyikan lagi kolom ID jika tabel di-refresh
        transactionTable.removeColumn(transactionTable.getColumnModel().getColumn(0));
        deleteButton.setEnabled(false);
    }

    /**
     * Memperbarui Progress Bar dan Label Budget.
     */
    private void refreshBudget() {
        double budget = transactionService.getMonthlyBudget();
        double spending = transactionService.getCurrentMonthSpending();
        int percentage = 0;

        if (budget > 0) {
            percentage = (int) ((spending / budget) * 100);
        }

        budgetLabel.setText(String.format("Pengeluaran: Rp %,.2f / Rp %,.2f", spending, budget));
        budgetProgressBar.setValue(percentage);
        budgetProgressBar.setString(percentage + "%");

        // Ganti warna progress bar
        if (percentage > 90) {
            budgetProgressBar.setForeground(new Color(210, 40, 40)); // Merah
        } else if (percentage > 70) {
            budgetProgressBar.setForeground(new Color(230, 190, 40)); // Kuning
        } else {
            budgetProgressBar.setForeground(new Color(40, 180, 90)); // Hijau
        }
    }

    /**
     * Logika untuk menambah transaksi baru.
     */
    private void addTransaction() {
        try {
            LocalDate date = LocalDate.parse(dateField.getText(), dateFormatter);
            String description = descriptionField.getText();
            double amount = Double.parseDouble(amountField.getText());
            TransactionType type = (TransactionType) typeComboBox.getSelectedItem();
            Category category = (Category) categoryComboBox.getSelectedItem();

            Transaction newTransaction = TransactionFactory.createTransaction(date, description, amount, type, category);
            transactionService.addTransaction(newTransaction);

            // Refresh UI
            loadInitialData();

            // Reset field input
            descriptionField.setText("");
            amountField.setText("");
            dateField.setText(LocalDate.now().format(dateFormatter));

        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Format tanggal salah. Gunakan yyyy-MM-dd.", "Error Input", JOptionPane.ERROR_MESSAGE);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Format jumlah salah. Masukkan angka.", "Error Input", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error Input", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Logika untuk menghapus transaksi yang dipilih.
     */
    private void deleteTransaction() {
        int selectedRow = transactionTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Pilih transaksi yang ingin dihapus.", "Tidak Ada Pilihan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Konversi index view ke index model jika tabel di-sort (meskipun saat ini tidak)
        int modelRow = transactionTable.convertRowIndexToModel(selectedRow);
        String transactionId = (String) tableModel.getValueAt(modelRow, 0); // Ambil ID dari kolom 0

        int confirm = JOptionPane.showConfirmDialog(this,
                "Anda yakin ingin menghapus transaksi ini?",
                "Konfirmasi Hapus",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            transactionService.deleteTransaction(transactionId);
            loadInitialData(); // Refresh tabel dan budget
        }
    }

    /**
     * Logika untuk memfilter transaksi.
     */
    private void filterTransactions() {
        try {
            Category category = (Category) filterCategoryComboBox.getSelectedItem(); // Bisa null
            LocalDate startDate = null;
            LocalDate endDate = null;

            if (!filterStartDateField.getText().trim().isEmpty()) {
                startDate = LocalDate.parse(filterStartDateField.getText(), dateFormatter);
            }
            if (!filterEndDateField.getText().trim().isEmpty()) {
                endDate = LocalDate.parse(filterEndDateField.getText(), dateFormatter);
            }

            List<Transaction> filteredList = transactionService.filterTransactions(category, startDate, endDate);
            refreshTable(filteredList);

        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Format tanggal filter salah. Gunakan yyyy-MM-dd.", "Error Filter", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Logika untuk mengubah budget bulanan.
     */
    private void setBudget() {
        String currentBudget = String.valueOf(transactionService.getMonthlyBudget());
        String input = JOptionPane.showInputDialog(this, "Masukkan budget bulanan baru:", currentBudget);

        if (input != null) {
            try {
                double newBudget = Double.parseDouble(input);
                if (newBudget < 0) throw new NumberFormatException();
                
                transactionService.setMonthlyBudget(newBudget);
                refreshBudget(); // Perbarui tampilan budget
                transactionService.checkBudgetStatus(); // Cek ulang status

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Masukkan angka positif yang valid untuk budget.", "Error Input", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Logika untuk membuat laporan (Strategy Pattern).
     */
    private void generateReport() {
        ReportStrategy selectedStrategy = (ReportStrategy) reportComboBox.getSelectedItem();
        if (selectedStrategy == null) {
            JOptionPane.showMessageDialog(this, "Silakan pilih jenis laporan.", "Error Laporan", JOptionPane.ERROR_MESSAGE);
            return;
        }

        reportService.setStrategy(selectedStrategy);
        String reportContent = reportService.generateReport(transactionService.getAllTransactions());

        // Tampilkan laporan di JTextArea dalam JScrollPane
        JTextArea textArea = new JTextArea(reportContent);
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(600, 400));

        JOptionPane.showMessageDialog(this,
                scrollPane,
                selectedStrategy.getReportName(),
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Logika untuk meminta saran dari AI.
     */
    private void getAIAdvice() {
        // Buat ringkasan transaksi
        String summary = reportService.generateReport(transactionService.getAllTransactions());
        
        // Tampilkan dialog loading
        JDialog loadingDialog = new JDialog(this, "Meminta Saran AI...", true);
        JProgressBar loadingBar = new JProgressBar();
        loadingBar.setIndeterminate(true);
        loadingBar.setStringPainted(true);
        loadingBar.setString("Menghubungi OpenAI, mohon tunggu...");
        loadingDialog.add(new JScrollPane(loadingBar)); // (Salah, harusnya add ke content pane)
        loadingDialog.getContentPane().add(loadingBar, BorderLayout.CENTER);
        loadingDialog.pack();
        loadingDialog.setSize(300, 75);
        loadingDialog.setLocationRelativeTo(this);
        loadingDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        // Gunakan SwingWorker untuk tugas background (API call)
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                // Panggil service AI
                return openAIService.getFinancialAdvice(summary);
            }

            @Override
            protected void done() {
                // Tutup dialog loading
                loadingDialog.dispose();
                try {
                    String aiResponse = get(); // Ambil hasil dari doInBackground()
                    
                    // Tampilkan hasil AI di JTextArea
                    JTextArea textArea = new JTextArea(aiResponse);
                    textArea.setWrapStyleWord(true);
                    textArea.setLineWrap(true);
                    textArea.setEditable(false);
                    textArea.setBackground(getBackground()); // Latar belakang sama
                    textArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
                    
                    JScrollPane scrollPane = new JScrollPane(textArea);
                    scrollPane.setPreferredSize(new Dimension(600, 400));
                    scrollPane.setBorder(null);

                    JOptionPane.showMessageDialog(MainFrame.this,
                            scrollPane,
                            "Saran Keuangan dari AI",
                            JOptionPane.INFORMATION_MESSAGE);

                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(MainFrame.this,
                            "Gagal mendapatkan saran: " + e.getMessage(),
                            "Error AI",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute(); // Mulai SwingWorker
        loadingDialog.setVisible(true); // Tampilkan dialog loading (akan diblok sampai worker selesai)
    }
}