package com.mysterymansion;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.mysterymansion.ui.MansionFrame;
import com.mysterymansion.util.DbConfig;

public class Main {
    public static void main(String[] args) {
        // Fail early with a clear message if the DB settings are missing
        try {
            DbConfig.validate();
        } catch (IllegalStateException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Configuration needed", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Force cross-platform look and feel so custom button colors render correctly on macOS
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(MansionFrame::new);
    }
}
