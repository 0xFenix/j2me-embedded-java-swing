package mzmod.ui;

import java.awt.*;

/**
 * Design tokens for MZMOD Advance Menu - Dark Mode.
 */
public final class Theme {

    private Theme() {}

    // ── Colors (Dark Mode) ──────────────────────────────────────────
    public static final Color PRIMARY        = new Color(0x5B, 0xA0, 0xD0);
    public static final Color PRIMARY_LIGHT  = new Color(0x7C, 0xBB, 0xE4);
    public static final Color ON_PRIMARY     = new Color(0x1A, 0x1A, 0x2E);

    public static final Color SECONDARY      = new Color(0x6C, 0xB4, 0xEE);
    public static final Color ACCENT         = new Color(0x4E, 0xC9, 0xA1);
    public static final Color ACCENT_HOVER   = new Color(0x3D, 0xB5, 0x8E);

    public static final Color BACKGROUND     = new Color(0x1E, 0x1E, 0x2E);
    public static final Color SURFACE        = new Color(0x2A, 0x2A, 0x3C);
    public static final Color SURFACE_ALT    = new Color(0x33, 0x33, 0x47);
    public static final Color BORDER         = new Color(0x44, 0x44, 0x5A);
    public static final Color BORDER_FOCUS   = new Color(0x6C, 0xB4, 0xEE);

    public static final Color TEXT_PRIMARY   = new Color(0xE0, 0xE0, 0xF0);
    public static final Color TEXT_SECONDARY = new Color(0x98, 0x98, 0xB0);
    public static final Color TEXT_MUTED     = new Color(0x6C, 0x6C, 0x84);

    public static final Color SUCCESS        = new Color(0x4E, 0xC9, 0xA1);
    public static final Color ERROR          = new Color(0xF0, 0x6C, 0x6C);
    public static final Color WARNING        = new Color(0xF0, 0xC0, 0x6C);
    public static final Color INFO           = new Color(0x6C, 0xB4, 0xEE);

    public static final Color DESTRUCTIVE    = new Color(0xF0, 0x6C, 0x6C);
    public static final Color DESTRUCTIVE_HOVER = new Color(0xD0, 0x50, 0x50);

    // ── Tab colors ──────────────────────────────────────────────────
    public static final Color TAB_ACTIVE     = new Color(0x6C, 0xB4, 0xEE);
    public static final Color TAB_HOVER      = new Color(0x38, 0x38, 0x50);
    public static final Color TAB_BG         = new Color(0x25, 0x25, 0x37);

    // ── List colors ─────────────────────────────────────────────────
    public static final Color LIST_BG        = new Color(0x24, 0x24, 0x36);
    public static final Color LIST_SEL_BG    = new Color(0x3A, 0x3A, 0x54);
    public static final Color LIST_SEL_FG    = new Color(0x6C, 0xB4, 0xEE);

    // ── Log colors ──────────────────────────────────────────────────
    public static final Color LOG_BG         = new Color(0x1A, 0x1A, 0x2A);
    public static final Color LOG_FG         = new Color(0xC0, 0xC0, 0xD8);

    // ── Fonts ───────────────────────────────────────────────────────
    private static final String FONT_FAMILY = getSystemFont();

    public static final Font FONT_REGULAR    = new Font(FONT_FAMILY, Font.PLAIN, 13);
    public static final Font FONT_MEDIUM     = new Font(FONT_FAMILY, Font.PLAIN, 13);
    public static final Font FONT_BOLD       = new Font(FONT_FAMILY, Font.BOLD, 13);
    public static final Font FONT_SMALL      = new Font(FONT_FAMILY, Font.PLAIN, 11);
    public static final Font FONT_LARGE      = new Font(FONT_FAMILY, Font.BOLD, 15);
    public static final Font FONT_TITLE      = new Font(FONT_FAMILY, Font.BOLD, 18);

    public static final Font FONT_MONO       = new Font(getMonoFont(), Font.PLAIN, 12);
    public static final Font FONT_MONO_SMALL = new Font(getMonoFont(), Font.PLAIN, 11);

    // ── Spacing (8px grid) ──────────────────────────────────────────
    public static final int SPACING_XS  = 4;
    public static final int SPACING_SM  = 8;
    public static final int SPACING_MD  = 12;
    public static final int SPACING_LG  = 16;
    public static final int SPACING_XL  = 24;
    public static final int SPACING_XXL = 32;

    // ── Border radius ───────────────────────────────────────────────
    public static final int RADIUS_SM = 8;
    public static final int RADIUS_MD = 12;
    public static final int RADIUS_LG = 16;
    public static final int RADIUS_XL = 20;

    // ── Component sizes ─────────────────────────────────────────────
    public static final int BUTTON_HEIGHT  = 34;
    public static final int INPUT_HEIGHT   = 32;
    public static final int TAB_HEIGHT     = 38;
    public static final int ICON_SIZE      = 16;
    public static final int TOOLBAR_HEIGHT = 42;

    // ── Padding ─────────────────────────────────────────────────────
    public static final Insets PAD_SM = new Insets(6, 10, 6, 10);
    public static final Insets PAD_MD = new Insets(8, 14, 8, 14);
    public static final Insets PAD_LG = new Insets(12, 18, 12, 18);
    public static final Insets PAD_XL = new Insets(16, 28, 16, 28);

    // ── Helper ──────────────────────────────────────────────────────
    private static String getSystemFont() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("mac")) return "SF Pro Text";
        if (os.contains("linux")) return "Ubuntu";
        return "Segoe UI";
    }

    private static String getMonoFont() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("mac")) return "SF Mono";
        if (os.contains("linux")) return "Ubuntu Mono";
        return "Cascadia Code";
    }
}
