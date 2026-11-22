package com.zuolema.app;

import android.Manifest;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.hardware.fingerprint.FingerprintManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Space;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class MainActivity extends Activity {

    private static final int REQ_IMAGES = 101;
    private static final int REQ_VIDEO = 102;
    private static final int REQ_AUDIO_PERM = 201;
    private static final int REQ_EXPORT = 301;
    private static final int REQ_IMPORT = 302;

    private static final String[] MOOD_IDS = {"happy", "relaxed", "calm", "low", "tired"};
    private static final String[] MOOD_EMOJI = {"😊", "😌", "🙂", "😔", "😣"};
    private static final String[] MOOD_LABEL = {"愉悦", "放松", "平静", "低落", "烦躁"};
    private static final String[] STATES = {"精力充沛", "放松", "有点疲惫", "敏感", "心不在焉", "很满足"};
    private static final String[] EXTRA_IDS = {"toys", "porn", "lube", "lingerie", "other"};
    private static final String[] EXTRA_LABELS = {"道具", "成人视频", "润滑液", "情趣内衣", "其他"};
    private static final String[] SEX_METHOD_IDS = {"none", "condom", "outside", "custom"};
    private static final String[] SEX_METHOD_LABELS = {"无套", "有套", "体外", "自定义"};
    private static final String[] SEX_METHOD_HINTS = {"不戴套", "全程佩戴", "体外结束", "写自己的方式"};
    private static final String[] MAST_METHOD_IDS = {"hand", "toy", "custom"};
    private static final String[] MAST_METHOD_LABELS = {"手", "道具", "自定义"};
    private static final String[] MAST_METHOD_HINTS = {"用手", "使用道具", "写自己的方式"};
    private static final String[] PLEASURE_LABEL = {"", "几乎没有", "轻微", "愉悦", "强烈", "极致"};
    private static final int[] DURATION_PRESETS = {5, 15, 30, 45, 60};

    private Store store;
    private FrameLayout content;
    private LinearLayout bottomBar;
    private LinearLayout tabDiary, tabTimeline, tabSettings;
    private TabIconView tabDiaryIcon, tabTimelineIcon, tabSettingsIcon;
    private TextView tabDiaryLabel, tabTimelineLabel, tabSettingsLabel;

    private String tab = "diary";
    private boolean diaryCalendar = true;
    private Calendar calendarCursor;
    private String date = Store.today();
    private String editingId = null;
    private String activityType = "";
    private String method = "";
    private String methodCustom = "";
    private int durationMinutes = 15;
    private String mood = "";
    private String bodyState = "";
    private int pleasure = 0;
    private String note = "";
    private final List<String> extras = new ArrayList<>();
    private final ArrayList<Store.Media> media = new ArrayList<>();
    private boolean dirty;
    private boolean bindingNote;
    private boolean exists;

    private EditText noteView;
    private TextView saveBtn;
    private TextView durationValue;
    private SeekBar durationBar;
    private int durationTapCount;
    private long durationTapAt;
    /** 默认 2 小时；连点彩蛋确认后仅当前这次编辑放宽到 5 小时。 */
    private int durationMax = 120;
    private final List<TextView> durationChips = new ArrayList<>();
    private LinearLayout sexTile, mastTile;
    private TextView sexGlyph, sexTitle, sexSub, mastGlyph, mastTitle, mastSub;
    private LinearLayout methodRow;
    private TextView methodText;
    private final List<LinearLayout> moodItems = new ArrayList<>();
    private final List<TextView> moodLabels = new ArrayList<>();
    private final List<TextView> stateChips = new ArrayList<>();
    private final List<TextView> extraChips = new ArrayList<>();
    private final List<TextView> hearts = new ArrayList<>();
    private TextView pleasureHint;
    private LinearLayout mediaRow;
    private HorizontalScrollView mediaScroll;

    private String selectedGender;
    private final List<LinearLayout> genderCards = new ArrayList<>();
    private TextView onboardBtn;

    private MediaPlayer audioPlayer;
    private String playingAudioPath;

    private MediaRecorder recorder;
    private File recordFile;
    private boolean recording;
    private long recordStartedAt;
    private Dialog recordDialog;
    private TextView recordTimer;
    private TextView recordAction;
    private TextView recordHint;
    private final Handler recordHandler = new Handler(Looper.getMainLooper());
    private Runnable recordTick;

    private String timelineFilter = "all";


    private boolean locked;
    private boolean lockVisible;
    private boolean suppressLock;
    private boolean pinChecking;
    private final StringBuilder pinBuffer = new StringBuilder();
    private LinearLayout pinDots;
    private TextView pinError;
    private FingerprintManager fingerprintManager;
    private CancellationSignal fingerprintSignal;
    private boolean fingerprintRunning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new Store(this);
        store.purgeCloudConfig();
        purgeCloudCache();
        Ui.apply(resolveDark(store.theme()), isPureBlack(store.theme()), store.gender(), resolveAccent());
        applyWindowFlags();
        date = Store.today();
        calendarCursor = Calendar.getInstance();
        calendarCursor.set(Calendar.DAY_OF_MONTH, 1);
        installShell();
        applySystemBars();

        if (store.hasPin() && !store.withinGrace()) {
            showLock();
        } else if (store.gender() == null) {
            showOnboarding();
        } else {
            selectedGender = store.gender();
            showMain();
        }
    }

    private void purgeCloudCache() {
        String[] names = {"lanzou-backup.zip", "webdav-backup.zip", "webdav-restore.zip"};
        for (String name : names) {
            File f = new File(getCacheDir(), name);
            if (f.isFile()) f.delete();
        }
    }

    private int bottomInset() {
        return dp(64);
    }

    private void installShell() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Ui.BG);
        content = new FrameLayout(this);
        content.setClipToPadding(false);
        root.addView(content, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        bottomBar = buildBottomBar();
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.BOTTOM;
        root.addView(bottomBar, lp);
        setContentView(root);
    }

    private int resolveAccent() {
        String mode = store.accentMode();
        if ("custom".equals(mode)) return store.accentColor();
        if ("dynamic".equals(mode)) return systemAccent();
        return 0;
    }

    private int systemAccent() {
        if (Build.VERSION.SDK_INT < 31) return 0;
        try {
            int id = getResources().getIdentifier("system_accent1_500", "color", "android");
            if (id == 0) return 0;
            return getResources().getColor(id, getTheme());
        } catch (Exception e) {
            return 0;
        }
    }

    private boolean isPureBlack(String mode) {
        return "dark".equals(mode) && store.pureBlack();
    }

    private boolean resolveDark(String mode) {
        if ("dark".equals(mode) || "black".equals(mode)) return true;
        if ("light".equals(mode)) return false;
        int night = getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return night == Configuration.UI_MODE_NIGHT_YES;
    }

    private void applySystemBars() {
        Window w = getWindow();
        w.setStatusBarColor(Ui.BG);
        w.setNavigationBarColor(Ui.BG);
        int flags = 0;
        if (!Ui.isDark() && Build.VERSION.SDK_INT >= 23) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        }
        if (!Ui.isDark() && Build.VERSION.SDK_INT >= 26) {
            flags |= 0x00000010;
        }
        w.getDecorView().setSystemUiVisibility(flags);
    }

    /* ==================== 进入密码 ==================== */

    private void showLock() {
        locked = true;
        lockVisible = true;
        pinChecking = false;
        pinBuffer.setLength(0);
        bottomBar.setVisibility(View.GONE);
        content.setPadding(0, 0, 0, 0);
        content.removeAllViews();

        LinearLayout page = Ui.col(this);
        page.setBackgroundColor(Ui.BG);
        page.setPadding(0, 0, 0, dp(24));

        // 上半部分可滚动，密码键盘固定在底部，不再挤在屏幕中间。
        ScrollView topScroll = new ScrollView(this);
        topScroll.setFillViewport(true);
        topScroll.setBackgroundColor(Ui.BG);
        LinearLayout top = Ui.col(this);
        top.setGravity(Gravity.CENTER);
        top.setPadding(dp(26), dp(20), dp(26), dp(8));
        topScroll.addView(top, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        page.addView(topScroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        TextView logo = Ui.serif(this, "做", 30, Ui.ROSE, true);
        logo.setGravity(Gravity.CENTER);
        logo.setBackground(Ui.bg(Ui.alpha(Ui.ROSE, 0x22), 36, this));
        top.addView(logo, new LinearLayout.LayoutParams(dp(72), dp(72)));
        top.addView(Ui.space(this, 18));

        TextView title = Ui.serif(this, "欢迎回来", 28, Ui.INK, true);
        title.setGravity(Gravity.CENTER);
        top.addView(title);
        top.addView(Ui.space(this, 6));
        TextView sub = Ui.tv(this, "输入 4 位密码，继续写今天。", 12, Ui.MUTED, false);
        sub.setGravity(Gravity.CENTER);
        top.addView(sub);
        top.addView(Ui.space(this, 18));

        if (store.fingerprintEnabled() && canUseFingerprint()) {
            TextView fp = Ui.tv(this, "☝  用指纹解锁", 13, Ui.DEEP, true);
            fp.setGravity(Gravity.CENTER);
            fp.setBackground(Ui.ripple(Ui.alpha(Ui.ROSE, 0x18), 22,
                    Ui.alpha(Ui.ROSE, 0x33), this));
            fp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startFingerprint();
                }
            });
            top.addView(fp, new LinearLayout.LayoutParams(dp(190), dp(44)));
            top.addView(Ui.space(this, 18));
        }

        pinDots = Ui.row(this);
        pinDots.setGravity(Gravity.CENTER);
        top.addView(pinDots);
        top.addView(Ui.space(this, 10));

        pinError = Ui.tv(this, "密码不对，再试一次", 11, Ui.DANGER, false);
        pinError.setGravity(Gravity.CENTER);
        pinError.setVisibility(View.INVISIBLE);
        top.addView(pinError);

        LinearLayout keypad = Ui.col(this);
        keypad.setGravity(Gravity.CENTER_HORIZONTAL);
        String[][] keys = {
                {"1", "2", "3"},
                {"4", "5", "6"},
                {"7", "8", "9"},
                {"", "0", "⌫"}
        };
        for (String[] line : keys) {
            LinearLayout row = Ui.row(this);
            row.setGravity(Gravity.CENTER);
            for (String label : line) {
                if (label.length() == 0) {
                    row.addView(new Space(this), new LinearLayout.LayoutParams(dp(68), dp(58)));
                } else {
                    TextView key = Ui.tv(this, label, "⌫".equals(label) ? 18 : 22, Ui.INK, true);
                    key.setGravity(Gravity.CENTER);
                    key.setBackground(Ui.ripple(Ui.SOFT2, 30,
                            Ui.alpha(Ui.ROSE, 0x33), this));
                    final String v = label;
                    key.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            onPinKey(v);
                        }
                    });
                    LinearLayout.LayoutParams klp = new LinearLayout.LayoutParams(dp(68), dp(58));
                    klp.setMargins(dp(6), dp(5), dp(6), dp(5));
                    row.addView(key, klp);
                }
            }
            keypad.addView(row);
        }
        page.addView(keypad);
        page.addView(Ui.space(this, 14));
        TextView foot = Ui.tv(this, "密码只保存在本机，不会上传", 10, Ui.MUTED, false);
        foot.setGravity(Gravity.CENTER);
        page.addView(foot);

        content.addView(page, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        refreshPinDots();
        if (store.fingerprintEnabled() && canUseFingerprint()) {
            content.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (lockVisible) startFingerprint();
                }
            }, 420);
        }
    }

    private void onPinKey(String key) {
        if (pinChecking) return;
        if ("⌫".equals(key)) {
            if (pinBuffer.length() > 0) {
                pinBuffer.deleteCharAt(pinBuffer.length() - 1);
            }
            refreshPinDots();
            return;
        }
        if (pinBuffer.length() >= 4) return;
        pinBuffer.append(key);
        if (pinError != null) pinError.setVisibility(View.INVISIBLE);
        refreshPinDots();
        if (pinBuffer.length() == 4) {
            pinChecking = true;
            final String pin = pinBuffer.toString();
            content.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (store.checkPin(pin)) {
                        unlock();
                    } else {
                        pinChecking = false;
                        pinBuffer.setLength(0);
                        refreshPinDots();
                        if (pinError != null) pinError.setVisibility(View.VISIBLE);
                    }
                }
            }, 110);
        }
    }

    private void refreshPinDots() {
        if (pinDots == null) return;
        pinDots.removeAllViews();
        for (int i = 0; i < 4; i++) {
            View dot = new View(this);
            boolean filled = i < pinBuffer.length();
            dot.setBackground(filled
                    ? Ui.bg(Ui.ROSE, 20, this)
                    : Ui.strokeBg(Ui.SOFT, 20, Ui.LINE, 1, this));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(14), dp(14));
            lp.setMargins(dp(7), 0, dp(7), 0);
            pinDots.addView(dot, lp);
        }
    }

    private void unlock() {
        cancelFingerprint();
        pinChecking = false;
        locked = false;
        lockVisible = false;
        pinBuffer.setLength(0);
        store.markUnlocked();
        if (store.gender() == null) {
            showOnboarding();
        } else {
            selectedGender = store.gender();
            showMain();
        }
    }

    /* ==================== 首次开启：选择性别 ==================== */

    private void showOnboarding() {
        locked = false;
        lockVisible = false;
        bottomBar.setVisibility(View.GONE);
        content.setPadding(0, 0, 0, 0);
        content.removeAllViews();
        genderCards.clear();
        selectedGender = null;

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Ui.BG);
        LinearLayout page = Ui.col(this);
        page.setPadding(dp(26), dp(30), dp(26), dp(30));
        scroll.addView(page, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout pill = Ui.row(this);
        pill.setGravity(Gravity.CENTER);
        pill.setBackground(Ui.bg(Ui.alpha(Ui.ROSE, 0x24), 30, this));
        TextView pillText = Ui.tv(this, "PRIVATE · 只在本机", 10, Ui.ROSE, true);
        pillText.setLetterSpacing(0.12f);
        pill.addView(pillText);
        LinearLayout.LayoutParams plp = Ui.lp(ViewGroup.LayoutParams.WRAP_CONTENT, dp(30));
        page.addView(pill, plp);

        page.addView(Ui.space(this, 34));
        TextView title = Ui.serif(this, "做了吗", 44, Ui.INK, true);
        page.addView(title);
        page.addView(Ui.space(this, 10));
        TextView sub = Ui.tv(this, "把身体的感觉，留给自己。", 17, Ui.DEEP, false);
        page.addView(sub);
        page.addView(Ui.space(this, 12));
        TextView desc = Ui.tv(this,
                "记录每一次亲密、心情和身体状态。\n没有账号，所有内容只保存在这台手机。",
                13, Ui.MUTED, false);
        page.addView(desc);

        page.addView(Ui.space(this, 32));
        TextView pick = Ui.label(this, "先告诉我");
        page.addView(pick);
        page.addView(Ui.space(this, 10));
        TextView pickTitle = Ui.tv(this, "你是", 20, Ui.INK, true);
        page.addView(pickTitle);
        page.addView(Ui.space(this, 14));

        addGenderCard(page, "female", "♀", "女生", "她的身体日记");
        addGenderCard(page, "male", "♂", "男生", "他的身体日记");
        addGenderCard(page, "secret", "✦", "保密", "只想安静记录");

        page.addView(Ui.space(this, 20));
        onboardBtn = Ui.tv(this, "开始记录", 16, Color.WHITE, true);
        onboardBtn.setGravity(Gravity.CENTER);
        onboardBtn.setBackground(Ui.bg(Ui.ROSE, 18, this));
        onboardBtn.setAlpha(0.38f);
        onboardBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedGender == null) {
                    toast("先选一个身份");
                    return;
                }
                store.setGender(selectedGender);
                Ui.apply(resolveDark(store.theme()), isPureBlack(store.theme()), selectedGender, resolveAccent());
                showMain();
            }
        });
        page.addView(onboardBtn, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));

        page.addView(Ui.space(this, 14));
        TextView privacy = Ui.tv(this, "之后可以在「设置」里随时切换", 11, Ui.MUTED, false);
        privacy.setGravity(Gravity.CENTER);
        page.addView(privacy);

        content.addView(scroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        AlphaAnimation fade = new AlphaAnimation(0f, 1f);
        fade.setDuration(280);
        scroll.startAnimation(fade);
    }

    private void addGenderCard(LinearLayout parent, final String id, String glyph,
                               String titleText, String subtitle) {
        final LinearLayout card = Ui.row(this);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackground(Ui.strokeBg(Ui.CARD, 20, Ui.LINE, 1, this));
        card.setClickable(true);

        TextView icon = Ui.tv(this, glyph, 20, Ui.ROSE, false);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(Ui.bg(Ui.SOFT, 30, this));
        card.addView(icon, new LinearLayout.LayoutParams(dp(44), dp(44)));

        LinearLayout col = Ui.col(this);
        col.setPadding(dp(14), 0, 0, 0);
        TextView t = Ui.tv(this, titleText, 16, Ui.INK, true);
        TextView s = Ui.tv(this, subtitle, 11, Ui.MUTED, false);
        col.addView(t);
        col.addView(Ui.space(this, 3));
        col.addView(s);
        card.addView(col, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        final TextView check = Ui.tv(this, "○", 20, Ui.MUTED, false);
        check.setGravity(Gravity.CENTER);
        card.addView(check, new LinearLayout.LayoutParams(dp(28), dp(28)));

        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedGender = id;
                refreshGenderCards();
            }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(10);
        parent.addView(card, lp);
        genderCards.add(card);
        card.setTag(new Object[]{id, check, icon, t, s});
    }

    private void refreshGenderCards() {
        for (LinearLayout card : genderCards) {
            Object[] tags = (Object[]) card.getTag();
            String id = (String) tags[0];
            TextView check = (TextView) tags[1];
            TextView icon = (TextView) tags[2];
            TextView title = (TextView) tags[3];
            TextView sub = (TextView) tags[4];
            boolean selected = id.equals(selectedGender);
            card.setBackground(selected
                    ? Ui.strokeBg(Ui.SOFT2, 20, Ui.ROSE, 1.5f, this)
                    : Ui.strokeBg(Ui.CARD, 20, Ui.LINE, 1, this));
            check.setText(selected ? "✓" : "○");
            check.setTextColor(selected ? Ui.ROSE : Ui.MUTED);
            icon.setBackground(Ui.bg(selected ? Ui.alpha(Ui.ROSE, 0x24) : Ui.SOFT, 30, this));
            icon.setTextColor(selected ? Ui.DEEP : Ui.ROSE);
            title.setTextColor(Ui.INK);
            sub.setTextColor(selected ? Ui.DEEP : Ui.MUTED);
        }
        if (onboardBtn != null) {
            boolean ready = selectedGender != null;
            onboardBtn.setAlpha(ready ? 1f : 0.38f);
            onboardBtn.setBackground(Ui.bg(ready ? Ui.ROSE : Ui.alpha(Ui.ROSE, 0x55), 18, this));
        }
    }

    /* ==================== 主界面骨架 ==================== */

    private void showMain() {
        Ui.apply(resolveDark(store.theme()), isPureBlack(store.theme()), store.gender(), resolveAccent());
        locked = false;
        lockVisible = false;
        bottomBar.setVisibility(View.VISIBLE);
        content.setPadding(0, 0, 0, bottomInset());
        if (selectedGender == null) selectedGender = store.gender();
        tab = "diary";
        diaryCalendar = true;
        editingId = null;
        date = Store.today();
        calendarCursor = Calendar.getInstance();
        calendarCursor.set(Calendar.DAY_OF_MONTH, 1);
        loadDraft();
        render();
        refreshTabs();
    }

    private void applyTheme(String mode) {
        store.setTheme(mode);
        Ui.apply(resolveDark(mode), isPureBlack(mode), store.gender(), resolveAccent());
        rebuildForTheme();
    }

    private void applyAccent(String mode, int color) {
        store.setAccentMode(mode);
        if (color != 0) store.setAccentColor(color);
        Ui.apply(resolveDark(store.theme()), isPureBlack(store.theme()), store.gender(), resolveAccent());
        rebuildForTheme();
    }

    private void rebuildForTheme() {
        if ("diary".equals(tab) && !diaryCalendar) commitDraft(false);
        applySystemBars();
        installShell();
        if (locked) {
            showLock();
            return;
        }
        if (store.gender() == null) {
            showOnboarding();
            return;
        }
        selectedGender = store.gender();
        bottomBar.setVisibility(View.VISIBLE);
        content.setPadding(0, 0, 0, bottomInset());
        render();
        refreshTabs();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if ("system".equals(store.theme())) {
            boolean dark = resolveDark("system");
            if (dark != Ui.isDark()) {
                Ui.apply(dark, store.gender(), resolveAccent());
                if (locked) {
                    applySystemBars();
                    showLock();
                } else {
                    rebuildForTheme();
                }
            }
        }
    }

    private LinearLayout buildBottomBar() {
        LinearLayout wrap = Ui.col(this);
        wrap.setBackgroundColor(Color.TRANSPARENT);
        wrap.setClipChildren(false);

        LinearLayout bar = Ui.row(this);
        bar.setPadding(dp(8), dp(7), dp(8), dp(7));
        bar.setBackground(Ui.bg(Ui.isBlack() ? Ui.BG : Ui.CARD, 0, this));
        bar.setElevation(0);
        wrap.addView(bar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        tabDiary = buildTab(0, "日记");
        tabTimeline = buildTab(1, "时间轴");
        tabSettings = buildTab(2, "设置");
        tabDiaryIcon = (TabIconView) tabDiary.getChildAt(0);
        tabDiaryLabel = (TextView) tabDiary.getChildAt(1);
        tabTimelineIcon = (TabIconView) tabTimeline.getChildAt(0);
        tabTimelineLabel = (TextView) tabTimeline.getChildAt(1);
        tabSettingsIcon = (TabIconView) tabSettings.getChildAt(0);
        tabSettingsLabel = (TextView) tabSettings.getChildAt(1);

        bar.addView(tabDiary, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        bar.addView(tabTimeline, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        bar.addView(tabSettings, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return wrap;
    }

    private LinearLayout buildTab(final int index, String label) {
        final LinearLayout item = Ui.col(this);
        item.setGravity(Gravity.CENTER);
        item.setPadding(0, dp(6), 0, dp(6));
        item.setClickable(true);
        TabIconView icon = new TabIconView(this, index);
        item.addView(icon, new LinearLayout.LayoutParams(dp(23), dp(23)));
        TextView text = Ui.tv(this, label, 10, Ui.MUTED, false);
        text.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tlp = Ui.lp(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.topMargin = dp(3);
        item.addView(text, tlp);
        item.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(index == 0 ? "diary" : index == 1 ? "timeline" : "settings");
            }
        });
        return item;
    }

    private void switchTab(String next) {
        if (next.equals(tab)) {
            if ("diary".equals(tab) && !diaryCalendar) {
                closeEditor();
            }
            return;
        }
        if ("diary".equals(tab)) commitDraft(false);
        tab = next;
        if ("diary".equals(tab)) {
            diaryCalendar = true;
            calendarCursor = Calendar.getInstance();
            calendarCursor.set(Calendar.DAY_OF_MONTH, 1);
            loadDraft();
        }
        render();
        refreshTabs();
    }

    private void refreshTabs() {
        styleTab(tabDiary, tabDiaryIcon, tabDiaryLabel, "diary".equals(tab));
        styleTab(tabTimeline, tabTimelineIcon, tabTimelineLabel, "timeline".equals(tab));
        styleTab(tabSettings, tabSettingsIcon, tabSettingsLabel, "settings".equals(tab));
    }

    private void styleTab(LinearLayout item, TabIconView icon, TextView label, boolean on) {
        item.setBackground(null);
        icon.setColor(on ? Ui.ROSE : Ui.MUTED);
        label.setTextColor(on ? Ui.DEEP : Ui.MUTED);
        label.setTypeface(android.graphics.Typeface.create(
                on ? "sans-serif-medium" : "sans-serif", android.graphics.Typeface.NORMAL));
    }

    private void render() {
        content.removeAllViews();
        View page;
        if ("timeline".equals(tab)) page = buildTimeline();
        else if ("settings".equals(tab)) page = buildSettings();
        else page = diaryCalendar ? buildCalendar() : buildDiaryEditor();
        content.addView(page, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        animateIn(page);
    }

    private void animateIn(View view) {
        if (view == null) return;
        AnimationSet set = new AnimationSet(true);
        set.setInterpolator(new DecelerateInterpolator());
        AlphaAnimation fade = new AlphaAnimation(0.2f, 1f);
        fade.setDuration(220);
        TranslateAnimation rise = new TranslateAnimation(0, 0, dp(12), 0);
        rise.setDuration(240);
        set.addAnimation(fade);
        set.addAnimation(rise);
        view.startAnimation(set);
    }

    /* ==================== 日记页 ==================== */

    private void loadDraft() {
        Store.Entry e = editingId == null ? null : store.byId(editingId);
        exists = e != null;
        if (e == null) {
            activityType = "";
            method = "";
            methodCustom = "";
            durationMinutes = 15;
            mood = "";
            bodyState = "";
            pleasure = 0;
            note = "";
            extras.clear();
            media.clear();
        } else {
            activityType = e.type == null ? "" : e.type;
            method = e.method == null ? "" : e.method;
            methodCustom = e.methodCustom == null ? "" : e.methodCustom;
            durationMinutes = e.durationMinutes;
            mood = e.mood == null ? "" : e.mood;
            bodyState = e.bodyState == null ? "" : e.bodyState;
            pleasure = e.pleasure;
            note = e.note == null ? "" : e.note;
            extras.clear();
            if (e.extras != null) extras.addAll(e.extras);
            media.clear();
            media.addAll(e.media);
        }
        dirty = false;
    }

    private View buildDiaryEditor() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Ui.BG);
        scroll.setClipToPadding(false);
        LinearLayout page = Ui.col(this);
        page.setPadding(dp(18), dp(18), dp(18), dp(26));
        scroll.addView(page, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        page.addView(buildEditorHeader());
        page.addView(Ui.space(this, 14));
        page.addView(buildActivityCard());
        page.addView(buildExtrasCard());
        page.addView(buildDurationCard());
        page.addView(buildMoodCard());
        page.addView(buildStateCard());
        page.addView(buildPleasureCard());
        page.addView(buildNoteCard());
        page.addView(buildMediaCard());
        page.addView(buildSaveArea());
        return scroll;
    }

    /* ==================== 日历首页 ==================== */

    private View buildCalendar() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Ui.BG);
        scroll.setClipToPadding(false);
        LinearLayout page = Ui.col(this);
        page.setPadding(dp(18), dp(18), dp(18), dp(26));
        scroll.addView(page, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        page.addView(buildCalendarHeader());
        page.addView(Ui.space(this, 16));
        page.addView(buildCalendarCard());
        page.addView(Ui.space(this, 14));
        page.addView(buildCalendarSummary());

        TextView hint = Ui.tv(this, "点一个日期，记录或修改那一天。", 11, Ui.MUTED, false);
        hint.setGravity(Gravity.CENTER);
        page.addView(hint);
        return scroll;
    }

    private View buildCalendarHeader() {
        LinearLayout row = Ui.row(this);
        LinearLayout col = Ui.col(this);
        col.addView(Ui.label(this, "DIARY CALENDAR"));
        col.addView(Ui.space(this, 7));
        col.addView(Ui.serif(this, "做了吗", 32, Ui.INK, true));
        col.addView(Ui.space(this, 5));
        col.addView(Ui.tv(this, "点一天，记录那一天。", 12, Ui.MUTED, false));
        row.addView(col, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        String g = selectedGender == null ? "secret" : selectedGender;
        TextView avatar = Ui.tv(this, genderGlyph(g), 20, Ui.ROSE, false);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(Ui.bg(Ui.alpha(Ui.ROSE, 0x24), 30, this));
        row.addView(avatar, new LinearLayout.LayoutParams(dp(46), dp(46)));
        return row;
    }

    private View buildCalendarCard() {
        LinearLayout card = Ui.card(this);
        card.setPadding(dp(12), dp(14), dp(12), dp(14));

        LinearLayout top = Ui.row(this);
        TextView prev = circleButton("‹");
        prev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shiftCalendarMonth(-1);
            }
        });
        top.addView(prev, new LinearLayout.LayoutParams(dp(34), dp(34)));

        LinearLayout mid = Ui.col(this);
        mid.setGravity(Gravity.CENTER);
        TextView month = Ui.serif(this, monthTitle(), 20, Ui.INK, true);
        month.setGravity(Gravity.CENTER);
        TextView monthSub = Ui.tv(this, "点一天开始记录", 10, Ui.MUTED, false);
        monthSub.setGravity(Gravity.CENTER);
        mid.addView(month);
        mid.addView(Ui.space(this, 2));
        mid.addView(monthSub);
        top.addView(mid, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView next = circleButton("›");
        if (isCurrentMonth()) {
            next.setAlpha(0.22f);
        } else {
            next.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    shiftCalendarMonth(1);
                }
            });
        }
        top.addView(next, new LinearLayout.LayoutParams(dp(34), dp(34)));
        card.addView(top);

        if (!isCurrentMonth()) {
            TextView backToday = Ui.tv(this, "回到今天", 10, Ui.ROSE, true);
            backToday.setPadding(dp(9), dp(4), dp(9), dp(4));
            backToday.setBackground(Ui.bg(0x14B9556C, 20, this));
            backToday.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    goCalendarToday();
                }
            });
            LinearLayout jump = Ui.row(this);
            jump.setGravity(Gravity.RIGHT);
            jump.addView(backToday);
            card.addView(jump);
            card.addView(Ui.space(this, 6));
        } else {
            card.addView(Ui.space(this, 12));
        }

        LinearLayout week = Ui.row(this);
        String[] names = {"日", "一", "二", "三", "四", "五", "六"};
        for (String name : names) {
            TextView w = Ui.tv(this, name, 10, Ui.MUTED, false);
            w.setGravity(Gravity.CENTER);
            week.addView(w, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
        card.addView(week);
        card.addView(Ui.space(this, 2));

        Calendar first = (Calendar) calendarCursor.clone();
        int offset = first.get(Calendar.DAY_OF_WEEK) - 1;
        int days = first.getActualMaximum(Calendar.DAY_OF_MONTH);
        int rows = (offset + days + 6) / 7;
        Calendar cursor = (Calendar) first.clone();
        cursor.add(Calendar.DAY_OF_MONTH, -offset);
        for (int r = 0; r < rows; r++) {
            LinearLayout weekRow = Ui.row(this);
            for (int c = 0; c < 7; c++) {
                boolean inMonth = cursor.get(Calendar.MONTH) == first.get(Calendar.MONTH)
                        && cursor.get(Calendar.YEAR) == first.get(Calendar.YEAR);
                View cell = inMonth ? buildCalendarDayCell((Calendar) cursor.clone())
                        : new View(this);
                weekRow.addView(cell, new LinearLayout.LayoutParams(0, dp(52), 1f));
                cursor.add(Calendar.DAY_OF_MONTH, 1);
            }
            card.addView(weekRow);
        }

        card.addView(Ui.space(this, 8));
        LinearLayout legend = Ui.row(this);
        legend.setGravity(Gravity.CENTER);
        addLegend(legend, Ui.ROSE, "做爱");
        addLegend(legend, Ui.GOLD, "自慰");
        addLegend(legend, Ui.MUTED, "其他记录");
        card.addView(legend);
        return card;
    }

    private View buildCalendarDayCell(final Calendar c) {
        final String key = formatDate(c);
        final boolean today = key.equals(Store.today());
        final boolean future = key.compareTo(Store.today()) > 0;
        List<Store.Entry> dayEntries = store.byDate(key);
        boolean has = !dayEntries.isEmpty();

        LinearLayout cell = Ui.col(this);
        cell.setGravity(Gravity.CENTER);
        cell.setPadding(0, dp(3), 0, dp(3));
        cell.setClickable(!future);
        if (!future) {
            cell.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openEditor(key);
                }
            });
        }
        if (future) cell.setAlpha(0.3f);

        TextView num = Ui.tv(this, String.valueOf(c.get(Calendar.DAY_OF_MONTH)), 14,
                today ? Color.WHITE : (has ? Ui.DEEP : Ui.INK), today || has);
        num.setGravity(Gravity.CENTER);
        if (today) {
            num.setBackground(Ui.bg(Ui.ROSE, 30, this));
        } else if (has) {
            num.setBackground(Ui.bg(Ui.alpha(Ui.ROSE, 0x1C), 30, this));
        }
        cell.addView(num, new LinearLayout.LayoutParams(dp(32), dp(32)));

        LinearLayout dots = Ui.row(this);
        dots.setGravity(Gravity.CENTER);
        dots.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(8)));
        if (has) {
            int shown = 0;
            for (Store.Entry e : dayEntries) {
                if (shown >= 3) break;
                int color = "sex".equals(e.type) ? Ui.ROSE
                        : ("mast".equals(e.type) ? Ui.GOLD : Ui.MUTED);
                dots.addView(dot(color));
                shown++;
            }
            if (dayEntries.size() > 3) {
                dots.addView(Ui.tv(this, "+" + (dayEntries.size() - 3), 8, Ui.MUTED, false));
            }
        }
        cell.addView(dots);
        return cell;
    }

    private View dot(int color) {
        View v = new View(this);
        v.setBackground(Ui.bg(color, 30, this));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(5), dp(5));
        lp.rightMargin = dp(2);
        v.setLayoutParams(lp);
        return v;
    }

    private void addLegend(LinearLayout row, int color, String text) {
        View dot = new View(this);
        dot.setBackground(Ui.bg(color, 30, this));
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(5), dp(5));
        dlp.rightMargin = dp(4);
        row.addView(dot, dlp);
        TextView t = Ui.tv(this, text, 9, Ui.MUTED, false);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.rightMargin = dp(12);
        row.addView(t, tlp);
    }

    private View buildCalendarSummary() {
        LinearLayout card = Ui.card(this);
        card.setPadding(dp(8), dp(14), dp(8), dp(14));
        LinearLayout row = Ui.row(this);

        String prefix = monthKey();
        java.util.HashSet<String> daysSet = new java.util.HashSet<>();
        int sex = 0, mast = 0;
        for (Store.Entry e : store.entries()) {
            if (e.date != null && e.date.startsWith(prefix)) {
                daysSet.add(e.date);
                if ("sex".equals(e.type)) sex++;
                if ("mast".equals(e.type)) mast++;
            }
        }
        int days = daysSet.size();
        row.addView(statCell(String.valueOf(days), "天记录"),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(Ui.vdivider(this, 30));
        row.addView(statCell(String.valueOf(sex), "做爱次数"),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(Ui.vdivider(this, 30));
        row.addView(statCell(String.valueOf(mast), "自慰次数"),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        card.addView(row);
        return card;
    }

    private String monthTitle() {
        return calendarCursor.get(Calendar.YEAR) + "年"
                + (calendarCursor.get(Calendar.MONTH) + 1) + "月";
    }

    private String monthKey() {
        return formatDate(calendarCursor).substring(0, 7);
    }

    private boolean isCurrentMonth() {
        Calendar now = Calendar.getInstance();
        return calendarCursor.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                && calendarCursor.get(Calendar.MONTH) == now.get(Calendar.MONTH);
    }

    private void shiftCalendarMonth(int delta) {
        Calendar c = (Calendar) calendarCursor.clone();
        c.add(Calendar.MONTH, delta);
        c.set(Calendar.DAY_OF_MONTH, 1);
        if (delta > 0) {
            Calendar now = Calendar.getInstance();
            if (c.get(Calendar.YEAR) > now.get(Calendar.YEAR)
                    || (c.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                    && c.get(Calendar.MONTH) > now.get(Calendar.MONTH))) {
                return;
            }
        }
        calendarCursor = c;
        render();
    }

    private void goCalendarToday() {
        calendarCursor = Calendar.getInstance();
        calendarCursor.set(Calendar.DAY_OF_MONTH, 1);
        render();
    }

    private void openEditor(String day) {
        if (store.byDate(day).isEmpty()) {
            openNewEntry(day);
        } else {
            showDayEntries(day);
        }
    }

    private void openNewEntry(String day) {
        tab = "diary";
        date = day;
        editingId = null;
        durationMax = 120;
        diaryCalendar = false;
        calendarCursor = parseDate(day);
        calendarCursor.set(Calendar.DAY_OF_MONTH, 1);
        loadDraft();
        render();
        refreshTabs();
    }

    private void openEntryEditor(Store.Entry e) {
        tab = "diary";
        date = e.date;
        editingId = e.id;
        durationMax = 120;
        diaryCalendar = false;
        calendarCursor = parseDate(e.date);
        calendarCursor.set(Calendar.DAY_OF_MONTH, 1);
        loadDraft();
        render();
        refreshTabs();
    }

    private void addAnotherEntry() {
        commitDraft(true);
        openNewEntry(date);
    }

    private void closeEditor() {
        if (!diaryCalendar) commitDraft(false);
        editingId = null;
        durationMax = 120;
        diaryCalendar = true;
        calendarCursor = parseDate(date);
        calendarCursor.set(Calendar.DAY_OF_MONTH, 1);
        render();
        refreshTabs();
    }

    private void showDayEntries(final String day) {
        final List<Store.Entry> list = store.byDate(day);
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout box = Ui.col(this);
        box.setBackground(Ui.bg(Ui.CARD, 26, this));
        box.setPadding(dp(20), dp(18), dp(20), dp(18));

        LinearLayout head = Ui.row(this);
        LinearLayout hc = Ui.col(this);
        hc.addView(Ui.label(this, "DAY LOG"));
        hc.addView(Ui.space(this, 5));
        hc.addView(Ui.serif(this, humanDate(day) + " " + weekday(day), 24, Ui.INK, true));
        hc.addView(Ui.space(this, 3));
        hc.addView(Ui.tv(this, list.size() + " 次记录 · 可以继续添加", 11, Ui.MUTED, false));
        head.addView(hc, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView close = Ui.tv(this, "×", 22, Ui.MUTED, false);
        close.setGravity(Gravity.CENTER);
        close.setBackground(Ui.bg(Ui.SOFT2, 30, this));
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        head.addView(close, new LinearLayout.LayoutParams(dp(34), dp(34)));
        box.addView(head);
        box.addView(Ui.space(this, 12));

        for (int index = 0; index < list.size(); index++) {
            final Store.Entry e = list.get(index);
            LinearLayout item = Ui.row(this);
            item.setGravity(Gravity.CENTER_VERTICAL);
            item.setPadding(dp(12), dp(11), dp(10), dp(11));
            item.setBackground(Ui.ripple(Ui.SOFT2, 14, Ui.alpha(Ui.ROSE, 0x22), this));
            LinearLayout info = Ui.col(this);
            LinearLayout tags = Ui.row(this);
            tags.addView(miniTag("第 " + (index + 1) + " 次", Ui.MUTED, 0x149A8A8F));
            tags.addView(miniTag(typeLabel(e.type), typeColor(e.type), typeBg(e.type)));
            if (e.durationMinutes > 0) tags.addView(miniTag(e.durationMinutes + " 分钟", Ui.DEEP, 0x0F7E3345));
            if (!e.media.isEmpty()) tags.addView(miniTag(e.media.size() + " 个媒体", Ui.MUTED, 0x149A8A8F));
            info.addView(tags);
            String moodLine = moodSummary(e);
            if (!moodLine.isEmpty()) {
                info.addView(Ui.space(this, 5));
                info.addView(Ui.tv(this, moodLine, 11, Ui.MUTED, false));
            }
            item.addView(info, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            item.addView(Ui.tv(this, "›", 20, Ui.MUTED, false));
            LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            ilp.bottomMargin = dp(7);
            box.addView(item, ilp);
            item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                    openEntryEditor(e);
                }
            });
        }

        box.addView(Ui.space(this, 6));
        TextView add = Ui.tv(this, "＋ 新增一次", 14, Color.WHITE, true);
        add.setGravity(Gravity.CENTER);
        add.setBackground(Ui.ripple(Ui.ROSE, 16, 0x33FFFFFF, this));
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                openNewEntry(day);
            }
        });
        box.addView(add, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));

        MaxHeightScrollView sc = new MaxHeightScrollView(this);
        sc.setFillViewport(false);
        sc.addView(box);
        sc.setBackgroundColor(Color.TRANSPARENT);
        dialog.setContentView(sc);
        Window win = dialog.getWindow();
        if (win != null) {
            win.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = getResources().getDisplayMetrics().widthPixels - dp(32);
            win.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    private String typeLabel(String type) {
        if ("sex".equals(type)) return "做爱";
        if ("mast".equals(type)) return "自慰";
        return "记录";
    }

    private int typeColor(String type) {
        if ("sex".equals(type)) return Ui.ROSE;
        if ("mast".equals(type)) return Ui.GOLD;
        return Ui.MUTED;
    }

    private int typeBg(String type) {
        if ("sex".equals(type)) return 0x14B9556C;
        if ("mast".equals(type)) return 0x18C08A55;
        return 0x149A8A8F;
    }

    private View buildEditorHeader() {
        LinearLayout row = Ui.row(this);
        TextView back = circleButton("‹");
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeEditor();
            }
        });
        row.addView(back, new LinearLayout.LayoutParams(dp(36), dp(36)));

        LinearLayout col = Ui.col(this);
        col.setPadding(dp(12), 0, 0, 0);
        col.addView(Ui.label(this, "ENTRY"));
        col.addView(Ui.space(this, 4));
        col.addView(Ui.serif(this, humanDate(date) + " " + weekday(date), 24, Ui.INK, true));
        col.addView(Ui.space(this, 3));
        String sub;
        if (exists) {
            int index = 0;
            List<Store.Entry> dayList = store.byDate(date);
            for (int i = 0; i < dayList.size(); i++) {
                if (dayList.get(i).id.equals(editingId)) index = i + 1;
            }
            sub = "第 " + index + " 次 · " + typeLabel(activityType);
        } else {
            sub = "新增一次 · 只选一种";
        }
        col.addView(Ui.tv(this, sub, 11, Ui.MUTED, false));
        row.addView(col, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        String g = selectedGender == null ? "secret" : selectedGender;
        TextView avatar = Ui.tv(this, genderGlyph(g), 18, Ui.ROSE, false);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(Ui.bg(Ui.alpha(Ui.ROSE, 0x24), 30, this));
        row.addView(avatar, new LinearLayout.LayoutParams(dp(42), dp(42)));

        if (exists) {
            TextView add = Ui.tv(this, "＋", 18, Ui.ROSE, true);
            add.setGravity(Gravity.CENTER);
            add.setBackground(Ui.ripple(Ui.alpha(Ui.ROSE, 0x18), 30,
                    Ui.alpha(Ui.ROSE, 0x33), this));
            add.setContentDescription("当天再加一次");
            add.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    addAnotherEntry();
                }
            });
            LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(dp(38), dp(38));
            alp.leftMargin = dp(8);
            row.addView(add, alp);
        }
        if (exists) {
            TextView del = Ui.tv(this, "×", 20, Ui.DANGER, true);
            del.setGravity(Gravity.CENTER);
            del.setBackground(Ui.ripple(Ui.alpha(Ui.DANGER, 0x18), 30,
                    Ui.alpha(Ui.DANGER, 0x33), this));
            del.setContentDescription("删除这一次记录");
            del.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    confirmDeleteCurrent();
                }
            });
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(38), dp(38));
            dlp.leftMargin = dp(8);
            row.addView(del, dlp);
        }
        return row;
    }

    private View buildDiaryHeader() {
        LinearLayout row = Ui.row(this);
        LinearLayout col = Ui.col(this);
        TextView label = Ui.label(this, "PRIVATE DIARY");
        col.addView(label);
        col.addView(Ui.space(this, 7));
        TextView title = Ui.serif(this, "做了吗", 32, Ui.INK, true);
        col.addView(title);
        col.addView(Ui.space(this, 5));
        String msg = "today".equals(date) ? "今天的感觉，值得留下来。" : "补记这一天，也算数。";
        col.addView(Ui.tv(this, msg, 12, Ui.MUTED, false));
        row.addView(col, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        String g = selectedGender == null ? "secret" : selectedGender;
        TextView avatar = Ui.tv(this, genderGlyph(g), 20, Ui.ROSE, false);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(Ui.bg(Ui.alpha(Ui.ROSE, 0x24), 30, this));
        row.addView(avatar, new LinearLayout.LayoutParams(dp(46), dp(46)));
        return row;
    }

    private View buildDateCard() {
        LinearLayout card = Ui.card(this);
        card.setPadding(dp(10), dp(10), dp(10), dp(10));

        LinearLayout row = Ui.row(this);
        TextView prev = circleButton("‹");
        prev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shiftDay(-1);
            }
        });
        row.addView(prev, new LinearLayout.LayoutParams(dp(36), dp(36)));

        LinearLayout center = Ui.col(this);
        center.setGravity(Gravity.CENTER);
        center.setPadding(dp(6), dp(2), dp(6), dp(2));
        center.setClickable(true);
        TextView dateText = Ui.tv(this, humanDate(date), 18, Ui.INK, true);
        dateText.setGravity(Gravity.CENTER);
        TextView sub = Ui.tv(this, weekday(date) + (isToday(date) ? " · 今天" : ""),
                11, Ui.MUTED, false);
        sub.setGravity(Gravity.CENTER);
        center.addView(dateText);
        center.addView(Ui.space(this, 3));
        center.addView(sub);
        center.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDatePicker();
            }
        });
        row.addView(center, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView next = circleButton("›");
        if (isToday(date)) {
            next.setAlpha(0.25f);
        } else {
            next.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    shiftDay(1);
                }
            });
        }
        row.addView(next, new LinearLayout.LayoutParams(dp(36), dp(36)));
        row.addView(Ui.space(this, 4));
        TextView cal = circleButton("历");
        cal.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        cal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDatePicker();
            }
        });
        row.addView(cal, new LinearLayout.LayoutParams(dp(36), dp(36)));
        card.addView(row);

        if (!isToday(date)) {
            LinearLayout bottom = Ui.row(this);
            bottom.setGravity(Gravity.END);
            TextView back = Ui.tv(this, "回到今天 →", 11, Ui.ROSE, true);
            back.setPadding(dp(8), dp(7), dp(4), dp(2));
            back.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    goToday();
                }
            });
            bottom.addView(back);
            card.addView(bottom);
        }
        return card;
    }

    private TextView circleButton(String text) {
        TextView v = Ui.tv(this, text, 20, Ui.DEEP, false);
        v.setGravity(Gravity.CENTER);
        v.setBackground(Ui.ripple(Ui.SOFT2, 30, Ui.alpha(Ui.ROSE, 0x33), this));
        v.setClickable(true);
        return v;
    }

    private View buildActivityCard() {
        LinearLayout card = Ui.card(this);
        card.addView(sectionHeader("01", "这一次是什么", "单选"));
        card.addView(Ui.space(this, 13));

        LinearLayout row = Ui.row(this);
        sexTile = activityTile("♥", "做爱", "和伴侣", true);
        mastTile = activityTile("✦", "自慰", "自己", false);
        LinearLayout.LayoutParams left = new LinearLayout.LayoutParams(0, dp(94), 1f);
        left.rightMargin = dp(9);
        row.addView(sexTile, left);
        row.addView(mastTile, new LinearLayout.LayoutParams(0, dp(94), 1f));
        card.addView(row);

        sexTile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean deselect = "sex".equals(activityType);
                activityType = deselect ? "" : "sex";
                method = "";
                methodCustom = "";
                markDirty();
                refreshActivityTiles();
                refreshMethodRow();
                if (!deselect) showMethodSheet();
            }
        });
        mastTile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean deselect = "mast".equals(activityType);
                activityType = deselect ? "" : "mast";
                method = "";
                methodCustom = "";
                markDirty();
                refreshActivityTiles();
                refreshMethodRow();
                if (!deselect) showMethodSheet();
            }
        });
        card.addView(Ui.space(this, 10));
        card.addView(buildMethodRow());
        refreshActivityTiles();
        return card;
    }

    private View buildMethodRow() {
        methodRow = Ui.row(this);
        methodRow.setGravity(Gravity.CENTER_VERTICAL);
        methodText = Ui.tv(this, "方式 · 未选择", 12, Ui.MUTED, false);
        methodRow.addView(methodText, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView change = Ui.tv(this, "选择", 12, Ui.ROSE, true);
        change.setPadding(dp(8), dp(6), dp(8), dp(6));
        change.setBackground(Ui.ripple(Ui.SOFT2, 14, Ui.alpha(Ui.ROSE, 0x22), this));
        change.setClickable(true);
        change.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMethodSheet();
            }
        });
        methodRow.addView(change);
        refreshMethodRow();
        return methodRow;
    }

    private void refreshMethodRow() {
        if (methodRow == null || methodText == null) return;
        String label = methodLabel(method, methodCustom);
        if (label.isEmpty()) {
            methodText.setText(activityType.isEmpty() ? "方式 · 先选这一项" : "方式 · 未选择");
            methodText.setTextColor(Ui.MUTED);
        } else {
            methodText.setText("方式 · " + label);
            methodText.setTextColor(Ui.DEEP);
        }
        if (methodRow.getChildCount() > 1) {
            TextView change = (TextView) methodRow.getChildAt(1);
            change.setText(label.isEmpty() ? "选择" : "更改");
        }
    }

    private String methodLabel(String id, String custom) {
        if (id == null || id.isEmpty()) return "";
        for (int i = 0; i < SEX_METHOD_IDS.length; i++) {
            if (SEX_METHOD_IDS[i].equals(id)) {
                if ("custom".equals(id)) {
                    return custom == null || custom.trim().isEmpty() ? "自定义" : custom.trim();
                }
                return SEX_METHOD_LABELS[i];
            }
        }
        for (int i = 0; i < MAST_METHOD_IDS.length; i++) {
            if (MAST_METHOD_IDS[i].equals(id)) return MAST_METHOD_LABELS[i];
        }
        return "";
    }

    private void applyMethod(String id) {
        method = id;
        methodCustom = "";
        markDirty();
        refreshMethodRow();
        animateIn(methodRow);
    }

    private void showMethodSheet() {
        if (activityType.isEmpty()) return;
        final boolean sex = "sex".equals(activityType);
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout sheet = Ui.col(this);
        sheet.setBackground(Ui.bg(Ui.CARD, 28, this));
        sheet.setPadding(dp(20), dp(18), dp(20), dp(22));

        LinearLayout head = Ui.row(this);
        LinearLayout titleBox = Ui.col(this);
        titleBox.addView(Ui.label(this, sex ? "SEX MODE" : "MAST MODE"));
        titleBox.addView(Ui.space(this, 5));
        titleBox.addView(Ui.serif(this, sex ? "做爱方式" : "自慰方式", 23, Ui.INK, true));
        titleBox.addView(Ui.space(this, 4));
        titleBox.addView(Ui.tv(this, "选一个最接近这一次的，之后还能改", 11, Ui.MUTED, false));
        head.addView(titleBox, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView close = Ui.tv(this, "×", 20, Ui.MUTED, false);
        close.setGravity(Gravity.CENTER);
        close.setBackground(Ui.bg(Ui.SOFT2, 24, this));
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { dialog.dismiss(); }
        });
        head.addView(close, new LinearLayout.LayoutParams(dp(34), dp(34)));
        sheet.addView(head);
        sheet.addView(Ui.space(this, 16));

        if (sex) {
            for (int i = 0; i < SEX_METHOD_IDS.length; i++) {
                sheet.addView(methodOption(dialog, SEX_METHOD_IDS[i],
                        SEX_METHOD_LABELS[i], SEX_METHOD_HINTS[i]));
                sheet.addView(Ui.space(this, 8));
            }
        } else {
            for (int i = 0; i < MAST_METHOD_IDS.length; i++) {
                sheet.addView(methodOption(dialog, MAST_METHOD_IDS[i],
                        MAST_METHOD_LABELS[i], MAST_METHOD_HINTS[i]));
                sheet.addView(Ui.space(this, 8));
            }
        }

        dialog.setContentView(sheet);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.35f);
            window.setGravity(Gravity.BOTTOM);
            window.setWindowAnimations(R.style.BottomSheetAnimation);
        }
        dialog.show();
        if (window != null) {
            int width = getResources().getDisplayMetrics().widthPixels - dp(24);
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.BOTTOM);
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.y = dp(12);
            window.setAttributes(lp);
        }
    }

    private View methodOption(final Dialog dialog, final String id,
                              String title, String hint) {
        boolean on = id.equals(method);
        LinearLayout row = Ui.row(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        row.setBackground(Ui.ripple(on ? Ui.alpha(Ui.ROSE, 0x14) : Ui.SOFT2,
                16, Ui.alpha(Ui.ROSE, 0x2A), this));
        row.setClickable(true);

        LinearLayout text = Ui.col(this);
        text.addView(Ui.tv(this, title, 15, on ? Ui.DEEP : Ui.INK, true));
        text.addView(Ui.space(this, 2));
        text.addView(Ui.tv(this, hint, 10, Ui.MUTED, false));
        row.addView(text, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView mark = Ui.tv(this, on ? "✓" : "›", 18,
                on ? Ui.ROSE : Ui.MUTED, true);
        mark.setGravity(Gravity.CENTER);
        row.addView(mark, new LinearLayout.LayoutParams(dp(30), dp(30)));

        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                if ("custom".equals(id)) {
                    showCustomMethodDialog();
                } else {
                    applyMethod(id);
                }
            }
        });
        return row;
    }

    private void showCustomMethodDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout card = Ui.col(this);
        card.setBackground(Ui.bg(Ui.CARD, 26, this));
        card.setPadding(dp(22), dp(20), dp(22), dp(18));

        card.addView(Ui.label(this, "CUSTOM"));
        card.addView(Ui.space(this, 6));
        card.addView(Ui.serif(this, "自定义方式", 22, Ui.INK, true));
        card.addView(Ui.space(this, 6));
        card.addView(Ui.tv(this, "写一个你习惯的说法，只存在这台手机里", 11, Ui.MUTED, false));
        card.addView(Ui.space(this, 12));

        final EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(methodCustom == null ? "" : methodCustom);
        input.setHint("sex".equals(activityType) ? "例如：先戴套，后来摘了" : "例如：用了某个玩具");
        input.setTextColor(Ui.INK);
        input.setHintTextColor(Ui.MUTED);
        input.setTextSize(14);
        input.setPadding(dp(12), dp(10), dp(12), dp(10));
        input.setBackground(Ui.strokeBg(Ui.SOFT2, 14, Ui.LINE, 1, this));
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(24)});
        card.addView(input);

        LinearLayout actions = Ui.row(this);
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        alp.topMargin = dp(16);
        card.addView(actions, alp);
        TextView cancel = Ui.tv(this, "取消", 14, Ui.INK, true);
        cancel.setGravity(Gravity.CENTER);
        cancel.setBackground(Ui.ripple(Ui.SOFT2, 22, Ui.alpha(Ui.INK, 0x22), this));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, dp(46), 1f);
        clp.rightMargin = dp(8);
        actions.addView(cancel, clp);
        TextView ok = Ui.tv(this, "用这个", 14, Color.WHITE, true);
        ok.setGravity(Gravity.CENTER);
        ok.setBackground(Ui.ripple(Ui.ROSE, 22, 0x33FFFFFF, this));
        actions.addView(ok, new LinearLayout.LayoutParams(0, dp(46), 1f));

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { dialog.dismiss(); }
        });
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                method = "custom";
                methodCustom = input.getText().toString().trim();
                markDirty();
                refreshMethodRow();
                animateIn(methodRow);
                dialog.dismiss();
            }
        });

        dialog.setContentView(card);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.5f);
        }
        dialog.show();
        if (window != null) {
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.88f);
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
        input.requestFocus();
        input.setSelection(input.getText().length());
    }

    private LinearLayout activityTile(String glyph, String title, String sub, boolean first) {
        LinearLayout tile = Ui.col(this);
        tile.setGravity(Gravity.CENTER);
        tile.setClickable(true);
        TextView icon = Ui.tv(this, glyph, 24, Ui.ROSE, false);
        icon.setGravity(Gravity.CENTER);
        TextView t = Ui.tv(this, title, 15, Ui.INK, true);
        t.setGravity(Gravity.CENTER);
        TextView s = Ui.tv(this, sub, 10, Ui.MUTED, false);
        s.setGravity(Gravity.CENTER);
        tile.addView(icon);
        tile.addView(Ui.space(this, 4));
        tile.addView(t);
        tile.addView(Ui.space(this, 2));
        tile.addView(s);
        if (first) {
            sexGlyph = icon;
            sexTitle = t;
            sexSub = s;
        } else {
            mastGlyph = icon;
            mastTitle = t;
            mastSub = s;
        }
        return tile;
    }

    private void refreshActivityTiles() {
        styleActivityTile(sexTile, sexGlyph, sexTitle, sexSub, "sex".equals(activityType));
        styleActivityTile(mastTile, mastGlyph, mastTitle, mastSub, "mast".equals(activityType));
    }

    private void styleActivityTile(LinearLayout tile, TextView glyph, TextView title,
                                   TextView sub, boolean on) {
        tile.setBackground(on
                ? Ui.gradient(0xFFC4657A, 0xFFA94760, 18, this)
                : Ui.strokeBg(Ui.SOFT2, 18, Ui.LINE, 1, this));
        glyph.setTextColor(on ? 0xFFFFFFFF : Ui.ROSE);
        title.setTextColor(on ? 0xFFFFFFFF : Ui.INK);
        sub.setTextColor(on ? 0xFFF4D9DE : Ui.MUTED);
        tile.animate().scaleX(on ? 1.02f : 1f).scaleY(on ? 1.02f : 1f)
                .setDuration(160).start();
    }

    private LinearLayout sectionHeader(String index, String title, String hint) {
        LinearLayout row = Ui.row(this);
        TextView idx = Ui.label(this, index);
        idx.setPadding(0, 0, dp(8), 0);
        row.addView(idx);
        row.addView(Ui.tv(this, title, 16, Ui.INK, true),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (hint != null) {
            row.addView(Ui.tv(this, hint, 10, Ui.MUTED, false));
        }
        return row;
    }

    private View buildExtrasCard() {
        LinearLayout card = Ui.card(this);
        card.addView(sectionHeader("02", "这次用到的", "可多选"));
        card.addView(Ui.space(this, 8));
        card.addView(Ui.tv(this, "选填，不选就是没有用", 11, Ui.MUTED, false));
        card.addView(Ui.space(this, 12));

        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        LinearLayout row = Ui.row(this);
        extraChips.clear();
        for (int i = 0; i < EXTRA_IDS.length; i++) {
            final String id = EXTRA_IDS[i];
            TextView chip = Ui.pill(this, EXTRA_LABELS[i], extras.contains(id));
            chip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (extras.contains(id)) extras.remove(id);
                    else extras.add(id);
                    markDirty();
                    refreshExtraChips();
                }
            });
            LinearLayout.LayoutParams lp = Ui.lp(ViewGroup.LayoutParams.WRAP_CONTENT, dp(34));
            if (i < EXTRA_IDS.length - 1) lp.rightMargin = dp(7);
            row.addView(chip, lp);
            extraChips.add(chip);
        }
        scroll.addView(row);
        card.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        refreshExtraChips();
        return card;
    }

    private void refreshExtraChips() {
        for (int i = 0; i < extraChips.size(); i++) {
            boolean on = extras.contains(EXTRA_IDS[i]);
            TextView chip = extraChips.get(i);
            chip.setTextColor(on ? Color.WHITE : Ui.INK);
            chip.setTypeface(android.graphics.Typeface.create(
                    on ? "sans-serif-medium" : "sans-serif", android.graphics.Typeface.NORMAL));
            chip.setBackground(on ? Ui.bg(Ui.ROSE, 30, this)
                    : Ui.strokeBg(Ui.SOFT2, 30, Ui.LINE, 1, this));
        }
    }

    private String extraLabel(String id) {
        for (int i = 0; i < EXTRA_IDS.length; i++) {
            if (EXTRA_IDS[i].equals(id)) return EXTRA_LABELS[i];
        }
        return id;
    }

    private String extrasSummary(Store.Entry e) {
        if (e.extras == null || e.extras.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (String id : e.extras) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append(extraLabel(id));
        }
        return sb.toString();
    }

    private View buildDurationCard() {
        durationTapCount = 0;
        durationTapAt = 0;
        LinearLayout card = Ui.card(this);
        LinearLayout top = Ui.row(this);
        LinearLayout left = Ui.col(this);
        left.addView(sectionHeader("03", "持续了多久", null));
        left.addView(Ui.space(this, 8));
        left.addView(Ui.tv(this, "拖动或点一个常用时长", 11, Ui.MUTED, false));
        top.addView(left, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        durationValue = Ui.tv(this, durationMinutes == 0 ? "未记录" : durationMinutes + " 分钟",
                19, Ui.ROSE, true);
        durationValue.setClickable(true);
        durationValue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long now = System.currentTimeMillis();
                if (now - durationTapAt > 1000) durationTapCount = 0;
                durationTapAt = now;
                durationTapCount++;
                if (durationTapCount >= 10) {
                    durationTapCount = 0;
                    showDurationEasterEgg();
                }
            }
        });
        top.addView(durationValue);
        card.addView(top);

        card.addView(Ui.space(this, 12));
        durationBar = new SeekBar(this);
        durationBar.setMax(durationMax);
        durationBar.setProgress(Math.min(durationMinutes, durationMax));
        durationBar.setProgressTintList(ColorStateList.valueOf(Ui.ROSE));
        durationBar.setProgressBackgroundTintList(ColorStateList.valueOf(Ui.LINE));
        durationBar.setThumbTintList(ColorStateList.valueOf(Ui.DEEP));
        durationBar.setPadding(0, 0, 0, 0);
        card.addView(durationBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(32)));
        durationBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    setDuration(progress, false);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        LinearLayout presets = Ui.row(this);
        durationChips.clear();
        for (int i = 0; i < DURATION_PRESETS.length; i++) {
            final int minutes = DURATION_PRESETS[i];
            TextView chip = Ui.pill(this, minutes + " 分钟", minutes == durationMinutes);
            chip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setDuration(minutes, true);
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(32), 1f);
            if (i < DURATION_PRESETS.length - 1) lp.rightMargin = dp(6);
            presets.addView(chip, lp);
            durationChips.add(chip);
        }
        card.addView(presets);
        refreshDurationChips();
        return card;
    }

    private void showDurationEasterEgg() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout card = Ui.col(this);
        card.setBackground(Ui.bg(Ui.CARD, 28, this));
        card.setPadding(dp(24), dp(22), dp(24), dp(20));

        TextView eyebrow = Ui.label(this, "EASTER EGG");
        eyebrow.setGravity(Gravity.CENTER);
        card.addView(eyebrow);

        TextView badge = Ui.serif(this, "10", 28, Color.WHITE, true);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(Ui.gradient(Ui.ROSE, Ui.DEEP, 40, this));
        LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(dp(64), dp(64));
        badgeLp.gravity = Gravity.CENTER_HORIZONTAL;
        badgeLp.topMargin = dp(12);
        card.addView(badge, badgeLp);

        TextView title = Ui.serif(this, "你挺牛逼呗", 26, Ui.INK, true);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleLp.topMargin = dp(14);
        card.addView(title, titleLp);

        TextView msg = Ui.tv(this,
                "连续点十次，手速可以。\n本次编辑把时长上限开到 5 小时。",
                13, Ui.MUTED, false);
        msg.setGravity(Gravity.CENTER);
        msg.setLineSpacing(dp(4), 1f);
        LinearLayout.LayoutParams msgLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        msgLp.topMargin = dp(8);
        card.addView(msg, msgLp);

        TextView tag = Ui.tv(this, "仅本次编辑生效", 10, Ui.DEEP, true);
        tag.setGravity(Gravity.CENTER);
        tag.setBackground(Ui.bg(Ui.alpha(Ui.ROSE, 0x18), 20, this));
        LinearLayout.LayoutParams tagLp = new LinearLayout.LayoutParams(dp(120), dp(28));
        tagLp.gravity = Gravity.CENTER_HORIZONTAL;
        tagLp.topMargin = dp(14);
        card.addView(tag, tagLp);

        final TextView confirm = Ui.tv(this, "行，继续记录", 14, Color.WHITE, true);
        confirm.setGravity(Gravity.CENTER);
        confirm.setBackground(Ui.ripple(Ui.ROSE, 24, Ui.DEEP, this));
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        btnLp.topMargin = dp(18);
        card.addView(confirm, btnLp);
        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                durationMax = 300;
                if (durationBar != null) {
                    durationBar.setMax(300);
                    durationBar.setProgress(Math.min(durationMinutes, 300));
                }
                dialog.dismiss();
            }
        });

        dialog.setContentView(card);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface d, int keyCode, KeyEvent event) {
                // 只允许点按钮退出，返回键也不关。
                return keyCode == KeyEvent.KEYCODE_BACK;
            }
        });

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.55f);
        }
        dialog.show();
        if (window != null) {
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.86f);
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void setDuration(int minutes, boolean moveBar) {
        durationMinutes = Math.max(0, Math.min(300, minutes));
        if (durationValue != null) {
            durationValue.setText(durationMinutes == 0 ? "未记录" : durationMinutes + " 分钟");
        }
        if (moveBar && durationBar != null) durationBar.setProgress(durationMinutes);
        refreshDurationChips();
        markDirty();
    }

    private void refreshDurationChips() {
        int count = Math.min(durationChips.size(), DURATION_PRESETS.length);
        for (int i = 0; i < count; i++) {
            boolean on = DURATION_PRESETS[i] == durationMinutes;
            TextView chip = durationChips.get(i);
            chip.setTextColor(on ? Color.WHITE : Ui.INK);
            chip.setTypeface(android.graphics.Typeface.create(
                    on ? "sans-serif-medium" : "sans-serif", android.graphics.Typeface.NORMAL));
            chip.setBackground(on ? Ui.bg(Ui.ROSE, 30, this)
                    : Ui.strokeBg(Ui.SOFT2, 30, Ui.LINE, 1, this));
        }
    }

    private View buildMoodCard() {
        LinearLayout card = Ui.card(this);
        card.addView(sectionHeader("04", "当时的心情", "单选"));
        card.addView(Ui.space(this, 13));
        LinearLayout row = Ui.row(this);
        moodItems.clear();
        moodLabels.clear();
        for (int i = 0; i < MOOD_IDS.length; i++) {
            final int index = i;
            LinearLayout item = Ui.col(this);
            item.setGravity(Gravity.CENTER);
            item.setPadding(0, dp(9), 0, dp(9));
            item.setClickable(true);
            TextView emoji = Ui.tv(this, MOOD_EMOJI[i], 24, Ui.INK, false);
            emoji.setGravity(Gravity.CENTER);
            TextView label = Ui.tv(this, MOOD_LABEL[i], 10, Ui.MUTED, false);
            label.setGravity(Gravity.CENTER);
            item.addView(emoji);
            item.addView(Ui.space(this, 5));
            item.addView(label);
            item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mood = MOOD_IDS[index].equals(mood) ? "" : MOOD_IDS[index];
                    markDirty();
                    refreshMoodItems();
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (i < MOOD_IDS.length - 1) lp.rightMargin = dp(4);
            row.addView(item, lp);
            moodItems.add(item);
            moodLabels.add(label);
        }
        card.addView(row);
        refreshMoodItems();
        return card;
    }

    private void refreshMoodItems() {
        for (int i = 0; i < moodItems.size(); i++) {
            boolean on = MOOD_IDS[i].equals(mood);
            moodItems.get(i).setBackground(on ? Ui.bg(Ui.SOFT, 16, this) : null);
            moodLabels.get(i).setTextColor(on ? Ui.DEEP : Ui.MUTED);
            moodLabels.get(i).setTypeface(android.graphics.Typeface.create(
                    on ? "sans-serif-medium" : "sans-serif", android.graphics.Typeface.NORMAL));
        }
    }

    private View buildStateCard() {
        LinearLayout card = Ui.card(this);
        card.addView(sectionHeader("05", "身体状态", "单选"));
        card.addView(Ui.space(this, 13));
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        LinearLayout row = Ui.row(this);
        stateChips.clear();
        for (int i = 0; i < STATES.length; i++) {
            final String value = STATES[i];
            TextView chip = Ui.pill(this, value, value.equals(bodyState));
            chip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    bodyState = value.equals(bodyState) ? "" : value;
                    markDirty();
                    refreshStateChips();
                }
            });
            LinearLayout.LayoutParams lp = Ui.lp(ViewGroup.LayoutParams.WRAP_CONTENT, dp(32));
            if (i < STATES.length - 1) lp.rightMargin = dp(7);
            row.addView(chip, lp);
            stateChips.add(chip);
        }
        scroll.addView(row);
        card.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return card;
    }

    private void refreshStateChips() {
        for (int i = 0; i < stateChips.size(); i++) {
            boolean on = STATES[i].equals(bodyState);
            TextView chip = stateChips.get(i);
            chip.setTextColor(on ? Color.WHITE : Ui.INK);
            chip.setTypeface(android.graphics.Typeface.create(
                    on ? "sans-serif-medium" : "sans-serif", android.graphics.Typeface.NORMAL));
            chip.setBackground(on ? Ui.bg(Ui.ROSE, 30, this)
                    : Ui.strokeBg(Ui.SOFT2, 30, Ui.LINE, 1, this));
        }
    }

    private View buildPleasureCard() {
        LinearLayout card = Ui.card(this);
        LinearLayout top = Ui.row(this);
        top.addView(sectionHeader("06", "快感度", null),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        pleasureHint = Ui.tv(this, "点选 0–5", 11, Ui.MUTED, false);
        top.addView(pleasureHint);
        card.addView(top);
        card.addView(Ui.space(this, 12));

        LinearLayout row = Ui.row(this);
        hearts.clear();
        for (int i = 1; i <= 5; i++) {
            final int value = i;
            TextView heart = Ui.tv(this, "♡", 30, Ui.LINE, false);
            heart.setGravity(Gravity.CENTER);
            heart.setPadding(0, dp(2), 0, dp(2));
            heart.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    pleasure = pleasure == value ? 0 : value;
                    markDirty();
                    refreshHearts();
                }
            });
            row.addView(heart, new LinearLayout.LayoutParams(0, dp(46), 1f));
            hearts.add(heart);
        }
        card.addView(row);
        refreshHearts();
        return card;
    }

    private void refreshHearts() {
        for (int i = 0; i < hearts.size(); i++) {
            boolean on = (i + 1) <= pleasure;
            hearts.get(i).setText(on ? "♥" : "♡");
            hearts.get(i).setTextColor(on ? Ui.ROSE : 0xFFDCCBCB);
            hearts.get(i).setTextSize(TypedValue.COMPLEX_UNIT_SP, on ? 31 : 29);
        }
        if (pleasureHint != null) {
            pleasureHint.setText(pleasure == 0 ? "点选 0–5" : PLEASURE_LABEL[pleasure]);
            pleasureHint.setTextColor(pleasure == 0 ? Ui.MUTED : Ui.DEEP);
        }
    }

    private View buildNoteCard() {
        LinearLayout card = Ui.card(this);
        card.addView(sectionHeader("07", "想记点什么", "选填"));
        card.addView(Ui.space(this, 12));
        noteView = new EditText(this);
        noteView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        noteView.setTextColor(Ui.INK);
        noteView.setHintTextColor(Ui.MUTED);
        noteView.setHint("身体感受、当时的心情，或者一句想对自己说的话…");
        noteView.setGravity(Gravity.TOP | Gravity.START);
        noteView.setBackground(Ui.bg(Ui.SOFT2, 16, this));
        noteView.setPadding(dp(14), dp(12), dp(14), dp(12));
        noteView.setMinHeight(dp(96));
        noteView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        noteView.setSingleLine(false);
        bindingNote = true;
        noteView.setText(note);
        bindingNote = false;
        noteView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (bindingNote) return;
                note = s.toString();
                markDirty();
            }
        });
        card.addView(noteView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return card;
    }

    private View buildMediaCard() {
        LinearLayout card = Ui.card(this);
        LinearLayout head = Ui.row(this);
        head.addView(sectionHeader("08", "照片、视频和声音", null),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        head.addView(Ui.tv(this, "最多 9 个", 10, Ui.MUTED, false));
        card.addView(head);
        card.addView(Ui.space(this, 12));

        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        mediaScroll = scroll;
        mediaRow = Ui.row(this);
        scroll.addView(mediaRow);
        card.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(96)));

        card.addView(Ui.space(this, 10));
        LinearLayout buttons = Ui.row(this);
        TextView photo = addMediaButton("＋ 照片");
        TextView video = addMediaButton("＋ 视频");
        TextView audio = addMediaButton("＋ 录音");
        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(0, dp(42), 1f);
        lp1.rightMargin = dp(7);
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0, dp(42), 1f);
        lp2.rightMargin = dp(7);
        buttons.addView(photo, lp1);
        buttons.addView(video, lp2);
        buttons.addView(audio, new LinearLayout.LayoutParams(0, dp(42), 1f));
        card.addView(buttons);

        photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickImages();
            }
        });
        video.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickVideo();
            }
        });
        audio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestAudio();
            }
        });
        refreshMediaRow();
        return card;
    }

    private TextView addMediaButton(String text) {
        TextView v = Ui.tv(this, text, 12, Ui.ROSE, true);
        v.setGravity(Gravity.CENTER);
        v.setBackground(Ui.ripple(Ui.SOFT2, 14, Ui.alpha(Ui.ROSE, 0x33), this));
        v.setClickable(true);
        return v;
    }

    private View buildSaveArea() {
        LinearLayout area = Ui.col(this);
        area.setPadding(0, dp(3), 0, 0);
        saveBtn = Ui.tv(this, "保存这一天", 16, Color.WHITE, true);
        saveBtn.setGravity(Gravity.CENTER);
        saveBtn.setBackground(Ui.bg(Ui.ROSE, 18, this));
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean wasEmpty = isEmptyDraft();
                boolean wasExisting = exists;
                commitDraft(true);
                if (wasEmpty) toast(wasExisting ? "这一次已清空" : "还没有内容，未保存");
                else toast("已保存，只在这台手机里");
            }
        });
        area.addView(saveBtn, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(54)));
        area.addView(Ui.space(this, 10));
        TextView foot = Ui.tv(this, "仅保存在本机 · 可随时删除", 10, Ui.MUTED, false);
        foot.setGravity(Gravity.CENTER);
        area.addView(foot);
        refreshSaveButton();
        return area;
    }

    private void markDirty() {
        dirty = true;
        refreshSaveButton();
    }

    private void refreshSaveButton() {
        if (saveBtn == null) return;
        if (dirty) {
            saveBtn.setText("保存这一次");
            saveBtn.setBackground(Ui.bg(Ui.ROSE, 18, this));
            saveBtn.setTextColor(Color.WHITE);
        } else if (exists) {
            saveBtn.setText("已保存");
            saveBtn.setBackground(Ui.bg(Ui.alpha(Ui.ROSE, 0x24), 18, this));
            saveBtn.setTextColor(Ui.ROSE);
        } else {
            saveBtn.setText("保存这一次");
            saveBtn.setBackground(Ui.bg(Ui.ROSE, 18, this));
            saveBtn.setTextColor(Color.WHITE);
        }
    }

    private boolean isEmptyDraft() {
        return activityType.isEmpty() && method.isEmpty() && durationMinutes == 15
                && mood.isEmpty() && bodyState.isEmpty() && pleasure == 0
                && note.trim().isEmpty() && extras.isEmpty() && media.isEmpty();
    }

    private void commitDraft(boolean force) {
        if (!dirty && !force) return;
        if (noteView != null) note = noteView.getText().toString();
        Store.Entry old = editingId == null ? null : store.byId(editingId);
        boolean changed = false;
        if (isEmptyDraft()) {
            if (old != null) {
                store.removeById(old.id);
                changed = true;
            }
            editingId = null;
            exists = false;
        } else {
            Store.Entry e = old == null ? new Store.Entry() : old;
            e.id = editingId;
            e.date = date;
            e.type = activityType;
            e.method = method;
            e.methodCustom = methodCustom;
            e.durationMinutes = durationMinutes;
            e.mood = mood;
            e.bodyState = bodyState;
            e.pleasure = pleasure;
            e.note = note;
            e.extras = new ArrayList<>(extras);
            e.media = new ArrayList<>(media);
            store.put(e);
            editingId = e.id;
            exists = true;
            changed = true;
        }
        dirty = false;
        refreshSaveButton();
    }

    /* ==================== 日期 ==================== */

    private void openDatePicker() {
        Calendar c = parseDate(date);
        DatePickerDialog dialog = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int day) {
                        Calendar picked = Calendar.getInstance();
                        picked.set(year, month, day);
                        commitDraft(false);
                        date = formatDate(picked);
                        loadDraft();
                        render();
                    }
                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        dialog.show();
    }

    private void shiftDay(int delta) {
        Calendar c = parseDate(date);
        c.add(Calendar.DAY_OF_MONTH, delta);
        Calendar today = Calendar.getInstance();
        if (c.after(today) && !sameDay(c, today)) return;
        commitDraft(false);
        date = formatDate(c);
        loadDraft();
        render();
    }

    private void goToday() {
        if (isToday(date)) return;
        commitDraft(false);
        date = Store.today();
        loadDraft();
        render();
    }

    private boolean sameDay(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    private static Calendar parseDate(String value) {
        Calendar c = Calendar.getInstance();
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(value);
            if (d != null) c.setTime(d);
        } catch (Exception ignored) {
        }
        return c;
    }

    private static String formatDate(Calendar c) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.getTime());
    }

    private static String humanDate(String value) {
        return new SimpleDateFormat("M月d日", Locale.CHINA).format(parseDate(value).getTime());
    }

    private static String weekday(String value) {
        String[] names = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
        return names[parseDate(value).get(Calendar.DAY_OF_WEEK) - 1];
    }

    private static boolean isToday(String value) {
        return Store.today().equals(value);
    }

    private static String monthLabel(String yyyymm) {
        String[] parts = yyyymm.split("-");
        if (parts.length < 2) return yyyymm;
        return parts[0] + "年" + Integer.parseInt(parts[1]) + "月";
    }

    /* ==================== 时间轴 ==================== */

    private View buildTimeline() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Ui.BG);
        scroll.setClipToPadding(false);
        LinearLayout page = Ui.col(this);
        page.setPadding(dp(18), dp(18), dp(18), dp(26));
        scroll.addView(page, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout head = Ui.row(this);
        LinearLayout headCol = Ui.col(this);
        headCol.addView(Ui.label(this, "TIMELINE"));
        headCol.addView(Ui.space(this, 7));
        headCol.addView(Ui.serif(this, "时间轴", 32, Ui.INK, true));
        headCol.addView(Ui.space(this, 5));
        headCol.addView(Ui.tv(this, "回看身体与心情的轨迹。", 12, Ui.MUTED, false));
        head.addView(headCol, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView count = Ui.tv(this, store.countDays() + " 天 · " + store.entries().size() + " 次", 12, Ui.ROSE, true);
        count.setPadding(dp(12), dp(7), dp(12), dp(7));
        count.setBackground(Ui.bg(Ui.alpha(Ui.ROSE, 0x24), 30, this));
        head.addView(count);
        page.addView(head);
        page.addView(Ui.space(this, 16));

        page.addView(buildStatsCard());
        page.addView(Ui.space(this, 14));

        HorizontalScrollView filters = new HorizontalScrollView(this);
        filters.setHorizontalScrollBarEnabled(false);
        LinearLayout filterRow = Ui.row(this);
        addFilterChip(filterRow, "all", "全部");
        addFilterChip(filterRow, "sex", "做爱");
        addFilterChip(filterRow, "mast", "自慰");
        addFilterChip(filterRow, "media", "有媒体");
        filters.addView(filterRow);
        page.addView(filters);
        page.addView(Ui.space(this, 16));

        List<Store.Entry> list = filteredEntries();
        if (list.isEmpty()) {
            LinearLayout empty = Ui.card(this);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(20), dp(42), dp(20), dp(42));
            TextView icon = Ui.tv(this, "✦", 30, Ui.MUTED, false);
            icon.setGravity(Gravity.CENTER);
            empty.addView(icon);
            empty.addView(Ui.space(this, 12));
            TextView t = Ui.tv(this, "这里还空着", 17, Ui.INK, true);
            t.setGravity(Gravity.CENTER);
            empty.addView(t);
            empty.addView(Ui.space(this, 6));
            TextView s = Ui.tv(this, "回到日记页，记下今天的第一笔。", 12, Ui.MUTED, false);
            s.setGravity(Gravity.CENTER);
            empty.addView(s);
            page.addView(empty);
            return scroll;
        }

        String currentMonth = "";
        for (Store.Entry e : list) {
            String month = e.date.substring(0, 7);
            if (!month.equals(currentMonth)) {
                currentMonth = month;
                LinearLayout mh = Ui.row(this);
                mh.setPadding(dp(2), dp(10), dp(2), dp(10));
                TextView label = Ui.tv(this, monthLabel(month), 14, Ui.INK, true);
                mh.addView(label, new LinearLayout.LayoutParams(0,
                        ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                int n = 0;
                for (Store.Entry x : list) {
                    if (x.date.startsWith(month)) n++;
                }
                mh.addView(Ui.tv(this, n + " 条", 10, Ui.MUTED, false));
                page.addView(mh);
            }
            page.addView(timelineCard(e));
        }
        page.addView(Ui.space(this, 8));
        return scroll;
    }

    private void addFilterChip(LinearLayout row, final String id, String text) {
        boolean on = id.equals(timelineFilter);
        TextView chip = Ui.pill(this, text, on);
        chip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (timelineFilter.equals(id)) return;
                timelineFilter = id;
                render();
            }
        });
        LinearLayout.LayoutParams lp = Ui.lp(ViewGroup.LayoutParams.WRAP_CONTENT, dp(32));
        lp.rightMargin = dp(7);
        row.addView(chip, lp);
    }

    private List<Store.Entry> filteredEntries() {
        List<Store.Entry> out = new ArrayList<>();
        for (Store.Entry e : store.entries()) {
            if ("sex".equals(timelineFilter) && !"sex".equals(e.type)) continue;
            if ("mast".equals(timelineFilter) && !"mast".equals(e.type)) continue;
            if ("media".equals(timelineFilter) && e.media.isEmpty()) continue;
            out.add(e);
        }
        return out;
    }

    private View buildStatsCard() {
        LinearLayout card = Ui.card(this);
        card.setPadding(dp(8), dp(16), dp(8), dp(16));
        LinearLayout row = Ui.row(this);

        int total = store.entries().size();
        String monthPrefix = new SimpleDateFormat("yyyy-MM", Locale.US).format(new Date());
        int month = 0;
        for (Store.Entry e : store.entries()) {
            if (e.date.startsWith(monthPrefix)) month++;
        }
        int streak = streakDays();

        row.addView(statCell(String.valueOf(total), "累计次数"), new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(Ui.vdivider(this, 30));
        row.addView(statCell(String.valueOf(month), "本月次数"), new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(Ui.vdivider(this, 30));
        row.addView(statCell(streak + " 天", "连续"), new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        card.addView(row);
        return card;
    }

    private View statCell(String value, String label) {
        LinearLayout c = Ui.col(this);
        c.setGravity(Gravity.CENTER);
        TextView v = Ui.serif(this, value, 23, Ui.DEEP, true);
        v.setGravity(Gravity.CENTER);
        TextView l = Ui.tv(this, label, 10, Ui.MUTED, false);
        l.setGravity(Gravity.CENTER);
        c.addView(v);
        c.addView(Ui.space(this, 4));
        c.addView(l);
        return c;
    }

    private int streakDays() {
        java.util.HashSet<String> days = new java.util.HashSet<>();
        for (Store.Entry e : store.entries()) {
            if (e.hasActivity()) days.add(e.date);
        }
        Calendar c = Calendar.getInstance();
        if (!days.contains(formatDate(c))) c.add(Calendar.DAY_OF_MONTH, -1);
        int n = 0;
        while (days.contains(formatDate(c))) {
            n++;
            c.add(Calendar.DAY_OF_MONTH, -1);
        }
        return n;
    }

    private View timelineCard(final Store.Entry e) {
        LinearLayout card = Ui.row(this);
        card.setBackground(Ui.ripple(Ui.CARD, 20, 0x14B9556C, this));
        card.setPadding(dp(14), dp(14), dp(12), dp(14));
        card.setClickable(true);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.bottomMargin = dp(10);
        card.setLayoutParams(clp);

        LinearLayout left = Ui.col(this);
        left.setGravity(Gravity.CENTER);
        TextView day = Ui.serif(this, e.date.substring(8, 10), 23, Ui.ROSE, true);
        day.setGravity(Gravity.CENTER);
        TextView wd = Ui.tv(this, weekday(e.date), 10, Ui.MUTED, false);
        wd.setGravity(Gravity.CENTER);
        left.addView(day);
        left.addView(Ui.space(this, 2));
        left.addView(wd);
        card.addView(left, new LinearLayout.LayoutParams(dp(42),
                ViewGroup.LayoutParams.WRAP_CONTENT));

        card.addView(Ui.vdivider(this, 58));
        LinearLayout body = Ui.col(this);
        body.setPadding(dp(12), 0, 0, 0);

        LinearLayout tags = Ui.row(this);
        tags.addView(miniTag(typeLabel(e.type), typeColor(e.type), typeBg(e.type)));
        if (e.durationMinutes > 0) tags.addView(miniTag(e.durationMinutes + " 分钟", Ui.DEEP, 0x0F7E3345));
        body.addView(tags);

        String methodLine = methodLabel(e.method, e.methodCustom);
        if (!methodLine.isEmpty()) {
            body.addView(Ui.space(this, 6));
            body.addView(Ui.tv(this, "方式 · " + methodLine, 11, Ui.DEEP, false));
        }

        String extraLine = extrasSummary(e);
        if (!extraLine.isEmpty()) {
            body.addView(Ui.space(this, 6));
            body.addView(Ui.tv(this, "用到 · " + extraLine, 11, Ui.ROSE, false));
        }

        String moodLine = moodSummary(e);
        if (!moodLine.isEmpty()) {
            body.addView(Ui.space(this, 8));
            body.addView(Ui.tv(this, moodLine, 12, Ui.INK, false));
        }
        if (e.note != null && !e.note.trim().isEmpty()) {
            body.addView(Ui.space(this, 6));
            TextView n = Ui.tv(this, e.note.trim().replace('\n', ' '), 12, Ui.MUTED, false);
            n.setMaxLines(2);
            n.setEllipsize(android.text.TextUtils.TruncateAt.END);
            body.addView(n);
        }
        body.addView(Ui.space(this, 7));
        LinearLayout foot = Ui.row(this);
        if (e.pleasure > 0) {
            String hs = "";
            for (int i = 0; i < e.pleasure; i++) hs += "♥";
            foot.addView(Ui.tv(this, hs, 10, Ui.ROSE, false));
            foot.addView(Ui.space(this, 5));
        }
        if (!e.media.isEmpty()) {
            foot.addView(Ui.tv(this, "● " + e.media.size() + " 个媒体", 10, Ui.MUTED, false));
        }
        body.addView(foot);

        card.addView(body, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView chevron = Ui.tv(this, "›", 20, Ui.MUTED, false);
        card.addView(chevron);
        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDetail(e);
            }
        });
        return card;
    }

    private TextView miniTag(String text, int color, int bg) {
        TextView t = Ui.tv(this, text, 10, color, true);
        t.setPadding(dp(8), dp(3), dp(8), dp(3));
        t.setBackground(Ui.bg(bg, 20, this));
        LinearLayout.LayoutParams lp = Ui.lp(ViewGroup.LayoutParams.WRAP_CONTENT, dp(22));
        lp.rightMargin = dp(5);
        t.setLayoutParams(lp);
        return t;
    }

    private String moodSummary(Store.Entry e) {
        String s = "";
        for (int i = 0; i < MOOD_IDS.length; i++) {
            if (MOOD_IDS[i].equals(e.mood)) s = MOOD_EMOJI[i] + " " + MOOD_LABEL[i];
        }
        if (!e.bodyState.isEmpty()) {
            if (!s.isEmpty()) s += "  ·  ";
            s += e.bodyState;
        }
        return s;
    }

    private void showDetail(final Store.Entry e) {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout box = Ui.col(this);
        box.setBackground(Ui.bg(Ui.CARD, 26, this));
        box.setPadding(dp(20), dp(18), dp(20), dp(18));

        LinearLayout head = Ui.row(this);
        LinearLayout hc = Ui.col(this);
        hc.addView(Ui.label(this, "ENTRY"));
        hc.addView(Ui.space(this, 5));
        hc.addView(Ui.serif(this, humanDate(e.date) + " " + weekday(e.date), 24, Ui.INK, true));
        head.addView(hc, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView close = Ui.tv(this, "×", 22, Ui.MUTED, false);
        close.setGravity(Gravity.CENTER);
        close.setBackground(Ui.bg(Ui.SOFT2, 30, this));
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        head.addView(close, new LinearLayout.LayoutParams(dp(34), dp(34)));
        box.addView(head);
        box.addView(Ui.space(this, 14));

        LinearLayout tags = Ui.row(this);
        tags.addView(miniTag(typeLabel(e.type), typeColor(e.type), typeBg(e.type)));
        if (e.durationMinutes > 0) tags.addView(miniTag(e.durationMinutes + " 分钟", Ui.DEEP, 0x0F7E3345));
        box.addView(tags);

        box.addView(Ui.space(this, 14));
        addDetailRow(box, "心情", moodOnly(e));
        addDetailRow(box, "状态", e.bodyState.isEmpty() ? "未记录" : e.bodyState);
        addDetailRow(box, "方式", methodLabel(e.method, e.methodCustom).isEmpty()
                ? "未记录" : methodLabel(e.method, e.methodCustom));
        addDetailRow(box, "使用", e.extras == null || e.extras.isEmpty() ? "未使用" : extrasSummary(e));
        String hs = "";
        for (int i = 0; i < e.pleasure; i++) hs += "♥";
        addDetailRow(box, "快感", e.pleasure == 0 ? "未记录" : hs + "  " + PLEASURE_LABEL[e.pleasure]);
        if (e.note != null && !e.note.trim().isEmpty()) {
            box.addView(Ui.space(this, 6));
            TextView label = Ui.label(this, "备注");
            box.addView(label);
            box.addView(Ui.space(this, 6));
            TextView n = Ui.tv(this, e.note.trim(), 13, Ui.INK, false);
            n.setBackground(Ui.bg(Ui.SOFT2, 14, this));
            n.setPadding(dp(12), dp(10), dp(12), dp(10));
            box.addView(n);
        }
        if (!e.media.isEmpty()) {
            box.addView(Ui.space(this, 14));
            box.addView(Ui.label(this, "媒体"));
            box.addView(Ui.space(this, 8));
            HorizontalScrollView hs2 = new HorizontalScrollView(this);
            hs2.setHorizontalScrollBarEnabled(false);
            LinearLayout mr = Ui.row(this);
            for (Store.Media m : e.media) mr.addView(mediaTile(m, false));
            hs2.addView(mr);
            box.addView(hs2, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(96)));
        }

        box.addView(Ui.space(this, 18));
        LinearLayout actions = Ui.row(this);
        TextView edit = Ui.tv(this, "编辑这条", 14, Color.WHITE, true);
        edit.setGravity(Gravity.CENTER);
        edit.setBackground(Ui.ripple(Ui.ROSE, 16, 0x33FFFFFF, this));
        edit.setClickable(true);
        edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                tab = "diary";
                openEntryEditor(e);
            }
        });
        LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(0, dp(48), 1f);
        elp.rightMargin = dp(8);
        actions.addView(edit, elp);

        TextView del = Ui.tv(this, "删除", 14, Ui.DANGER, true);
        del.setGravity(Gravity.CENTER);
        del.setBackground(Ui.strokeBg(Ui.CARD, 16, Ui.alpha(Ui.DANGER, 0x66), 1, this));
        del.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmDelete(e, dialog);
            }
        });
        actions.addView(del, new LinearLayout.LayoutParams(0, dp(48), 1f));
        box.addView(actions);

        MaxHeightScrollView sc = new MaxHeightScrollView(this);
        sc.setFillViewport(false);
        sc.addView(box);
        sc.setBackgroundColor(Color.TRANSPARENT);
        dialog.setContentView(sc);
        Window win = dialog.getWindow();
        if (win != null) {
            win.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = getResources().getDisplayMetrics().widthPixels - dp(32);
            win.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    private String moodOnly(Store.Entry e) {
        for (int i = 0; i < MOOD_IDS.length; i++) {
            if (MOOD_IDS[i].equals(e.mood)) return MOOD_EMOJI[i] + "  " + MOOD_LABEL[i];
        }
        return "未记录";
    }

    private void addDetailRow(LinearLayout parent, String key, String value) {
        LinearLayout row = Ui.row(this);
        row.setPadding(0, dp(6), 0, dp(6));
        TextView k = Ui.tv(this, key, 12, Ui.MUTED, false);
        row.addView(k, new LinearLayout.LayoutParams(dp(54),
                ViewGroup.LayoutParams.WRAP_CONTENT));
        row.addView(Ui.tv(this, value, 13, Ui.INK, false),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        parent.addView(row);
    }

    private void confirmDelete(final Store.Entry e, final Dialog detail) {
        showConfirmDialog("删除这条记录？",
                "记录里的照片、视频和录音也会一起删掉，删除后无法恢复。",
                "删除", true, new Runnable() {
                    @Override
                    public void run() {
                        store.removeById(e.id);
                        if (detail != null) detail.dismiss();
                        toast("已删除");
                        render();
                    }
                });
    }

    private void confirmDeleteCurrent() {
        if (!exists || editingId == null) {
            toast("这一次还没有保存");
            return;
        }
        final String day = date;
        final String id = editingId;
        showConfirmDialog("删除这一次记录？",
                humanDate(day) + " 的这一条记录，以及它的照片、视频和录音都会删除，且无法恢复。",
                "删除这一次", true, new Runnable() {
                    @Override
                    public void run() {
                        if (audioPlayer != null) {
                            try {
                                audioPlayer.release();
                            } catch (Exception ignored) {
                            }
                            audioPlayer = null;
                            playingAudioPath = null;
                        }
                        store.removeById(id);
                        editingId = null;
                        durationMax = 120;
                        diaryCalendar = true;
                        calendarCursor = parseDate(day);
                        calendarCursor.set(Calendar.DAY_OF_MONTH, 1);
                        loadDraft();
                        render();
                        refreshTabs();
                        toast("已删除");
                        if (!store.byDate(day).isEmpty()) showDayEntries(day);
                    }
                });
    }

    /* ==================== 设置 ==================== */

    private View buildSettings() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Ui.BG);
        scroll.setClipToPadding(false);
        LinearLayout page = Ui.col(this);
        page.setPadding(dp(18), dp(18), dp(18), dp(28));
        scroll.addView(page, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout head = Ui.col(this);
        head.addView(Ui.label(this, "SETTINGS"));
        head.addView(Ui.space(this, 7));
        head.addView(Ui.serif(this, "设置", 32, Ui.INK, true));
        head.addView(Ui.space(this, 5));
        head.addView(Ui.tv(this, "这是只属于你的空间。", 12, Ui.MUTED, false));
        page.addView(head);
        page.addView(Ui.space(this, 18));

        LinearLayout profile = Ui.card(this);
        profile.addView(sectionHeader("01", "我的档案", null));
        profile.addView(Ui.space(this, 14));
        LinearLayout who = Ui.row(this);
        TextView avatar = Ui.tv(this, genderGlyph(store.gender()), 24, Ui.ROSE, false);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(Ui.bg(Ui.alpha(Ui.ROSE, 0x24), 30, this));
        who.addView(avatar, new LinearLayout.LayoutParams(dp(52), dp(52)));
        LinearLayout whoCol = Ui.col(this);
        whoCol.setPadding(dp(14), 0, 0, 0);
        whoCol.addView(Ui.tv(this, genderName(store.gender()), 18, Ui.INK, true));
        whoCol.addView(Ui.space(this, 3));
        whoCol.addView(Ui.tv(this, "切换后只影响显示，不会改动记录。", 11, Ui.MUTED, false));
        who.addView(whoCol);
        profile.addView(who);
        profile.addView(Ui.space(this, 14));

        LinearLayout genders = Ui.row(this);
        addGenderChip(genders, "female", "女生");
        addGenderChip(genders, "male", "男生");
        addGenderChip(genders, "secret", "保密");
        profile.addView(genders);
        page.addView(profile);

        page.addView(buildThemeCard());
        page.addView(buildSecurityCard());
        page.addView(buildBackupCard());
        page.addView(buildGithubCard());

        LinearLayout data = Ui.card(this);
        data.addView(sectionHeader("06", "本地数据", null));
        data.addView(Ui.space(this, 12));
        String size = formatBytes(store.mediaBytes());
        TextView stats = Ui.tv(this, store.entries().size() + " 次记录  ·  "
                + store.countDays() + " 天  ·  " + store.mediaCount() + " 个媒体  ·  " + size,
                12, Ui.INK, false);
        data.addView(stats);
        data.addView(Ui.space(this, 6));
        data.addView(Ui.tv(this, "全部存放在应用私有目录，不会上传到任何服务器。", 11, Ui.MUTED, false));
        data.addView(Ui.space(this, 14));
        TextView clear = Ui.tv(this, "清空全部记录与媒体", 13, Ui.DANGER, true);
        clear.setGravity(Gravity.CENTER);
        clear.setBackground(Ui.strokeBg(Ui.CARD, 14, Ui.alpha(Ui.DANGER, 0x55), 1, this));
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmClearAll();
            }
        });
        data.addView(clear, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(44)));
        page.addView(data);

        LinearLayout tips = Ui.card(this);
        tips.addView(sectionHeader("07", "使用提示", null));
        tips.addView(Ui.space(this, 12));
        addTip(tips, "一天可以记多次；每次只选做爱或自慰，点日期可继续添加。");
        addTip(tips, "时间轴可以按做爱、自慰或有媒体筛选。");
        addTip(tips, "录音需要麦克风权限，只录你主动开始的声音。");
        addTip(tips, "卸载应用会删除全部记录，建议先在设置里导出备份。");
        page.addView(tips);

        LinearLayout about = Ui.card(this);
        about.addView(sectionHeader("08", "关于", null));
        about.addView(Ui.space(this, 12));
        about.addView(Ui.serif(this, "做了吗", 26, Ui.INK, true));
        about.addView(Ui.space(this, 4));
        about.addView(Ui.tv(this, "版本 1.1.1 · 原生安卓 · 本机优先", 11, Ui.MUTED, false));
        about.addView(Ui.space(this, 10));
        about.addView(Ui.tv(this, "把身体的感觉留给自己。没有账号，没有评分，也没有别人。",
                12, Ui.MUTED, false));
        page.addView(about);

        TextView footer = Ui.tv(this, "PRIVATE BY DESIGN", 9, Ui.alpha(Ui.MUTED, 0xBB), true);
        footer.setLetterSpacing(0.18f);
        footer.setGravity(Gravity.CENTER);
        page.addView(footer);
        return scroll;
    }


    private LinearLayout buildGithubCard() {
        LinearLayout card = Ui.card(this);
        card.addView(sectionHeader("05", "GitHub", null));
        card.addView(Ui.space(this, 8));
        card.addView(Ui.tv(this, "源码、版本记录和 APK 都会放在这里。检查更新只读取公开的 Release 信息。",
                11, Ui.MUTED, false));
        card.addView(Ui.space(this, 14));

        LinearLayout row = Ui.row(this);
        TextView open = settingsAction("打开仓库", false);
        open.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openUrl(Github.URL);
            }
        });
        TextView check = settingsAction("检查更新", true);
        check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkForUpdates();
            }
        });
        LinearLayout.LayoutParams a = new LinearLayout.LayoutParams(0, dp(44), 1f);
        a.rightMargin = dp(8);
        row.addView(open, a);
        row.addView(check, new LinearLayout.LayoutParams(0, dp(44), 1f));
        card.addView(row);
        card.addView(Ui.space(this, 10));
        TextView repo = Ui.tv(this, Github.URL.replace("https://", ""), 10, Ui.MUTED, false);
        repo.setSingleLine(true);
        card.addView(repo);
        return card;
    }

    private void openUrl(String url) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "没有可以打开链接的应用", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkForUpdates() {
        Toast.makeText(this, "正在检查更新…", Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                String current = "1.0.0";
                try {
                    PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
                    if (pi.versionName != null) current = pi.versionName;
                } catch (Exception ignored) {
                }

                HttpURLConnection conn = null;
                try {
                    conn = (HttpURLConnection) new URL(Github.URL + "/releases/latest").openConnection();
                    conn.setRequestMethod("GET");
                    conn.setInstanceFollowRedirects(false);
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(20000);
                    conn.setRequestProperty("User-Agent", "zuolema-android");
                    int code = conn.getResponseCode();
                    if (code == 404) {
                        toastOnUi("还没有发布版本");
                        return;
                    }
                    if (code == 403) {
                        toastOnUi("GitHub 访问受限，请稍后再试");
                        return;
                    }
                    if (code != 301 && code != 302) {
                        toastOnUi("检查失败（" + code + "）");
                        return;
                    }
                    String location = conn.getHeaderField("Location");
                    if (location == null || location.length() == 0) {
                        toastOnUi("没有找到最新版本");
                        return;
                    }
                    if (location.startsWith("/")) location = "https://github.com" + location;
                    int tagAt = location.lastIndexOf("/tag/");
                    if (tagAt < 0) {
                        toastOnUi("没有找到最新版本");
                        return;
                    }
                    final String tag = Uri.decode(location.substring(tagAt + 5));
                    final String page = location;
                    final String version = tag.startsWith("v") || tag.startsWith("V")
                            ? tag.substring(1) : tag;
                    final String download = Github.URL + "/releases/download/" + tag
                            + "/zuolema-" + version + ".apk";
                    if (isNewer(tag, current)) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showUpdateDialog(tag, "", page, download);
                            }
                        });
                    } else {
                        toastOnUi("已是最新版本 · " + current);
                    }
                } catch (Exception e) {
                    toastOnUi("检查更新失败，请检查网络");
                } finally {
                    if (conn != null) conn.disconnect();
                }
            }
        }).start();
    }

    private void toastOnUi(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isNewer(String tag, String current) {
        int[] a = versionParts(tag);
        int[] b = versionParts(current);
        int n = Math.max(a.length, b.length);
        for (int i = 0; i < n; i++) {
            int x = i < a.length ? a[i] : 0;
            int y = i < b.length ? b[i] : 0;
            if (x != y) return x > y;
        }
        return false;
    }

    private int[] versionParts(String value) {
        String v = value == null ? "" : value.trim();
        if (v.startsWith("v") || v.startsWith("V")) v = v.substring(1);
        String[] pieces = v.split("[^0-9]+");
        int count = 0;
        for (String p : pieces) if (p.length() > 0) count++;
        int[] out = new int[count];
        int j = 0;
        for (String p : pieces) {
            if (p.length() > 0) out[j++] = Integer.parseInt(p);
        }
        return out;
    }

    private void showUpdateDialog(String tag, String notes, final String page, final String download) {
        final Dialog dialog = new Dialog(this);
        LinearLayout panel = Ui.col(this);
        panel.setPadding(dp(22), dp(22), dp(22), dp(18));
        panel.setBackground(Ui.bg(Ui.CARD, 24, this));

        panel.addView(Ui.label(this, "UPDATE"));
        panel.addView(Ui.space(this, 8));
        panel.addView(Ui.serif(this, "发现新版本", 24, Ui.INK, true));
        panel.addView(Ui.space(this, 5));
        panel.addView(Ui.tv(this, tag, 12, Ui.ROSE, true));
        panel.addView(Ui.space(this, 8));
        String body = notes == null ? "" : notes.trim();
        if (body.length() > 600) body = body.substring(0, 600) + "…";
        if (body.length() > 0) {
            TextView note = Ui.tv(this, body, 12, Ui.MUTED, false);
            note.setLineSpacing(dp(3), 1f);
            panel.addView(note);
            panel.addView(Ui.space(this, 14));
        }

        LinearLayout row = Ui.row(this);
        TextView later = Ui.tv(this, "稍后", 13, Ui.MUTED, true);
        later.setGravity(Gravity.CENTER);
        later.setBackground(Ui.strokeBg(Ui.CARD, 14, Ui.LINE, 1, this));
        later.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        TextView go = Ui.tv(this, download != null && download.length() > 0 ? "下载 APK" : "打开发布页",
                13, Color.WHITE, true);
        go.setGravity(Gravity.CENTER);
        go.setBackground(Ui.ripple(Ui.ROSE, 14, 0x33FFFFFF, this));
        go.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openUrl(download != null && download.length() > 0 ? download : page);
                dialog.dismiss();
            }
        });
        LinearLayout.LayoutParams a = new LinearLayout.LayoutParams(0, dp(44), 1f);
        a.rightMargin = dp(8);
        row.addView(later, a);
        row.addView(go, new LinearLayout.LayoutParams(0, dp(44), 1f));
        panel.addView(row);

        dialog.setContentView(panel);
        Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.88f),
                    WindowManager.LayoutParams.WRAP_CONTENT);
            w.setDimAmount(0.5f);
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }
        dialog.show();
    }

    private LinearLayout buildThemeCard() {
        LinearLayout card = Ui.card(this);
        card.addView(sectionHeader("02", "外观", null));
        card.addView(Ui.space(this, 6));
        card.addView(Ui.tv(this, "深浅色会立即生效；跟随系统时随手机设置切换。深色下可单独开启纯黑背景。",
                11, Ui.MUTED, false));
        card.addView(Ui.space(this, 14));
        LinearLayout row = Ui.row(this);
        addThemeChoice(row, "system", "跟随系统", "◐");
        addThemeChoice(row, "light", "浅色", "☀");
        addThemeChoice(row, "dark", "深色", "☾");
        card.addView(row);

        if ("dark".equals(store.theme())) {
            card.addView(Ui.space(this, 12));
            LinearLayout blackRow = Ui.row(this);
            blackRow.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout blackTexts = Ui.col(this);
            blackTexts.addView(Ui.tv(this, "纯黑背景", 13, Ui.INK, true));
            blackTexts.addView(Ui.space(this, 3));
            blackTexts.addView(Ui.tv(this, "只把最底层背景压到 #000000，卡片保留暗色表面。",
                    10, Ui.MUTED, false));
            blackRow.addView(blackTexts, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            Switch blackSwitch = new Switch(this);
            blackSwitch.setChecked(store.pureBlack());
            blackSwitch.setThumbTintList(ColorStateList.valueOf(Ui.ROSE));
            blackSwitch.setTrackTintList(ColorStateList.valueOf(Ui.alpha(Ui.ROSE, 0x55)));
            blackSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    store.setPureBlack(isChecked);
                    Ui.apply(true, isChecked, store.gender(), resolveAccent());
                    rebuildForTheme();
                    toast(isChecked ? "已开启纯黑背景" : "已关闭纯黑背景");
                }
            });
            blackRow.addView(blackSwitch);
            card.addView(blackRow);
        }

        card.addView(Ui.space(this, 16));
        card.addView(Ui.label(this, "主题色"));
        card.addView(Ui.space(this, 8));
        LinearLayout accents = Ui.row(this);
        addAccentChoice(accents, "default", "跟随性别", genderColor());
        addAccentChoice(accents, "dynamic", "动态取色",
                systemAccent() == 0 ? 0xFFB9556C : systemAccent());
        addAccentChoice(accents, "custom", "自定义", store.accentColor());
        card.addView(accents);
        if (Build.VERSION.SDK_INT < 31) {
            card.addView(Ui.space(this, 6));
            card.addView(Ui.tv(this, "动态取色需要 Android 12 及以上；低版本会使用当前性别主题色。",
                    10, Ui.MUTED, false));
        }

        return card;
    }

    private int genderColor() {
        if ("male".equals(store.gender())) return 0xFF3F6F9E;
        if ("secret".equals(store.gender())) return 0xFF8A6E73;
        return 0xFFB9556C;
    }

    private void addAccentChoice(LinearLayout row, final String id, String label, int color) {
        boolean on = id.equals(store.accentMode());
        LinearLayout item = Ui.col(this);
        item.setGravity(Gravity.CENTER);
        item.setPadding(0, dp(8), 0, dp(8));
        item.setBackground(on
                ? Ui.strokeBg(Ui.alpha(Ui.ROSE, 0x22), 16, Ui.ROSE, 1.2f, this)
                : Ui.bg(Ui.SOFT2, 16, this));
        item.setClickable(true);

        View dot = new View(this);
        dot.setBackground(Ui.bg(color | 0xFF000000, 20, this));
        item.addView(dot, new LinearLayout.LayoutParams(dp(20), dp(20)));
        item.addView(Ui.space(this, 5));
        TextView text = Ui.tv(this, label, 10, on ? Ui.DEEP : Ui.INK, true);
        text.setGravity(Gravity.CENTER);
        item.addView(text);

        item.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ("custom".equals(id)) {
                    showColorPicker();
                } else {
                    applyAccent(id, 0);
                    if ("dynamic".equals(id) && systemAccent() == 0) {
                        toast("当前系统不支持动态取色，已使用默认色");
                    }
                }
            }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(64), 1f);
        lp.setMargins(dp(3), 0, dp(3), 0);
        row.addView(item, lp);
    }

    private void showColorPicker() {
        final Dialog dialog = new Dialog(this);
        LinearLayout panel = Ui.col(this);
        panel.setPadding(dp(22), dp(22), dp(22), dp(18));
        panel.setBackground(Ui.bg(Ui.CARD, 22, this));
        panel.addView(Ui.serif(this, "自定义主题色", 22, Ui.INK, true));
        panel.addView(Ui.space(this, 6));
        panel.addView(Ui.tv(this, "拖动色相、饱和度和亮度，预览满意后应用。",
                11, Ui.MUTED, false));
        panel.addView(Ui.space(this, 14));

        final int start = store.accentMode().equals("custom")
                ? store.accentColor() : genderColor();
        final float[] hsv = new float[3];
        Color.colorToHSV(start, hsv);

        final View preview = new View(this);
        preview.setBackground(Ui.bg(start, 30, this));
        panel.addView(preview, new LinearLayout.LayoutParams(dp(58), dp(58)));
        panel.addView(Ui.space(this, 12));

        final SeekBar hue = colorBar(360, Math.round(hsv[0]));
        final SeekBar sat = colorBar(100, Math.round(hsv[1] * 100));
        final SeekBar val = colorBar(100, Math.round(hsv[2] * 100));
        panel.addView(barLabel("色相"));
        panel.addView(hue);
        panel.addView(barLabel("饱和度"));
        panel.addView(sat);
        panel.addView(barLabel("亮度"));
        panel.addView(val);

        final Runnable update = new Runnable() {
            @Override
            public void run() {
                int c = Color.HSVToColor(new float[]{
                        hue.getProgress(), sat.getProgress() / 100f, val.getProgress() / 100f});
                preview.setBackground(Ui.bg(c, 30, MainActivity.this));
            }
        };
        SeekBar.OnSeekBarChangeListener l = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                update.run();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        };
        hue.setOnSeekBarChangeListener(l);
        sat.setOnSeekBarChangeListener(l);
        val.setOnSeekBarChangeListener(l);

        panel.addView(Ui.space(this, 10));
        panel.addView(Ui.label(this, "快速选择"));
        panel.addView(Ui.space(this, 6));
        LinearLayout presets = Ui.row(this);
        int[] colors = {0xFFB9556C, 0xFFD9534F, 0xFFE08A3C, 0xFFC9A227,
                0xFF5B8C5A, 0xFF2F8F83, 0xFF4A76B8, 0xFF8A5BA6};
        for (final int c : colors) {
            View sw = new View(this);
            sw.setBackground(Ui.bg(c, 20, this));
            sw.setClickable(true);
            sw.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    float[] tmp = new float[3];
                    Color.colorToHSV(c, tmp);
                    hue.setProgress(Math.round(tmp[0]));
                    sat.setProgress(Math.round(tmp[1] * 100));
                    val.setProgress(Math.round(tmp[2] * 100));
                    update.run();
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(28), dp(28));
            lp.setMargins(0, 0, dp(8), 0);
            presets.addView(sw, lp);
        }
        panel.addView(presets);

        panel.addView(Ui.space(this, 16));
        LinearLayout actions = Ui.row(this);
        TextView cancel = settingsAction("取消", false);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        TextView apply = settingsAction("应用", true);
        apply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int c = Color.HSVToColor(new float[]{
                        hue.getProgress(), sat.getProgress() / 100f, val.getProgress() / 100f});
                dialog.dismiss();
                applyAccent("custom", c);
            }
        });
        LinearLayout.LayoutParams a = new LinearLayout.LayoutParams(0, dp(44), 1f);
        a.rightMargin = dp(8);
        actions.addView(cancel, a);
        actions.addView(apply, new LinearLayout.LayoutParams(0, dp(44), 1f));
        panel.addView(actions);

        dialog.setContentView(panel);
        dialog.show();
        Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.9f),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        update.run();
    }

    private SeekBar colorBar(int max, int value) {
        SeekBar bar = new SeekBar(this);
        bar.setMax(max);
        bar.setProgress(value);
        bar.setProgressTintList(ColorStateList.valueOf(Ui.ROSE));
        bar.setThumbTintList(ColorStateList.valueOf(Ui.ROSE));
        return bar;
    }

    private TextView barLabel(String text) {
        TextView v = Ui.tv(this, text, 10, Ui.MUTED, false);
        v.setPadding(0, dp(7), 0, 0);
        return v;
    }

    private void addThemeChoice(LinearLayout row, final String id, String label, String glyph) {
        boolean on = id.equals(store.theme());
        LinearLayout item = Ui.col(this);
        item.setGravity(Gravity.CENTER);
        item.setPadding(0, dp(10), 0, dp(10));
        item.setBackground(on
                ? Ui.strokeBg(Ui.alpha(Ui.ROSE, 0x22), 16, Ui.ROSE, 1.2f, this)
                : Ui.bg(Ui.SOFT2, 16, this));
        item.setClickable(true);

        TextView icon = Ui.tv(this, glyph, 17, on ? Ui.ROSE : Ui.MUTED, false);
        icon.setGravity(Gravity.CENTER);
        item.addView(icon);
        item.addView(Ui.space(this, 3));
        TextView text = Ui.tv(this, label, 11, on ? Ui.DEEP : Ui.INK, true);
        text.setGravity(Gravity.CENTER);
        item.addView(text);

        item.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyTheme(id);
            }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(72), 1f);
        lp.setMargins(dp(3), 0, dp(3), 0);
        row.addView(item, lp);
    }

    private LinearLayout buildSecurityCard() {
        LinearLayout card = Ui.card(this);
        card.addView(sectionHeader("03", "进入密码", null));
        card.addView(Ui.space(this, 6));
        boolean has = store.hasPin();
        card.addView(Ui.tv(this, has
                        ? "已开启 · 每次打开都要输入 4 位密码。"
                        : "未开启 · 拿到手机的人可以直接看到记录。",
                11, Ui.MUTED, false));
        card.addView(Ui.space(this, 14));

        if (!has) {
            TextView set = settingsAction("设置 4 位密码", true);
            set.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showPinDialog(false, false);
                }
            });
            card.addView(set, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(44)));
        } else {
            LinearLayout row = Ui.row(this);
            TextView change = settingsAction("更改密码", false);
            change.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showPinDialog(true, false);
                }
            });
            TextView off = settingsAction("关闭密码", false);
            off.setTextColor(Ui.DANGER);
            off.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showPinDialog(true, true);
                }
            });
            LinearLayout.LayoutParams a = new LinearLayout.LayoutParams(0, dp(44), 1f);
            a.rightMargin = dp(8);
            row.addView(change, a);
            row.addView(off, new LinearLayout.LayoutParams(0, dp(44), 1f));
            card.addView(row);

            card.addView(Ui.space(this, 14));
            card.addView(Ui.divider(this));
            card.addView(Ui.space(this, 12));

            if (canUseFingerprint()) {
                LinearLayout fpRow = Ui.row(this);
                fpRow.setGravity(Gravity.CENTER_VERTICAL);
                LinearLayout texts = Ui.col(this);
                texts.addView(Ui.tv(this, "指纹解锁", 13, Ui.INK, true));
                texts.addView(Ui.space(this, 3));
                texts.addView(Ui.tv(this, "用系统已录入的指纹快速打开，密码仍可兜底。",
                        10, Ui.MUTED, false));
                fpRow.addView(texts, new LinearLayout.LayoutParams(0,
                        ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                Switch sw = new Switch(this);
                sw.setChecked(store.fingerprintEnabled());
                sw.setThumbTintList(ColorStateList.valueOf(Ui.ROSE));
                sw.setTrackTintList(ColorStateList.valueOf(Ui.alpha(Ui.ROSE, 0x55)));
                sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        store.setFingerprintEnabled(isChecked);
                        if (!isChecked) cancelFingerprint();
                        toast(isChecked ? "已开启指纹解锁" : "已关闭指纹解锁");
                    }
                });
                fpRow.addView(sw);
                card.addView(fpRow);
            } else {
                card.addView(Ui.tv(this, "此设备没有已录入的指纹，暂时只能使用密码。",
                        10, Ui.MUTED, false));
            }
        }

        card.addView(Ui.space(this, 12));
        card.addView(Ui.divider(this));
        card.addView(Ui.space(this, 8));

        if (has) {
            addSecuritySwitch(card, "5 分钟内免密",
                    "输入一次密码后，5 分钟内切回应用不再验证。",
                    store.graceEnabled(),
                    new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            store.setGraceEnabled(isChecked);
                            toast(isChecked ? "已开启 5 分钟免密" : "已关闭 5 分钟免密");
                        }
                    });
            card.addView(Ui.space(this, 10));
            card.addView(Ui.divider(this));
            card.addView(Ui.space(this, 8));
        }

        addSecuritySwitch(card, "隐藏后台",
                "最近任务和截屏中不显示日记内容。",
                store.hideRecents(),
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        store.setHideRecents(isChecked);
                        applyWindowFlags();
                        toast(isChecked ? "已隐藏后台画面" : "已恢复后台显示");
                    }
                });

        if (!has) {
            card.addView(Ui.space(this, 8));
            card.addView(Ui.tv(this, "设置密码后可开启 5 分钟免密。", 10, Ui.MUTED, false));
        }
        return card;
    }

    private void applyWindowFlags() {
        if (store == null) return;
        if (store.hideRecents()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
    }

    private void addSecuritySwitch(LinearLayout card, String title, String desc,
                                    boolean checked,
                                    CompoundButton.OnCheckedChangeListener listener) {
        LinearLayout row = Ui.row(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout texts = Ui.col(this);
        texts.addView(Ui.tv(this, title, 13, Ui.INK, true));
        texts.addView(Ui.space(this, 3));
        texts.addView(Ui.tv(this, desc, 10, Ui.MUTED, false));
        row.addView(texts, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Switch sw = new Switch(this);
        sw.setChecked(checked);
        sw.setThumbTintList(ColorStateList.valueOf(Ui.ROSE));
        sw.setTrackTintList(ColorStateList.valueOf(Ui.alpha(Ui.ROSE, 0x55)));
        sw.setOnCheckedChangeListener(listener);
        row.addView(sw);
        card.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private boolean canUseFingerprint() {
        if (Build.VERSION.SDK_INT < 23) return false;
        try {
            if (fingerprintManager == null) {
                fingerprintManager = (FingerprintManager) getSystemService(FINGERPRINT_SERVICE);
            }
            return fingerprintManager != null
                    && fingerprintManager.isHardwareDetected()
                    && fingerprintManager.hasEnrolledFingerprints();
        } catch (Exception e) {
            return false;
        }
    }

    private void startFingerprint() {
        if (!lockVisible || !store.hasPin() || !store.fingerprintEnabled()
                || !canUseFingerprint() || fingerprintRunning) return;
        try {
            cancelFingerprint();
            fingerprintSignal = new CancellationSignal();
            fingerprintRunning = true;
            fingerprintManager.authenticate(null, fingerprintSignal, 0,
                    new FingerprintManager.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationSucceeded(
                                FingerprintManager.AuthenticationResult result) {
                            fingerprintRunning = false;
                            fingerprintSignal = null;
                            if (lockVisible) unlock();
                        }

                        @Override
                        public void onAuthenticationFailed() {
                            if (pinError != null) {
                                pinError.setText("指纹未识别，再试一次");
                                pinError.setVisibility(View.VISIBLE);
                            }
                        }

                        @Override
                        public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
                            if (pinError != null) {
                                pinError.setText(helpString);
                                pinError.setVisibility(View.VISIBLE);
                            }
                        }

                        @Override
                        public void onAuthenticationError(int errorCode, CharSequence errString) {
                            fingerprintRunning = false;
                            fingerprintSignal = null;
                            if (errorCode != FingerprintManager.FINGERPRINT_ERROR_CANCELED
                                    && pinError != null) {
                                pinError.setText("指纹不可用，请输入密码");
                                pinError.setVisibility(View.VISIBLE);
                            }
                        }
                    }, new Handler(Looper.getMainLooper()));
        } catch (Exception e) {
            fingerprintRunning = false;
            fingerprintSignal = null;
        }
    }

    private void cancelFingerprint() {
        fingerprintRunning = false;
        if (fingerprintSignal != null) {
            try {
                fingerprintSignal.cancel();
            } catch (Exception ignored) {
            }
            fingerprintSignal = null;
        }
    }

    private LinearLayout buildBackupCard() {
        LinearLayout card = Ui.card(this);
        card.addView(sectionHeader("04", "备份与恢复", null));
        card.addView(Ui.space(this, 6));
        card.addView(Ui.tv(this, "导出为 zip，包含全部日记、图片、视频和录音。导入会覆盖当前数据。",
                11, Ui.MUTED, false));
        card.addView(Ui.space(this, 14));

        LinearLayout row = Ui.row(this);
        TextView export = settingsAction("导出数据", true);
        export.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportData();
            }
        });
        TextView importBtn = settingsAction("导入数据", false);
        importBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                importData();
            }
        });
        LinearLayout.LayoutParams a = new LinearLayout.LayoutParams(0, dp(44), 1f);
        a.rightMargin = dp(8);
        row.addView(export, a);
        row.addView(importBtn, new LinearLayout.LayoutParams(0, dp(44), 1f));
        card.addView(row);
        return card;
    }


    private TextView settingsAction(String text, boolean primary) {
        TextView btn = Ui.tv(this, text, 13, primary ? Color.WHITE : Ui.INK, true);
        btn.setGravity(Gravity.CENTER);
        btn.setBackground(primary
                ? Ui.ripple(Ui.ROSE, 14, 0x33FFFFFF, this)
                : Ui.strokeBg(Ui.CARD, 14, Ui.LINE, 1, this));
        return btn;
    }

    private void showPinDialog(final boolean changing, final boolean disabling) {
        final Dialog dialog = new Dialog(this);
        LinearLayout panel = Ui.col(this);
        panel.setPadding(dp(22), dp(22), dp(22), dp(18));
        panel.setBackground(Ui.bg(Ui.CARD, 22, this));

        panel.addView(Ui.serif(this, disabling ? "关闭密码" : changing ? "更改密码" : "设置密码",
                22, Ui.INK, true));
        panel.addView(Ui.space(this, 6));
        panel.addView(Ui.tv(this, disabling
                        ? "输入当前密码后关闭，之后打开应用不再验证。"
                        : "使用 4 位数字。忘记密码只能清空应用数据。",
                11, Ui.MUTED, false));
        panel.addView(Ui.space(this, 14));

        final EditText current = changing ? pinField("当前密码") : null;
        if (current != null) {
            panel.addView(current);
            panel.addView(Ui.space(this, 8));
        }
        final EditText first = disabling ? null : pinField(changing ? "新密码" : "密码");
        final EditText second = disabling ? null : pinField("再输一次");
        if (first != null) {
            panel.addView(first);
            panel.addView(Ui.space(this, 8));
            panel.addView(second);
        }

        final TextView error = Ui.tv(this, "", 11, Ui.DANGER, false);
        error.setVisibility(View.GONE);
        panel.addView(Ui.space(this, 6));
        panel.addView(error);
        panel.addView(Ui.space(this, 12));

        LinearLayout actions = Ui.row(this);
        TextView cancel = settingsAction("取消", false);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        TextView save = settingsAction(disabling ? "确认关闭" : "保存", true);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (changing && (current == null || !store.checkPin(current.getText().toString()))) {
                    error.setText("当前密码不对");
                    error.setVisibility(View.VISIBLE);
                    return;
                }
                if (disabling) {
                    store.clearPin();
                    locked = false;
                    dialog.dismiss();
                    render();
                    refreshTabs();
                    toast("已关闭进入密码");
                    return;
                }
                String a = first.getText().toString();
                String b = second.getText().toString();
                if (a.length() != 4) {
                    error.setText("请输入 4 位数字");
                    error.setVisibility(View.VISIBLE);
                    return;
                }
                if (!a.equals(b)) {
                    error.setText("两次输入不一致");
                    error.setVisibility(View.VISIBLE);
                    return;
                }
                store.setPin(a);
                dialog.dismiss();
                render();
                refreshTabs();
                toast(changing ? "密码已更新" : "密码已开启");
            }
        });
        LinearLayout.LayoutParams a = new LinearLayout.LayoutParams(0, dp(44), 1f);
        a.rightMargin = dp(8);
        actions.addView(cancel, a);
        actions.addView(save, new LinearLayout.LayoutParams(0, dp(44), 1f));
        panel.addView(actions);

        dialog.setContentView(panel);
        dialog.show();
        Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.88f),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        if (first != null) first.requestFocus();
        else if (current != null) current.requestFocus();
    }

    private EditText pinField(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setSingleLine(true);
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        e.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
        e.setTextSize(18);
        e.setTextColor(Ui.INK);
        e.setHintTextColor(Ui.MUTED);
        e.setGravity(Gravity.CENTER);
        e.setLetterSpacing(0.35f);
        e.setBackground(Ui.strokeBg(Ui.SOFT2, 12, Ui.LINE, 1, this));
        e.setPadding(dp(12), dp(9), dp(12), dp(9));
        return e;
    }

    private void addGenderChip(LinearLayout row, final String id, String label) {
        boolean on = id.equals(store.gender());
        TextView chip = Ui.pill(this, label, on);
        chip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                store.setGender(id);
                selectedGender = id;
                Ui.apply(resolveDark(store.theme()), isPureBlack(store.theme()), id, resolveAccent());
                rebuildForTheme();
                toast("已切换为" + genderName(id));
            }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(36), 1f);
        lp.rightMargin = dp(7);
        row.addView(chip, lp);
    }

    private void addTip(LinearLayout parent, String text) {
        LinearLayout row = Ui.row(this);
        row.setPadding(0, dp(4), 0, dp(4));
        TextView dot = Ui.tv(this, "·", 16, Ui.ROSE, true);
        dot.setGravity(Gravity.CENTER);
        row.addView(dot, new LinearLayout.LayoutParams(dp(16),
                ViewGroup.LayoutParams.WRAP_CONTENT));
        row.addView(Ui.tv(this, text, 12, Ui.MUTED, false),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        parent.addView(row);
    }

    private void showConfirmDialog(String title, String message, String confirmText,
                                   final boolean danger, final Runnable onConfirm) {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout card = Ui.col(this);
        card.setBackground(Ui.bg(Ui.CARD, 28, this));
        card.setPadding(dp(24), dp(22), dp(24), dp(20));

        TextView mark = Ui.serif(this, danger ? "!" : "✓", 24,
                danger ? Ui.DANGER : Ui.ROSE, true);
        mark.setGravity(Gravity.CENTER);
        mark.setBackground(Ui.bg(Ui.alpha(danger ? Ui.DANGER : Ui.ROSE, 0x1C), 28, this));
        LinearLayout.LayoutParams markLp = new LinearLayout.LayoutParams(dp(52), dp(52));
        markLp.gravity = Gravity.CENTER_HORIZONTAL;
        card.addView(mark, markLp);

        TextView eyebrow = Ui.label(this, "PLEASE CONFIRM");
        eyebrow.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams eyeLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        eyeLp.topMargin = dp(12);
        card.addView(eyebrow, eyeLp);

        TextView heading = Ui.serif(this, title, 22, Ui.INK, true);
        heading.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams headLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        headLp.topMargin = dp(8);
        card.addView(heading, headLp);

        TextView body = Ui.tv(this, message, 13, Ui.MUTED, false);
        body.setGravity(Gravity.CENTER);
        body.setLineSpacing(dp(4), 1f);
        LinearLayout.LayoutParams bodyLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bodyLp.topMargin = dp(9);
        card.addView(body, bodyLp);

        LinearLayout actions = Ui.row(this);
        LinearLayout.LayoutParams actionsLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        actionsLp.topMargin = dp(20);
        card.addView(actions, actionsLp);

        TextView cancel = Ui.tv(this, "取消", 14, Ui.INK, true);
        cancel.setGravity(Gravity.CENTER);
        cancel.setBackground(Ui.ripple(Ui.SOFT2, 22, Ui.alpha(Ui.INK, 0x22), this));
        LinearLayout.LayoutParams cancelLp = new LinearLayout.LayoutParams(0, dp(46), 1f);
        cancelLp.rightMargin = dp(8);
        actions.addView(cancel, cancelLp);

        final TextView confirm = Ui.tv(this, confirmText, 14, Color.WHITE, true);
        confirm.setGravity(Gravity.CENTER);
        confirm.setBackground(Ui.ripple(danger ? Ui.DANGER : Ui.ROSE, 22,
                0x33FFFFFF, this));
        actions.addView(confirm, new LinearLayout.LayoutParams(0, dp(46), 1f));

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                if (onConfirm != null) onConfirm.run();
            }
        });

        dialog.setContentView(card);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.5f);
        }
        dialog.show();
        if (window != null) {
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.88f);
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void exportData() {
        suppressLock = true;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        String stamp = new SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(new Date());
        intent.putExtra(Intent.EXTRA_TITLE, "做了吗-备份-" + stamp + ".zip");
        try {
            startActivityForResult(intent, REQ_EXPORT);
        } catch (Exception e) {
            suppressLock = false;
            toast("没有可用的文件保存器");
        }
    }

    private void importData() {
        suppressLock = true;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        intent.putExtra(Intent.EXTRA_MIME_TYPES,
                new String[]{"application/zip", "application/octet-stream"});
        try {
            startActivityForResult(intent, REQ_IMPORT);
        } catch (Exception e) {
            suppressLock = false;
            toast("没有可用的文件选择器");
        }
    }

    private void runExport(final Uri uri) {
        toast("正在导出…");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    OutputStream os = getContentResolver().openOutputStream(uri, "w");
                    if (os == null) throw new Exception("无法写入文件");
                    store.exportZip(os);
                    os.close();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toast("已导出备份");
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toast("导出失败：" + safeMessage(e));
                        }
                    });
                }
            }
        }).start();
    }

    private void confirmImport(final Uri uri) {
        showConfirmDialog("导入并覆盖？",
                "当前的日记、照片、视频和录音会被备份里的内容替换。",
                "覆盖导入", false, new Runnable() {
                    @Override
                    public void run() {
                        runImport(uri);
                    }
                });
    }

    private void runImport(final Uri uri) {
        toast("正在导入…");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    InputStream in = getContentResolver().openInputStream(uri);
                    if (in == null) throw new Exception("无法读取文件");
                    final int n = store.importZip(in);
                    in.close();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (audioPlayer != null) {
                                try {
                                    audioPlayer.release();
                                } catch (Exception ignored) {
                                }
                                audioPlayer = null;
                                playingAudioPath = null;
                            }
                            date = Store.today();
                            tab = "diary";
                            diaryCalendar = true;
                            loadDraft();
                            render();
                            refreshTabs();
                            toast(n == 0 ? "备份是空的，已清空" : "已导入 " + n + " 条记录");
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toast("导入失败：" + safeMessage(e));
                        }
                    });
                }
            }
        }).start();
    }

    private String safeMessage(Exception e) {
        String m = e.getMessage();
        return m == null || m.length() == 0 ? "文件无法读取" : m;
    }

    private void confirmClearAll() {
        showConfirmDialog("清空全部记录？",
                "所有日记、照片、视频和录音都会被删除，且无法恢复。",
                "全部清空", true, new Runnable() {
                    @Override
                    public void run() {
                        if (audioPlayer != null) {
                            try {
                                audioPlayer.release();
                            } catch (Exception ignored) {
                            }
                            audioPlayer = null;
                            playingAudioPath = null;
                        }
                        store.clearAll();
                        date = Store.today();
                        loadDraft();
                        toast("已清空");
                        render();
                    }
                });
    }

    private String genderName(String id) {
        if ("female".equals(id)) return "女生";
        if ("male".equals(id)) return "男生";
        return "保密";
    }

    private String genderGlyph(String id) {
        if ("female".equals(id)) return "♀";
        if ("male".equals(id)) return "♂";
        return "✦";
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.US, "%.1f KB", bytes / 1024f);
        if (bytes < 1024L * 1024 * 1024) {
            return String.format(Locale.US, "%.1f MB", bytes / 1024f / 1024f);
        }
        return String.format(Locale.US, "%.2f GB", bytes / 1024f / 1024f / 1024f);
    }

    /* ==================== 媒体 ==================== */

    private void refreshMediaRow() {
        if (mediaRow == null) return;
        mediaRow.removeAllViews();
        if (media.isEmpty()) {
            TextView hint = Ui.tv(this, "还没有照片、视频或录音", 11, Ui.MUTED, false);
            hint.setGravity(Gravity.CENTER_VERTICAL);
            mediaRow.addView(hint, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, dp(92)));
            return;
        }
        for (Store.Media m : media) {
            mediaRow.addView(mediaTile(m, true));
        }
    }

    private View mediaTile(final Store.Media m, boolean deletable) {
        final FrameLayout tile = new FrameLayout(this);
        tile.setBackground(Ui.bg(Ui.SOFT, 16, this));
        tile.setClipToOutline(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(92), dp(92));
        lp.rightMargin = dp(9);
        tile.setLayoutParams(lp);
        tile.setClickable(true);

        if ("image".equals(m.kind) || "video".equals(m.kind)) {
            ImageView iv = new ImageView(this);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Bitmap bm = "image".equals(m.kind) ? decodeSampled(m.path, dp(220), dp(220))
                    : videoThumb(m.path);
            if (bm != null) iv.setImageBitmap(bm);
            else iv.setBackgroundColor("video".equals(m.kind) ? Ui.DEEP : Ui.SOFT);
            tile.addView(iv, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            if ("video".equals(m.kind)) {
                TextView play = Ui.tv(this, "▶", 24, Color.WHITE, false);
                play.setGravity(Gravity.CENTER);
                play.setShadowLayer(dp(4), 0, dp(1), 0x88000000);
                tile.addView(play, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                TextView badge = Ui.tv(this, "视频", 9, Color.WHITE, false);
                badge.setGravity(Gravity.CENTER);
                badge.setBackgroundColor(0x77000000);
                FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(18));
                blp.gravity = Gravity.BOTTOM;
                tile.addView(badge, blp);
            }
        } else {
            LinearLayout audio = Ui.col(this);
            audio.setGravity(Gravity.CENTER);
            boolean playing = m.path != null && m.path.equals(playingAudioPath);
            TextView glyph = Ui.tv(this, playing ? "■" : "♫", 28, Ui.ROSE, false);
            glyph.setGravity(Gravity.CENTER);
            TextView label = Ui.tv(this, playing ? "播放中" : "录音", 9, Ui.MUTED, false);
            label.setGravity(Gravity.CENTER);
            audio.addView(glyph);
            audio.addView(Ui.space(this, 3));
            audio.addView(label);
            tile.addView(audio, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }

        if (deletable) {
            TextView x = Ui.tv(this, "×", 15, Color.WHITE, false);
            x.setGravity(Gravity.CENTER);
            x.setBackground(Ui.bg(0x99000000, 30, this));
            FrameLayout.LayoutParams xlp = new FrameLayout.LayoutParams(dp(22), dp(22));
            xlp.gravity = Gravity.TOP | Gravity.END;
            xlp.topMargin = dp(4);
            xlp.rightMargin = dp(4);
            tile.addView(x, xlp);
            x.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    media.remove(m);
                    markDirty();
                    refreshMediaRow();
                }
            });
        }

        tile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ("image".equals(m.kind)) showImage(m.path);
                else if ("video".equals(m.kind)) showVideo(m.path);
                else toggleAudio(m.path);
            }
        });
        return tile;
    }

    private Bitmap decodeSampled(String path, int reqW, int reqH) {
        if (path == null || !new File(path).exists()) return null;
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, bounds);
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;
            int sample = 1;
            while (bounds.outWidth / (sample * 2) >= reqW
                    && bounds.outHeight / (sample * 2) >= reqH) {
                sample *= 2;
            }
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sample;
            opts.inPreferredConfig = Bitmap.Config.RGB_565;
            return BitmapFactory.decodeFile(path, opts);
        } catch (Exception e) {
            return null;
        }
    }

    private final java.util.HashMap<String, Bitmap> videoThumbs = new java.util.HashMap<>();

    private Bitmap videoThumb(String path) {
        if (videoThumbs.containsKey(path)) return videoThumbs.get(path);
        if (path == null || !new File(path).exists()) return null;
        MediaMetadataRetriever r = new MediaMetadataRetriever();
        Bitmap bm = null;
        try {
            r.setDataSource(path);
            bm = r.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
        } catch (Exception ignored) {
        } finally {
            try {
                r.release();
            } catch (Exception ignored) {
            }
        }
        if (bm != null) videoThumbs.put(path, bm);
        return bm;
    }

    private void pickImages() {
        if (media.size() >= 9) {
            toast("最多添加 9 个媒体");
            return;
        }
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        // 选图期间不要把编辑页锁掉，否则返回时会被锁屏并丢掉刚选的文件。
        suppressLock = true;
        try {
            startActivityForResult(intent, REQ_IMAGES);
        } catch (Exception e) {
            suppressLock = false;
            toast("没有可用的图片选择器");
        }
    }

    private void pickVideo() {
        if (media.size() >= 9) {
            toast("最多添加 9 个媒体");
            return;
        }
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("video/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        suppressLock = true;
        try {
            startActivityForResult(intent, REQ_VIDEO);
        } catch (Exception e) {
            suppressLock = false;
            toast("没有可用的视频选择器");
        }
    }

    private void takeReadPermission(Uri uri) {
        if (uri == null) return;
        try {
            getContentResolver().takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_EXPORT || requestCode == REQ_IMPORT) {
            suppressLock = false;
        }
        if (requestCode == REQ_IMAGES || requestCode == REQ_VIDEO) {
            // 等 Activity 真正回到前台后再恢复自动锁，避免 onStart 抢先弹密码。
            content.postDelayed(new Runnable() {
                @Override
                public void run() {
                    suppressLock = false;
                }
            }, 500);
        }
        if (resultCode != RESULT_OK || data == null) return;
        if (requestCode == REQ_EXPORT) {
            if (data.getData() != null) runExport(data.getData());
            return;
        }
        if (requestCode == REQ_IMPORT) {
            if (data.getData() != null) confirmImport(data.getData());
            return;
        }
        if (requestCode == REQ_IMAGES) {
            int added = 0;
            int failed = 0;
            ClipData clip = data.getClipData();
            if (clip != null) {
                for (int i = 0; i < clip.getItemCount() && media.size() < 9; i++) {
                    Uri uri = clip.getItemAt(i).getUri();
                    takeReadPermission(uri);
                    String path = store.importMedia(uri, "image");
                    if (path != null) {
                        media.add(new Store.Media("image", path));
                        added++;
                    } else {
                        failed++;
                    }
                }
            } else if (data.getData() != null) {
                takeReadPermission(data.getData());
                String path = store.importMedia(data.getData(), "image");
                if (path != null) {
                    media.add(new Store.Media("image", path));
                    added++;
                } else {
                    failed++;
                }
            }
            if (added > 0) {
                markDirty();
                refreshMediaRow();
                toast(failed > 0 ? "已添加 " + added + " 张，失败 " + failed + " 张"
                        : "已添加 " + added + " 张照片");
            } else {
                toast("照片导入失败，请换一张或检查相册权限");
            }
        } else if (requestCode == REQ_VIDEO) {
            if (data.getData() == null) return;
            takeReadPermission(data.getData());
            String path = store.importMedia(data.getData(), "video");
            if (path != null) {
                media.add(new Store.Media("video", path));
                markDirty();
                refreshMediaRow();
                toast("已添加视频");
            } else {
                toast("视频导入失败，请换一个或检查相册权限");
            }
        }
    }

    private void toggleAudio(String path) {
        if (audioPlayer != null) {
            boolean same = path != null && path.equals(playingAudioPath);
            try {
                audioPlayer.stop();
            } catch (Exception ignored) {
            }
            audioPlayer.release();
            audioPlayer = null;
            playingAudioPath = null;
            if (same) {
                refreshMediaRow();
                return;
            }
        }
        if (path == null || !new File(path).exists()) {
            toast("找不到这段录音");
            return;
        }
        try {
            MediaPlayer p = new MediaPlayer();
            p.setDataSource(path);
            p.prepare();
            p.start();
            audioPlayer = p;
            playingAudioPath = path;
            p.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    playingAudioPath = null;
                    refreshMediaRow();
                }
            });
            p.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    playingAudioPath = null;
                    refreshMediaRow();
                    return false;
                }
            });
            refreshMediaRow();
        } catch (Exception e) {
            toast("无法播放这段录音");
        }
    }

    private void showImage(String path) {
        Bitmap bm = decodeSampled(path, getResources().getDisplayMetrics().widthPixels,
                getResources().getDisplayMetrics().heightPixels);
        if (bm == null) {
            toast("图片加载失败");
            return;
        }
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        FrameLayout frame = new FrameLayout(this);
        frame.setBackgroundColor(0xF2000000);
        ImageView iv = new ImageView(this);
        iv.setImageBitmap(bm);
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        frame.addView(iv, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        TextView close = Ui.tv(this, "×", 24, Color.WHITE, false);
        close.setGravity(Gravity.CENTER);
        close.setBackground(Ui.bg(0x66000000, 30, this));
        FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams(dp(38), dp(38));
        clp.gravity = Gravity.TOP | Gravity.END;
        clp.topMargin = dp(28);
        clp.rightMargin = dp(20);
        frame.addView(close, clp);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.setContentView(frame);
        Window win = dialog.getWindow();
        if (win != null) {
            win.setBackgroundDrawable(new ColorDrawable(Color.BLACK));
            win.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
        dialog.show();
    }

    private void showVideo(String path) {
        if (path == null || !new File(path).exists()) {
            toast("找不到这个视频");
            return;
        }
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout box = Ui.col(this);
        box.setBackground(Ui.bg(Ui.CARD, 22, this));
        box.setPadding(dp(12), dp(12), dp(12), dp(14));

        LinearLayout head = Ui.row(this);
        head.setPadding(dp(4), 0, dp(4), dp(10));
        head.addView(Ui.tv(this, "视频", 15, Ui.INK, true),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView close = Ui.tv(this, "×", 21, Ui.MUTED, false);
        close.setGravity(Gravity.CENTER);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        head.addView(close, new LinearLayout.LayoutParams(dp(30), dp(30)));
        box.addView(head);

        final VideoView vv = new VideoView(this);
        MediaController mc = new MediaController(this);
        vv.setMediaController(mc);
        mc.setAnchorView(vv);
        box.addView(vv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(230)));
        vv.setVideoPath(path);
        vv.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                vv.start();
            }
        });
        dialog.setContentView(box);
        Window win = dialog.getWindow();
        if (win != null) {
            win.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = getResources().getDisplayMetrics().widthPixels - dp(32);
            win.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface d) {
                try {
                    vv.stopPlayback();
                } catch (Exception ignored) {
                }
            }
        });
        dialog.show();
    }

    /* ==================== 录音 ==================== */

    private void requestAudio() {
        if (media.size() >= 9) {
            toast("最多添加 9 个媒体");
            return;
        }
        if (Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO_PERM);
            return;
        }
        showRecordDialog();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == REQ_AUDIO_PERM) {
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
                showRecordDialog();
            } else {
                toast("没有麦克风权限，无法录音");
            }
        }
    }

    private void showRecordDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(false);
        final LinearLayout box = Ui.col(this);
        box.setBackground(Ui.bg(Ui.CARD, 26, this));
        box.setPadding(dp(22), dp(20), dp(22), dp(18));
        box.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title = Ui.serif(this, "录一段声音", 22, Ui.INK, true);
        title.setGravity(Gravity.CENTER);
        box.addView(title);
        box.addView(Ui.space(this, 6));
        TextView sub = Ui.tv(this, "最长 10 分钟，只保存在本机。", 11, Ui.MUTED, false);
        sub.setGravity(Gravity.CENTER);
        box.addView(sub);
        box.addView(Ui.space(this, 20));

        recordTimer = Ui.tv(this, "00:00", 36, Ui.DEEP, true);
        recordTimer.setGravity(Gravity.CENTER);
        recordTimer.setTypeface(android.graphics.Typeface.create("sans-serif-light",
                android.graphics.Typeface.NORMAL));
        box.addView(recordTimer);
        box.addView(Ui.space(this, 6));
        recordHint = Ui.tv(this, "准备好后点开始", 11, Ui.MUTED, false);
        recordHint.setGravity(Gravity.CENTER);
        box.addView(recordHint);
        box.addView(Ui.space(this, 20));

        recordAction = Ui.tv(this, "开始录音", 15, Color.WHITE, true);
        recordAction.setGravity(Gravity.CENTER);
        recordAction.setBackground(Ui.ripple(Ui.ROSE, 18, 0x33FFFFFF, this));
        recordAction.setClickable(true);
        box.addView(recordAction, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        box.addView(Ui.space(this, 10));

        TextView cancel = Ui.tv(this, "取消", 12, Ui.MUTED, false);
        cancel.setGravity(Gravity.CENTER);
        cancel.setPadding(0, dp(8), 0, dp(8));
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRecording(false, dialog);
            }
        });
        box.addView(cancel);

        recordAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (recording) stopRecording(true, dialog);
                else startRecording();
            }
        });

        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface d) {
                stopRecording(false, dialog);
            }
        });
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface d) {
                if (recording) stopRecording(false, dialog);
                recordDialog = null;
                recordTimer = null;
                recordAction = null;
                recordHint = null;
            }
        });

        recordDialog = dialog;
        dialog.setContentView(box);
        Window win = dialog.getWindow();
        if (win != null) {
            win.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = getResources().getDisplayMetrics().widthPixels - dp(48);
            win.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    private void startRecording() {
        recordFile = store.newAudioFile();
        recorder = new MediaRecorder();
        try {
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setAudioSamplingRate(44100);
            recorder.setAudioEncodingBitRate(96000);
            recorder.setOutputFile(recordFile.getAbsolutePath());
            recorder.prepare();
            recorder.start();
            recording = true;
            recordStartedAt = android.os.SystemClock.elapsedRealtime();
            if (recordAction != null) {
                recordAction.setText("停止并保存");
                recordAction.setBackground(Ui.ripple(Ui.DEEP, 18, 0x33FFFFFF, this));
            }
            if (recordHint != null) {
                recordHint.setText("正在录音…");
                recordHint.setTextColor(Ui.ROSE);
            }
            startRecordTimer();
        } catch (Exception e) {
            recording = false;
            try {
                recorder.release();
            } catch (Exception ignored) {
            }
            recorder = null;
            if (recordFile != null) recordFile.delete();
            recordFile = null;
            toast("无法开始录音：" + e.getClass().getSimpleName());
        }
    }

    private void startRecordTimer() {
        recordTick = new Runnable() {
            @Override
            public void run() {
                if (!recording) return;
                long seconds = (android.os.SystemClock.elapsedRealtime() - recordStartedAt) / 1000;
                if (recordTimer != null) {
                    recordTimer.setText(String.format(Locale.US, "%02d:%02d",
                            seconds / 60, seconds % 60));
                }
                if (seconds >= 600) {
                    stopRecording(true, recordDialog);
                    return;
                }
                recordHandler.postDelayed(this, 500);
            }
        };
        recordHandler.post(recordTick);
    }

    private void stopRecording(boolean save, Dialog dialog) {
        if (!recording) {
            if (dialog != null && dialog.isShowing()) dialog.dismiss();
            return;
        }
        recording = false;
        if (recordTick != null) {
            recordHandler.removeCallbacks(recordTick);
            recordTick = null;
        }
        boolean ok = true;
        try {
            recorder.stop();
        } catch (Exception e) {
            ok = false;
        }
        try {
            recorder.release();
        } catch (Exception ignored) {
        }
        recorder = null;
        File f = recordFile;
        recordFile = null;
        if (save && ok && f != null && f.length() > 0) {
            media.add(new Store.Media("audio", f.getAbsolutePath()));
            markDirty();
            refreshMediaRow();
            toast("录音已添加");
        } else {
            if (f != null) f.delete();
            if (save) toast("录音太短，没有保存");
        }
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
    }

    /* ==================== 生命周期与工具 ==================== */

    @Override
    protected void onStart() {
        super.onStart();
        if (store != null && store.hasPin() && !lockVisible && !suppressLock) {
            if (store.withinGrace()) {
                locked = false;
            } else {
                showLock();
            }
        } else if (store != null && lockVisible && store.fingerprintEnabled()
                && canUseFingerprint()) {
            content.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (lockVisible) startFingerprint();
                }
            }, 350);
        }
    }

    @Override
    protected void onStop() {
        cancelFingerprint();
        if (store != null && store.hasPin() && !suppressLock && !isFinishing()) {
            if ("diary".equals(tab) && !diaryCalendar) commitDraft(false);
            locked = true;
        }
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (recording) {
            stopRecording(false, recordDialog);
        }
        if (audioPlayer != null) {
            try {
                audioPlayer.stop();
            } catch (Exception ignored) {
            }
            try {
                audioPlayer.release();
            } catch (Exception ignored) {
            }
            audioPlayer = null;
            playingAudioPath = null;
            refreshMediaRow();
        }
        if (store != null && "diary".equals(tab) && !diaryCalendar
                && bottomBar.getVisibility() == View.VISIBLE) {
            commitDraft(false);
        }
    }

    @Override
    public void onBackPressed() {
        if (locked || lockVisible) {
            moveTaskToBack(true);
            return;
        }
        if (bottomBar.getVisibility() == View.VISIBLE) {
            if ("diary".equals(tab) && !diaryCalendar) {
                closeEditor();
                return;
            }
            if (!"diary".equals(tab)) {
                switchTab("diary");
                return;
            }
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        recordHandler.removeCallbacksAndMessages(null);
        if (recording) {
            try {
                recorder.stop();
            } catch (Exception ignored) {
            }
            try {
                recorder.release();
            } catch (Exception ignored) {
            }
            recording = false;
        }
        if (audioPlayer != null) {
            try {
                audioPlayer.release();
            } catch (Exception ignored) {
            }
        }
    }

    private int dp(float v) {
        return Ui.dp(this, v);
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    /** 详情弹窗用，内容过长时限制高度并可滚动。 */
    private static class MaxHeightScrollView extends ScrollView {
        public MaxHeightScrollView(android.content.Context context) {
            super(context);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int cap = (int) (getResources().getDisplayMetrics().heightPixels * 0.82f);
            int h = MeasureSpec.makeMeasureSpec(cap, MeasureSpec.AT_MOST);
            super.onMeasure(widthMeasureSpec, h);
        }
    }

    /** 底部导航使用手绘小图标，避免依赖图标库。 */
    private static class TabIconView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int type;
        private final float density;

        public TabIconView(android.content.Context context, int type) {
            super(context);
            this.type = type;
            this.density = context.getResources().getDisplayMetrics().density;
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1.7f * density);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setColor(Ui.MUTED);
        }

        public void setColor(int color) {
            paint.setColor(color);
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth();
            float h = getHeight();
            float p = 4f * density;
            if (type == 0) {
                canvas.drawRoundRect(new RectF(p, p * 0.7f, w - p, h - p * 0.7f),
                        3f * density, 3f * density, paint);
                float x1 = p + 4f * density;
                float x2 = w - p - 4f * density;
                canvas.drawLine(x1, h * 0.42f, x2, h * 0.42f, paint);
                canvas.drawLine(x1, h * 0.62f, x2, h * 0.62f, paint);
                canvas.drawLine(x1, h * 0.82f, x2 - 4f * density, h * 0.82f, paint);
            } else if (type == 1) {
                canvas.drawLine(w / 2f, p, w / 2f, h - p, paint);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(w / 2f, h * 0.25f, 2.4f * density, paint);
                canvas.drawCircle(w / 2f, h * 0.55f, 2.4f * density, paint);
                canvas.drawCircle(w / 2f, h * 0.85f, 2.4f * density, paint);
                paint.setStyle(Paint.Style.STROKE);
            } else {
                float left = p;
                float right = w - p;
                float[] ys = {h * 0.25f, h * 0.5f, h * 0.75f};
                float[] knobs = {w * 0.62f, w * 0.38f, w * 0.7f};
                for (int i = 0; i < 3; i++) {
                    canvas.drawLine(left, ys[i], right, ys[i], paint);
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawCircle(knobs[i], ys[i], 2.3f * density, paint);
                    paint.setStyle(Paint.Style.STROKE);
                }
            }
        }
    }
}
