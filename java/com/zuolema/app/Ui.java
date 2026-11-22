package com.zuolema.app;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/** 统一的颜色、圆角、字体和布局小工具。 */
public final class Ui {

    public static int BG = 0xFFFAF5F1;
    public static int CARD = 0xFFFFFFFF;
    public static int ROSE = 0xFFB9556C;
    public static int DEEP = 0xFF7E3345;
    public static int INK = 0xFF32262B;
    public static int MUTED = 0xFF9A8A8F;
    public static int LINE = 0xFFEFE3DE;
    public static int SOFT = 0xFFF7EAE7;
    public static int SOFT2 = 0xFFFBF4F1;
    public static int GOLD = 0xFFC08A55;
    public static int DANGER = 0xFFB3453F;
    private static boolean dark = false;
    private static boolean black = false;
    private static int accent = 0;

    private Ui() {
    }

    public static boolean isDark() {
        return dark;
    }

    public static boolean isBlack() {
        return black;
    }

    public static int accent() {
        return accent;
    }

    /** 同时应用明暗、性别主题和自定义主题色；accent 为 0 时跟随性别。 */
    public static void apply(boolean on, int accentColor) {
        apply(on, null, accentColor);
    }

    public static void apply(boolean on, String gender, int accentColor) {
        apply(on, false, gender, accentColor);
    }

    /** 纯黑模式只保留纯黑表面，不叠任何亮度层。 */
    public static void apply(boolean on, boolean pureBlack, String gender, int accentColor) {
        dark = on;
        black = on && pureBlack;
        accent = accentColor;
        applyPalette(gender);
    }

    public static void setDark(boolean on) {
        dark = on;
        applyPalette(null);
    }

    public static void setAccent(int accentColor) {
        accent = accentColor;
        applyPalette(null);
    }

    private static void applyPalette(String gender) {
        if (dark) {
            BG = 0xFF141013;
            CARD = 0xFF21191D;
            ROSE = 0xFFD77C90;
            DEEP = 0xFFF0B9C5;
            INK = 0xFFF3E8EB;
            MUTED = 0xFFA38F96;
            LINE = 0xFF3B2B31;
            SOFT = 0xFF2B2025;
            SOFT2 = 0xFF251C20;
            GOLD = 0xFFD5A36E;
            DANGER = 0xFFE3827A;
        } else {
            BG = 0xFFFAF5F1;
            CARD = 0xFFFFFFFF;
            ROSE = 0xFFB9556C;
            DEEP = 0xFF7E3345;
            INK = 0xFF32262B;
            MUTED = 0xFF9A8A8F;
            LINE = 0xFFEFE3DE;
            SOFT = 0xFFF7EAE7;
            SOFT2 = 0xFFFBF4F1;
            GOLD = 0xFFC08A55;
            DANGER = 0xFFB3453F;
        }
        if (accent != 0) {
            applyAccentPalette();
        } else if ("male".equals(gender)) {
            applyMalePalette();
        } else if ("secret".equals(gender)) {
            applySecretPalette();
        }
        if (black) applyBlackSurfaces();
    }

    /** OLED 纯黑：背景纯黑，卡片保留但整体压暗，仍能和底色区分。 */
    private static void applyBlackSurfaces() {
        BG = 0xFF000000;
        CARD = 0xFF0F0D0E;
        SOFT = 0xFF141113;
        SOFT2 = 0xFF0A090A;
        LINE = 0xFF2C2528;
    }

    private static void applyMalePalette() {
        if (dark) {
            BG = 0xFF0F1419;
            CARD = 0xFF182028;
            ROSE = 0xFF7FAAD0;
            DEEP = 0xFFC7DFF2;
            INK = 0xFFE8F0F6;
            MUTED = 0xFF93A3B0;
            LINE = 0xFF2A3844;
            SOFT = 0xFF1C2833;
            SOFT2 = 0xFF17212A;
            GOLD = 0xFFD5A36E;
            DANGER = 0xFFE3827A;
        } else {
            BG = 0xFFF3F7FA;
            CARD = 0xFFFFFFFF;
            ROSE = 0xFF3F6F9E;
            DEEP = 0xFF244A6B;
            INK = 0xFF1F2C36;
            MUTED = 0xFF7D8C99;
            LINE = 0xFFDCE6EE;
            SOFT = 0xFFE7F0F7;
            SOFT2 = 0xFFF2F7FB;
            GOLD = 0xFFB98A4E;
            DANGER = 0xFFB3453F;
        }
    }

    private static void applySecretPalette() {
        if (dark) {
            BG = 0xFF141112;
            CARD = 0xFF211B1D;
            ROSE = 0xFFC69AA3;
            DEEP = 0xFFE7C6CC;
            INK = 0xFFF3E8EB;
            MUTED = 0xFFA6969A;
            LINE = 0xFF3B2E31;
            SOFT = 0xFF2B2225;
            SOFT2 = 0xFF251D20;
            GOLD = 0xFFD5A36E;
            DANGER = 0xFFE3827A;
        } else {
            BG = 0xFFF7F4F2;
            CARD = 0xFFFFFFFF;
            ROSE = 0xFF8A6E73;
            DEEP = 0xFF5D444A;
            INK = 0xFF30282A;
            MUTED = 0xFF968A8D;
            LINE = 0xFFE8DEDA;
            SOFT = 0xFFF2EBE8;
            SOFT2 = 0xFFFAF6F4;
            GOLD = 0xFFB98A4E;
            DANGER = 0xFFB3453F;
        }
    }

    private static void applyAccentPalette() {
        int a = accent & 0x00FFFFFF;
        double lum = luminance(a);
        if (dark) {
            if (lum < 0.22) a = mix(a, 0xFFFFFF, 0.42f);
        } else {
            if (lum > 0.62) a = mix(a, 0x000000, 0.42f);
        }
        ROSE = a | 0xFF000000;
        DEEP = (dark ? mix(ROSE, 0xFFFFFF, 0.46f) : mix(ROSE, 0x000000, 0.34f))
                | 0xFF000000;
        SOFT = mix(BG, ROSE, dark ? 0.20f : 0.10f) | 0xFF000000;
        SOFT2 = mix(BG, ROSE, dark ? 0.11f : 0.05f) | 0xFF000000;
        LINE = mix(BG, ROSE, dark ? 0.34f : 0.20f) | 0xFF000000;
    }

    private static int mix(int base, int over, float t) {
        int br = (base >> 16) & 0xFF, bgc = (base >> 8) & 0xFF, bb = base & 0xFF;
        int or = (over >> 16) & 0xFF, og = (over >> 8) & 0xFF, ob = over & 0xFF;
        int r = Math.round(br + (or - br) * t);
        int g = Math.round(bgc + (og - bgc) * t);
        int b = Math.round(bb + (ob - bb) * t);
        return (r << 16) | (g << 8) | b;
    }

    private static double luminance(int c) {
        double r = ((c >> 16) & 0xFF) / 255.0;
        double g = ((c >> 8) & 0xFF) / 255.0;
        double b = (c & 0xFF) / 255.0;
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    public static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                c.getResources().getDisplayMetrics()));
    }

    public static GradientDrawable bg(int color, float radiusDp, Context c) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(c, radiusDp));
        return d;
    }

    public static GradientDrawable strokeBg(int color, float radiusDp, int strokeColor,
                                            float strokeDp, Context c) {
        GradientDrawable d = bg(color, radiusDp, c);
        d.setStroke(dp(c, strokeDp), strokeColor);
        return d;
    }

    public static GradientDrawable gradient(int top, int bottom, float radiusDp, Context c) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{top, bottom});
        d.setCornerRadius(dp(c, radiusDp));
        return d;
    }

    public static Drawable ripple(int color, float radiusDp, int rippleColor, Context c) {
        return new RippleDrawable(ColorStateList.valueOf(rippleColor), bg(color, radiusDp, c), null);
    }

    public static TextView tv(Context c, CharSequence text, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(text);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setIncludeFontPadding(false);
        t.setLineSpacing(dp(c, 3), 1f);
        if (bold) t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        return t;
    }

    public static TextView serif(Context c, CharSequence text, float sp, int color, boolean bold) {
        TextView t = tv(c, text, sp, color, bold);
        t.setTypeface(Typeface.create("serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        return t;
    }

    public static TextView label(Context c, CharSequence text) {
        TextView t = tv(c, text, 11, ROSE, true);
        t.setLetterSpacing(0.12f);
        return t;
    }

    public static LinearLayout col(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    public static LinearLayout row(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    public static LinearLayout card(Context c) {
        LinearLayout l = col(c);
        l.setBackground(bg(CARD, 22, c));
        int p = dp(c, 17);
        l.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(c, 12);
        l.setLayoutParams(lp);
        l.setElevation(dp(c, 1));
        return l;
    }

    public static void margin(View v, int l, int t, int r, int b) {
        ViewGroup.LayoutParams raw = v.getLayoutParams();
        if (!(raw instanceof ViewGroup.MarginLayoutParams)) return;
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) raw;
        lp.setMargins(l, t, r, b);
        v.setLayoutParams(lp);
    }

    public static LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w, h);
    }

    public static LinearLayout.LayoutParams lp(int w, int h, float weight) {
        return new LinearLayout.LayoutParams(w, h, weight);
    }

    public static LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    public static View space(Context c, int h) {
        View v = new View(c);
        v.setLayoutParams(lp(1, dp(c, h)));
        return v;
    }

    public static View divider(Context c) {
        View v = new View(c);
        v.setBackgroundColor(LINE);
        v.setLayoutParams(lp(1, dp(c, 1)));
        return v;
    }

    public static View vdivider(Context c, int heightDp) {
        View v = new View(c);
        v.setBackgroundColor(LINE);
        v.setLayoutParams(lp(dp(c, 1), dp(c, heightDp)));
        return v;
    }

    public static TextView pill(Context c, String text, boolean selected) {
        TextView t = tv(c, text, 12, selected ? Color.WHITE : INK, selected);
        t.setGravity(Gravity.CENTER);
        int px = dp(c, 13);
        int py = dp(c, 7);
        t.setPadding(px, py, px, py);
        t.setBackground(selected ? bg(ROSE, 30, c) : strokeBg(SOFT2, 30, LINE, 1, c));
        t.setClickable(true);
        return t;
    }

    public static int alpha(int color, int a) {
        return (color & 0x00FFFFFF) | ((a & 0xFF) << 24);
    }
}
