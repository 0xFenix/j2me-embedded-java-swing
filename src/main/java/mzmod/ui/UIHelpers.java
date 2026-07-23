package mzmod.ui;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

/**
 * Factory methods for styled Swing components - Dark Mode with Radius.
 */
public final class UIHelpers {

    private UIHelpers() {}

    // ── Rounded border helpers ──────────────────────────────────────
    public static Border roundedBorder(Color color, int radius) {
        return BorderFactory.createCompoundBorder(
                new LineBorder(color, 1, true),
                new EmptyBorder(radius / 2, radius, radius / 2, radius)
        );
    }

    public static Border roundedBorder(Color color, int radius, int padH, int padV) {
        return BorderFactory.createCompoundBorder(
                new LineBorder(color, 1, true),
                new EmptyBorder(padV, padH, padV, padH)
        );
    }

    // ── Primary Button ──────────────────────────────────────────────
    public static JButton primaryButton(String text, Icon icon) {
        JButton btn = new JButton(text, icon);
        btn.setFont(Theme.FONT_BOLD);
        btn.setBackground(Theme.ACCENT);
        btn.setForeground(Theme.ON_PRIMARY);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(180, Theme.BUTTON_HEIGHT + 6));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            Color orig;
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                orig = btn.getBackground();
                btn.setBackground(Theme.ACCENT_HOVER);
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (orig != null) btn.setBackground(orig);
            }
        });
        return btn;
    }

    // ── Secondary Button ────────────────────────────────────────────
    public static JButton secondaryButton(String text, Icon icon) {
        JButton btn = new JButton(text, icon);
        btn.setFont(Theme.FONT_MEDIUM);
        btn.setBackground(Theme.SURFACE);
        btn.setForeground(Theme.TEXT_PRIMARY);
        btn.setFocusPainted(false);
        btn.setBorder(new LineBorder(Theme.BORDER, 1, true));
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(160, Theme.BUTTON_HEIGHT));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            Color orig;
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                orig = btn.getBackground();
                btn.setBackground(Theme.TAB_HOVER);
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (orig != null) btn.setBackground(orig);
            }
        });
        return btn;
    }

    // ── Toolbar Button ──────────────────────────────────────────────
    public static JButton toolbarButton(String text, Icon icon) {
        JButton btn = new JButton(text, icon);
        btn.setFont(Theme.FONT_SMALL);
        btn.setBackground(Theme.SURFACE_ALT);
        btn.setForeground(Theme.TEXT_PRIMARY);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(
                Math.max(btn.getPreferredSize().width + 16, 80),
                Theme.BUTTON_HEIGHT
        ));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            Color orig;
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                orig = btn.getBackground();
                btn.setBackground(Theme.TAB_HOVER);
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (orig != null) btn.setBackground(orig);
            }
        });
        return btn;
    }

    // ── Destructive Button ──────────────────────────────────────────
    public static JButton destructiveButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(Theme.FONT_MEDIUM);
        btn.setBackground(Theme.SURFACE_ALT);
        btn.setForeground(Theme.DESTRUCTIVE);
        btn.setFocusPainted(false);
        btn.setBorder(new LineBorder(new Color(0xF0, 0x6C, 0x6C, 80), 1, true));
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(90, Theme.BUTTON_HEIGHT));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            Color origBg, origFg;
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                origBg = btn.getBackground();
                origFg = btn.getForeground();
                btn.setBackground(Theme.DESTRUCTIVE);
                btn.setForeground(Theme.ON_PRIMARY);
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (origBg != null) {
                    btn.setBackground(origBg);
                    btn.setForeground(origFg);
                }
            }
        });
        return btn;
    }

    // ── Text Field ──────────────────────────────────────────────────
    public static JTextField styledField(String placeholder) {
        JTextField field = new JTextField();
        field.setFont(Theme.FONT_REGULAR);
        field.setPreferredSize(new Dimension(250, Theme.INPUT_HEIGHT));
        field.setBorder(roundedBorder(Theme.BORDER, Theme.RADIUS_SM, Theme.SPACING_MD, Theme.SPACING_XS));
        field.putClientProperty("JTextField.placeholderText", placeholder);
        return field;
    }

    // ── Labels ──────────────────────────────────────────────────────
    public static JLabel label(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(Theme.FONT_MEDIUM);
        lbl.setForeground(Theme.TEXT_PRIMARY);
        return lbl;
    }

    public static JLabel sectionHeader(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(Theme.FONT_LARGE);
        lbl.setForeground(Theme.PRIMARY);
        lbl.setBorder(new EmptyBorder(0, 0, Theme.SPACING_SM, 0));
        return lbl;
    }

    public static JLabel helperText(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(Theme.FONT_SMALL);
        lbl.setForeground(Theme.TEXT_MUTED);
        return lbl;
    }

    // ── Progress Bar ────────────────────────────────────────────────
    public static JProgressBar styledProgressBar() {
        JProgressBar bar = new JProgressBar();
        bar.setStringPainted(true);
        bar.setFont(Theme.FONT_SMALL);
        bar.setPreferredSize(new Dimension(0, 20));
        bar.setForeground(Theme.ACCENT);
        bar.setBackground(Theme.SURFACE_ALT);
        bar.setBorderPainted(false);
        bar.putClientProperty("JProgressBar.style", "round");
        return bar;
    }

    // ── Tabbed Pane ─────────────────────────────────────────────────
    public static JTabbedPane styledTabs() {
        JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);
        tabs.setFont(Theme.FONT_BOLD);
        return tabs;
    }

    // ── Scroll Pane ─────────────────────────────────────────────────
    public static JScrollPane styledScrollPane(JComponent view) {
        JScrollPane sp = new JScrollPane(view);
        sp.setBorder(roundedBorder(Theme.BORDER, Theme.RADIUS_MD));
        sp.getViewport().setBackground(Theme.LIST_BG);
        return sp;
    }
}
