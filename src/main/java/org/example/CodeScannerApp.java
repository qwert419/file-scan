package org.example;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.table.DefaultTableModel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedWriter;
import java.io.FileWriter;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.IOException;
import javax.swing.JProgressBar;
import javax.swing.Box;
import java.util.concurrent.*;
import java.util.ArrayList;
import javax.swing.ImageIcon;

public class CodeScannerApp extends JFrame {
    private static final String TITLE = "代码扫描工具 - 公众号：辰星安全 v1.3";
    private static final int THREAD_POOL_SIZE = 2;     // 减少线程数
    private static final int SCAN_DELAY = 2000;        // 增加延时到2秒
    private static final int HTTP_TIMEOUT = 10000;     // 增加HTTP超时时间到10秒
    private final Object progressLock = new Object();
    private volatile boolean isScanning = false;  // 添加扫描状态标志
    private JTextField urlField;
    private JTextField dirField;
    private JTextField proxyField;
    private JComboBox<String> languageComboBox;
    private JButton scanButton;
    private JButton stopButton;
    private JProgressBar progressBar;
    private DefaultTableModel tableModel;
    private JTable resultTable;
    private ExecutorService executorService;
    private SwingWorker<Void, ScanResult> currentWorker;
    private SimpleDateFormat dateFormat;
    private JScrollPane scrollPane;  // 添加滚动面板
    private JPanel mainPanel;

    // 使用静态方法创建实例
    public static CodeScannerApp getInstance() {
        return new CodeScannerApp();
    }

    // 私有构造函数
    public CodeScannerApp() {
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        setTitle("代码扫描工具 - 公众号：辰星安全 v1.3");
        
        try {
            ImageIcon icon = new ImageIcon(getClass().getResource("/icon.png"));
            setIconImage(icon.getImage());
        } catch (Exception e) {
            e.printStackTrace();
        }

        executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        initUI();
        initLayout();
    }

    private void initUI() {
        // 初始化表格
        String[] columnNames = {"序号", "文件路径", "URL", "状态码", "响应大小"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        resultTable = new JTable(tableModel);
        resultTable.getTableHeader().setReorderingAllowed(false);
        resultTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        
        // 设置列宽
        resultTable.getColumnModel().getColumn(0).setPreferredWidth(50);  // 序号列
        resultTable.getColumnModel().getColumn(1).setPreferredWidth(300); // 文件路径列
        resultTable.getColumnModel().getColumn(2).setPreferredWidth(400); // URL列
        resultTable.getColumnModel().getColumn(3).setPreferredWidth(80);  // 状态码列
        resultTable.getColumnModel().getColumn(4).setPreferredWidth(100); // 响应大小列

        // 创建滚动面板
        scrollPane = new JScrollPane(resultTable);

        // 初始化按钮
        scanButton = new JButton("开始扫描");
        stopButton = new JButton("停止扫描");
        stopButton.setEnabled(false);  // 初始状态下禁用停止按钮

        // 添加按钮事件监听器
        scanButton.addActionListener(this::onScan);
        stopButton.addActionListener(e -> stopScan());

        // 创建输入面板
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // URL输入区域
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel urlLabel = new JLabel("测试URL:");
        urlLabel.setFont(new Font("微软雅黑", Font.BOLD, 12));
        inputPanel.add(urlLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        urlField = new JTextField();
        urlField.setPreferredSize(new Dimension(300, 25));
        inputPanel.add(urlField, gbc);

        // 语言选择区域
        gbc.gridx = 2;
        gbc.weightx = 0;
        JLabel langLabel = new JLabel("文件类型:");
        langLabel.setFont(new Font("微软雅黑", Font.BOLD, 12));
        inputPanel.add(langLabel, gbc);

        gbc.gridx = 3;
        gbc.weightx = 0.3;
        String[] languages = {
            "全部",
            "JSP",
            "JSPX",
            "ASP",
            "ASPX",
            "ASHX",
            "HTML",
            "JS"
        };
        languageComboBox = new JComboBox<>(languages);
        languageComboBox.setPreferredSize(new Dimension(100, 25));
        inputPanel.add(languageComboBox, gbc);

        // 代理输入区域
        gbc.gridx = 4;
        gbc.weightx = 0;
        JLabel proxyLabel = new JLabel("代理设置:");
        proxyLabel.setFont(new Font("微软雅黑", Font.BOLD, 12));
        inputPanel.add(proxyLabel, gbc);

        gbc.gridx = 5;
        gbc.weightx = 0.5;
        proxyField = new JTextField();
        proxyField.setPreferredSize(new Dimension(150, 25));
        proxyField.setToolTipText("格式: http://127.0.0.1:8080 或 socks5://127.0.0.1:1080");
        inputPanel.add(proxyField, gbc);

        // 目录选择区域
        gbc.gridx = 0;
        gbc.gridy = 1;
        JLabel dirLabel = new JLabel("扫描目录:");
        dirLabel.setFont(new Font("微软雅黑", Font.BOLD, 12));
        inputPanel.add(dirLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        dirField = new JTextField();
        dirField.setPreferredSize(new Dimension(300, 25));
        inputPanel.add(dirField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        JButton browseButton = new JButton("选择目录");
        browseButton.setBackground(new Color(240, 240, 240));
        browseButton.setFocusPainted(false);
        browseButton.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        inputPanel.add(browseButton, gbc);

        // 创建按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        buttonPanel.add(scanButton);
        buttonPanel.add(stopButton);

        // 创建进度条
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("准备就绪");
        progressBar.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        progressBar.setPreferredSize(new Dimension(200, 20));
        buttonPanel.add(Box.createHorizontalStrut(10));
        buttonPanel.add(progressBar);

        gbc.gridx = 3;
        gbc.gridwidth = 3;
        inputPanel.add(buttonPanel, gbc);

        // 创建表格的右键菜单
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem copyUrlItem = new JMenuItem("复制URL");
        JMenuItem exportItem = new JMenuItem("导出结果");

        popupMenu.add(copyUrlItem);
        popupMenu.addSeparator();
        popupMenu.add(exportItem);

        // 添加表格的右键菜单监听
        resultTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = resultTable.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        resultTable.setRowSelectionInterval(row, row);
                        popupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
        });

        // 复制URL功能
        copyUrlItem.addActionListener(e -> {
            int selectedRow = resultTable.getSelectedRow();
            if (selectedRow != -1) {
                int urlColumn = 2; // URL 所在的列索引
                String url = (String) resultTable.getValueAt(selectedRow, urlColumn);
                StringSelection selection = new StringSelection(url);
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
            }
        });

        // 导出功能
        exportItem.addActionListener(e -> exportResults());

        // 创建工具栏
        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton exportButton = new JButton("导出结果");
        exportButton.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        exportButton.addActionListener(e -> exportResults());
        toolBar.add(exportButton);

        // 将工具栏添加到主面板
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.add(inputPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(toolBar, BorderLayout.SOUTH);

        // 设置窗口
        add(mainPanel);
        setSize(800, 600);
        setLocationRelativeTo(null);
        
        // 添加事件监听器
        browseButton.addActionListener(this::onBrowse);
    }

    private void initLayout() {
        mainPanel = new JPanel(new BorderLayout(5, 5));
        
        // 创建顶部面板
        JPanel topPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 第一行：测试URL和文件类型
        gbc.gridx = 0; gbc.gridy = 0;
        topPanel.add(new JLabel("测试URL:"), gbc);
        
        gbc.gridx = 1; gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.gridwidth = 2;
        topPanel.add(urlField, gbc);
        
        gbc.gridx = 3; gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        topPanel.add(new JLabel("文件类型:"), gbc);
        
        gbc.gridx = 4; gbc.gridy = 0;
        topPanel.add(languageComboBox, gbc);
        
        gbc.gridx = 5; gbc.gridy = 0;
        topPanel.add(new JLabel("代理设置:"), gbc);
        
        gbc.gridx = 6; gbc.gridy = 0;
        topPanel.add(proxyField, gbc);

        // 第二行：扫描目录
        gbc.gridx = 0; gbc.gridy = 1;
        topPanel.add(new JLabel("扫描目录:"), gbc);
        
        gbc.gridx = 1; gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.gridwidth = 6;
        topPanel.add(dirField, gbc);

        // 创建按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        buttonPanel.add(scanButton);
        buttonPanel.add(stopButton);

        // 创建进度条面板
        JPanel progressPanel = new JPanel(new BorderLayout());
        progressPanel.add(progressBar, BorderLayout.CENTER);
        progressBar.setPreferredSize(new Dimension(progressBar.getPreferredSize().width, 25));

        // 组装顶部区域
        JPanel northPanel = new JPanel(new BorderLayout(5, 5));
        northPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        northPanel.add(topPanel, BorderLayout.NORTH);
        northPanel.add(buttonPanel, BorderLayout.CENTER);
        northPanel.add(progressPanel, BorderLayout.SOUTH);

        // 设置表格面板
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
        tablePanel.add(scrollPane, BorderLayout.CENTER);

        // 组装主面板
        mainPanel.add(northPanel, BorderLayout.NORTH);
        mainPanel.add(tablePanel, BorderLayout.CENTER);

        // 设置窗口
        setContentPane(mainPanel);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 600);  // 设置固定窗口大小
        setLocationRelativeTo(null);
    }

    private void onBrowse(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogTitle("选择扫描目录");
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            dirField.setText(fileChooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void scanFiles() {
        File dir = new File(dirField.getText().trim());
        if (!dir.exists() || !dir.isDirectory()) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, "无效的目录路径", "错误", JOptionPane.ERROR_MESSAGE);
                stopScan();
            });
            return;
        }

        String baseUrl = urlField.getText().trim();
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }

        // 先统计文件总数
        AtomicInteger totalFiles = new AtomicInteger(0);
        countFiles(dir, totalFiles);

        if (totalFiles.get() == 0) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, "目录为空", "提示", JOptionPane.INFORMATION_MESSAGE);
                stopScan();
            });
            return;
        }

        // 开始处理文件
        processDirectory(dir, baseUrl, totalFiles.get());
    }

    private void processDirectory(File dir, String baseUrl, int totalFiles) {
        if (!isScanning) return;

        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (!isScanning) break;

            if (file.isDirectory()) {
                processDirectory(file, baseUrl, totalFiles);
            } else if (isTargetFile(file)) {
                String relativePath = getRelativePath(new File(dirField.getText()), file);
                String url = baseUrl + relativePath.replace('\\', '/');

                try {
                    ProxyConfig proxyConfig = getProxyConfig();
                    HttpResponse response = HttpFetcher.fetchUrlWithResponse(url, proxyConfig);
                    
                    SwingUtilities.invokeLater(() -> {
                        int rowCount = tableModel.getRowCount() + 1;
                        tableModel.addRow(new Object[]{
                            rowCount,
                            relativePath,
                            url,
                            response.getStatusCode(),
                            formatFileSize(response.getSize())
                        });
                        resultTable.scrollRectToVisible(resultTable.getCellRect(
                            resultTable.getRowCount() - 1, 0, true));
                    });

                    // 更新进度
                    int currentCount = tableModel.getRowCount();
                    updateProgress(currentCount, totalFiles);

                    // 添加延时，避免请求过快
                    Thread.sleep(SCAN_DELAY);
                } catch (Exception e) {
                    e.printStackTrace();
                    // 发生错误时也要添加到表格
                    SwingUtilities.invokeLater(() -> {
                        int rowCount = tableModel.getRowCount() + 1;
                        tableModel.addRow(new Object[]{
                            rowCount,
                            relativePath,
                            url,
                            "错误",
                            "0 B"
                        });
                    });
                }
            }
        }
    }

    private void updateProgress(int current, int total) {
        if (total > 0) {
            int percentage = (int) ((current / (double) total) * 100);
            SwingUtilities.invokeLater(() -> {
                progressBar.setValue(percentage);
                progressBar.setString(String.format("进度: %d/%d (%d%%)", 
                    current, total, percentage));
            });
        }
    }

    private void onScan(ActionEvent e) {
        if (urlField.getText().trim().isEmpty() || dirField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入测试URL和扫描目录", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        tableModel.setRowCount(0);
        scanButton.setEnabled(false);
        stopButton.setEnabled(true);
        progressBar.setValue(0);
        progressBar.setString("正在扫描...");
        isScanning = true;

        currentWorker = new SwingWorker<Void, ScanResult>() {
            @Override
            protected Void doInBackground() throws Exception {
                scanFiles();
                return null;
            }

            @Override
            protected void done() {
                if (!isCancelled()) {
                    scanButton.setEnabled(true);
                    stopButton.setEnabled(false);
                    progressBar.setString("扫描完成");
                }
                isScanning = false;
            }
        };

        currentWorker.execute();
    }

    private void stopScan() {
        isScanning = false;
        if (currentWorker != null) {
            currentWorker.cancel(true);
        }
        scanButton.setEnabled(true);
        stopButton.setEnabled(false);
        progressBar.setString("扫描已停止");
    }

    private String[] getFileExtensions(String language) {
        switch (language) {
            case "JSP":
                return new String[]{".jsp"};
            case "JSPX":
                return new String[]{".jspx"};
            case "ASP":
                return new String[]{".asp"};
            case "ASPX":
                return new String[]{".aspx"};
            case "ASHX":
                return new String[]{".ashx"};
            case "HTML":
                return new String[]{".html", ".htm"};
            case "JS":
                return new String[]{".js"};
            case "全部":
            default:
                return new String[]{
                    ".jsp", ".jspx", ".asp", ".aspx", 
                    ".ashx", ".html", ".htm", ".js"
                };
        }
    }

    private String getRelativePath(File baseDir, File file) {
        String basePath = baseDir.getAbsolutePath();
        String filePath = file.getAbsolutePath();
        
        // 确保路径分隔符统一
        basePath = basePath.replace('\\', '/');
        filePath = filePath.replace('\\', '/');
        
        // 如果基础路径不以/结尾，添加/
        if (!basePath.endsWith("/")) {
            basePath += "/";
        }
        
        // 获取相对路径
        if (filePath.startsWith(basePath)) {
            return filePath.substring(basePath.length());
        }
        
        return file.getName();
    }

    private String formatFileSize(long size) {
        if (size < 0) return "未知";
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.2f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.2f MB", size / (1024.0 * 1024));
        return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
    }

    private void exportResults() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("保存结果");
        fileChooser.setFileFilter(new FileNameExtensionFilter("文本文件 (*.txt)", "txt"));
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".txt")) {
                file = new File(file.getAbsolutePath() + ".txt");
            }
            
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                int rowCount = tableModel.getRowCount();
                int urlColumn = 2; // URL 所在的列索引
                
                for (int i = 0; i < rowCount; i++) {
                    String url = (String) tableModel.getValueAt(i, urlColumn);
                    // 提取URL中的路径部分
                    String path = extractPathFromUrl(url);
                    if (path != null && !path.isEmpty()) {
                        writer.write(path);
                        writer.newLine();
                    }
                }
                
                JOptionPane.showMessageDialog(this, 
                    "结果已成功导出到：" + file.getAbsolutePath(), 
                    "导出成功", 
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                    "导出失败：" + ex.getMessage(),
                    "错误",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private String extractPathFromUrl(String url) {
        try {
            // 移除基础URL部分
            String baseUrl = urlField.getText().trim();
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            
            if (url.startsWith(baseUrl)) {
                String path = url.substring(baseUrl.length());
                // 确保路径以/开头
                if (!path.startsWith("/")) {
                    path = "/" + path;
                }
                return path;
            }
            return url;
        } catch (Exception e) {
            return url;
        }
    }

    // 在窗口关闭时确保清理资源
    @Override
    public void dispose() {
        isScanning = false;
        if (currentWorker != null) {
            currentWorker.cancel(true);
        }
        if (executorService != null) {
            executorService.shutdownNow();
        }
        super.dispose();
    }

    // 添加日志输出方法
    private void logDebug(String message) {
        System.out.println("[DEBUG] " + message);
    }

    private ProxyConfig getProxyConfig() {
        String proxyText = proxyField.getText().trim();
        if (proxyText.isEmpty()) {
            return null;
        }
        try {
            String[] parts = proxyText.split(":");
            if (parts.length == 2) {
                String host = parts[0];
                int port = Integer.parseInt(parts[1]);
                return new ProxyConfig(host, port, true);  // 添加了 true 参数
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // 添加 countFiles 方法
    private void countFiles(File dir, AtomicInteger count) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    countFiles(file, count);
                } else if (isTargetFile(file)) {
                    count.incrementAndGet();
                }
            }
        }
    }

    // 添加 isTargetFile 方法
    private boolean isTargetFile(File file) {
        String selectedType = (String) languageComboBox.getSelectedItem();
        String fileName = file.getName().toLowerCase();
        
        if ("全部".equals(selectedType)) {
            return fileName.endsWith(".aspx") || 
                   fileName.endsWith(".php") || 
                   fileName.endsWith(".jsp");
        }
        
        return fileName.endsWith("." + selectedType.toLowerCase());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            CodeScannerApp app = new CodeScannerApp();
            app.setVisible(true);
        });
    }
}