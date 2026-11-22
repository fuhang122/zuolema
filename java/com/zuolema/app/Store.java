package com.zuolema.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/** 本地存储：一天可以有多条记录，媒体文件放在 filesDir/media。 */
public class Store {

    public static class Media {
        public String kind;
        public String path;

        public Media(String kind, String path) {
            this.kind = kind;
            this.path = path;
        }

        public JSONObject toJson() {
            JSONObject o = new JSONObject();
            try {
                o.put("kind", kind);
                o.put("path", path);
            } catch (Exception ignored) {
            }
            return o;
        }

        public static Media fromJson(JSONObject o) {
            return new Media(o.optString("kind", "image"), o.optString("path", ""));
        }
    }

    public static class Entry {
        public String id;
        public String date;
        /** sex / mast / 空（只记了心情等其他内容） */
        public String type = "";
        public int durationMinutes = 15;
        public String mood = "";
        public String bodyState = "";
        public int pleasure = 0;
        public String note = "";
        /** 本次用到的道具/成人内容等标签，可多选。 */
        public List<String> extras = new ArrayList<>();
        public List<Media> media = new ArrayList<>();
        public long createdAt;
        public long updatedAt;

        public boolean hasActivity() {
            return "sex".equals(type) || "mast".equals(type);
        }

        public boolean isEmpty() {
            return type.isEmpty() && durationMinutes == 15
                    && mood.isEmpty() && bodyState.isEmpty() && pleasure == 0
                    && note.trim().isEmpty() && extras.isEmpty() && media.isEmpty();
        }

        public Entry copy() {
            Entry e = new Entry();
            e.id = null;
            e.date = date;
            e.type = type;
            e.durationMinutes = durationMinutes;
            e.mood = mood;
            e.bodyState = bodyState;
            e.pleasure = pleasure;
            e.note = note;
            e.extras = new ArrayList<>(extras);
            e.media = new ArrayList<>(media);
            e.createdAt = createdAt;
            e.updatedAt = updatedAt;
            return e;
        }

        public JSONObject toJson() {
            JSONObject o = new JSONObject();
            try {
                o.put("id", id);
                o.put("date", date);
                o.put("type", type);
                // 兼容旧版本字段，方便回退安装。
                o.put("hadSex", "sex".equals(type));
                o.put("hadMasturbation", "mast".equals(type));
                o.put("durationMinutes", durationMinutes);
                o.put("mood", mood);
                o.put("bodyState", bodyState);
                o.put("pleasure", pleasure);
                o.put("note", note);
                JSONArray extraArr = new JSONArray();
                for (String extra : extras) extraArr.put(extra);
                o.put("extras", extraArr);
                o.put("createdAt", createdAt);
                o.put("updatedAt", updatedAt);
                JSONArray arr = new JSONArray();
                for (Media m : media) arr.put(m.toJson());
                o.put("media", arr);
            } catch (Exception ignored) {
            }
            return o;
        }

        public static Entry fromJson(JSONObject o) {
            Entry e = new Entry();
            e.id = o.optString("id", "");
            e.date = o.optString("date", "");
            e.type = o.optString("type", "");
            if (e.type.isEmpty()) {
                boolean sex = o.optBoolean("hadSex", false);
                boolean mast = o.optBoolean("hadMasturbation", false);
                e.type = sex ? "sex" : (mast ? "mast" : "");
            }
            e.durationMinutes = o.optInt("durationMinutes", 15);
            e.mood = o.optString("mood", "");
            e.bodyState = o.optString("bodyState", "");
            e.pleasure = o.optInt("pleasure", 0);
            e.note = o.optString("note", "");
            JSONArray extraArr = o.optJSONArray("extras");
            if (extraArr != null) {
                for (int i = 0; i < extraArr.length(); i++) {
                    String extra = extraArr.optString(i, "");
                    if (!extra.isEmpty()) e.extras.add(extra);
                }
            }
            e.createdAt = o.optLong("createdAt", 0);
            e.updatedAt = o.optLong("updatedAt", 0);
            if (e.createdAt == 0) e.createdAt = e.updatedAt;
            JSONArray arr = o.optJSONArray("media");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject m = arr.optJSONObject(i);
                    if (m != null) e.media.add(Media.fromJson(m));
                }
            }
            return e;
        }
    }

    private final Context ctx;
    private final File dataFile;
    private final File mediaDir;
    private final SharedPreferences prefs;
    private final List<Entry> entries = new ArrayList<>();
    private boolean loadedOk = false;

    public Store(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        this.dataFile = new File(this.ctx.getFilesDir(), "entries.json");
        this.mediaDir = new File(this.ctx.getFilesDir(), "media");
        this.prefs = this.ctx.getSharedPreferences("profile", Context.MODE_PRIVATE);
        // 兼容旧版：独立“纯黑”主题迁移为“深色 + 纯黑背景”开关。
        if ("black".equals(prefs.getString("theme", ""))) {
            prefs.edit().putString("theme", "dark").putBoolean("pure_black", true).apply();
        }
        if (!mediaDir.exists()) mediaDir.mkdirs();
        load();
        if (loadedOk) cleanupOrphans();
    }

    public File mediaDir() {
        if (!mediaDir.exists()) mediaDir.mkdirs();
        return mediaDir;
    }

    public String gender() {
        return prefs.getString("gender", null);
    }

    public void setGender(String g) {
        prefs.edit().putString("gender", g).apply();
    }

    public String theme() {
        return prefs.getString("theme", "system");
    }

    public void setTheme(String mode) {
        if (!"dark".equals(mode)) {
            prefs.edit().putString("theme", mode).putBoolean("pure_black", false).apply();
        } else {
            prefs.edit().putString("theme", mode).apply();
        }
    }

    public boolean pureBlack() {
        return prefs.getBoolean("pure_black", false);
    }

    public void setPureBlack(boolean on) {
        prefs.edit().putBoolean("pure_black", on).apply();
    }

    /** 旧版本的云端配置已移除；升级时清掉残留地址和凭据。 */
    public void purgeCloudConfig() {
        prefs.edit()
                .remove("webdav_provider")
                .remove("webdav_url")
                .remove("webdav_user")
                .remove("webdav_pass")
                .remove("webdav_realtime")
                .remove("last_cloud_sync")
                .apply();
    }

    public String accentMode() {
        return prefs.getString("accent_mode", "default");
    }

    public void setAccentMode(String mode) {
        prefs.edit().putString("accent_mode", mode).apply();
    }

    public int accentColor() {
        return prefs.getInt("accent_color", 0xFFB9556C);
    }

    public void setAccentColor(int color) {
        prefs.edit().putInt("accent_color", color).apply();
    }

    public boolean fingerprintEnabled() {
        return prefs.getBoolean("fingerprint_enabled", true);
    }

    public void setFingerprintEnabled(boolean on) {
        prefs.edit().putBoolean("fingerprint_enabled", on).apply();
    }

    /** 输入一次密码后，5 分钟内切回应用不再验证。 */
    public boolean graceEnabled() {
        return prefs.getBoolean("pin_grace", false);
    }

    public void setGraceEnabled(boolean on) {
        SharedPreferences.Editor e = prefs.edit().putBoolean("pin_grace", on);
        if (on) {
            e.putLong("pin_unlock_at", System.currentTimeMillis());
        } else {
            e.remove("pin_unlock_at");
        }
        e.apply();
    }

    public void markUnlocked() {
        prefs.edit().putLong("pin_unlock_at", System.currentTimeMillis()).apply();
    }

    public boolean withinGrace() {
        if (!graceEnabled()) return false;
        long at = prefs.getLong("pin_unlock_at", 0L);
        if (at <= 0L) return false;
        long elapsed = System.currentTimeMillis() - at;
        return elapsed >= 0L && elapsed < 5L * 60L * 1000L;
    }

    /** 最近任务/截图中隐藏应用画面。 */
    public boolean hideRecents() {
        return prefs.getBoolean("hide_recents", false);
    }

    public void setHideRecents(boolean on) {
        prefs.edit().putBoolean("hide_recents", on).apply();
    }

    public boolean hasPin() {
        String hash = prefs.getString("pin_hash", null);
        String salt = prefs.getString("pin_salt", null);
        return hash != null && salt != null;
    }

    public void setPin(String pin) {
        try {
            byte[] raw = new byte[16];
            new SecureRandom().nextBytes(raw);
            String salt = Base64.encodeToString(raw, Base64.NO_WRAP);
            prefs.edit()
                    .putString("pin_salt", salt)
                    .putString("pin_hash", pinHash(pin, salt))
                    .putLong("pin_unlock_at", System.currentTimeMillis())
                    .apply();
        } catch (Exception ignored) {
        }
    }

    public boolean checkPin(String pin) {
        String salt = prefs.getString("pin_salt", null);
        String hash = prefs.getString("pin_hash", null);
        if (salt == null || hash == null || pin == null) return false;
        return hash.equals(pinHash(pin, salt));
    }

    public void clearPin() {
        prefs.edit().remove("pin_salt").remove("pin_hash")
                .remove("pin_unlock_at").apply();
    }

    private static String pinHash(String pin, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update((salt + ":" + pin).getBytes("UTF-8"));
            byte[] out = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : out) sb.append(String.format(Locale.US, "%02x", b & 0xFF));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public List<Entry> entries() {
        return entries;
    }

    public List<Entry> byDate(String date) {
        List<Entry> out = new ArrayList<>();
        for (Entry e : entries) {
            if (date.equals(e.date)) out.add(e);
        }
        return out;
    }

    public Entry byId(String id) {
        if (id == null) return null;
        for (Entry e : entries) {
            if (id.equals(e.id)) return e;
        }
        return null;
    }

    /** 兼容旧调用：返回当天第一条记录。 */
    public Entry get(String date) {
        List<Entry> list = byDate(date);
        return list.isEmpty() ? null : list.get(0);
    }

    public void put(Entry e) {
        if (e.id == null || e.id.isEmpty()) e.id = UUID.randomUUID().toString();
        if (e.createdAt == 0) e.createdAt = System.currentTimeMillis();
        e.updatedAt = System.currentTimeMillis();
        int index = indexOfId(e.id);
        if (index >= 0) entries.set(index, e);
        else entries.add(e);
        sort();
        save();
        cleanupOrphans();
    }

    public void removeById(String id) {
        if (id == null) return;
        int index = indexOfId(id);
        if (index >= 0) {
            entries.remove(index);
            save();
            cleanupOrphans();
        }
    }

    public void remove(String date) {
        boolean changed = false;
        for (int i = entries.size() - 1; i >= 0; i--) {
            if (entries.get(i).date.equals(date)) {
                entries.remove(i);
                changed = true;
            }
        }
        if (changed) {
            save();
            cleanupOrphans();
        }
    }

    private int indexOfId(String id) {
        for (int i = 0; i < entries.size(); i++) {
            if (id.equals(entries.get(i).id)) return i;
        }
        return -1;
    }

    public int countDays() {
        Set<String> days = new HashSet<>();
        for (Entry e : entries) if (e.date != null) days.add(e.date);
        return days.size();
    }

    public void clearAll() {
        entries.clear();
        dataFile.delete();
        File[] files = mediaDir.listFiles();
        if (files != null) {
            for (File f : files) f.delete();
        }
    }

    public int mediaCount() {
        int n = 0;
        for (Entry e : entries) n += e.media.size();
        return n;
    }

    public long mediaBytes() {
        long total = 0;
        File[] files = mediaDir.listFiles();
        if (files != null) {
            for (File f : files) total += f.length();
        }
        return total;
    }

    public String importMedia(Uri uri, String kind) {
        String mime = ctx.getContentResolver().getType(uri);
        String ext = extensionFor(kind, mime);
        String name = kind + "_" + System.currentTimeMillis() + "_" + (int) (Math.random() * 1000) + "." + ext;
        File out = new File(mediaDir(), name);
        InputStream in = null;
        OutputStream os = null;
        try {
            in = ctx.getContentResolver().openInputStream(uri);
            if (in == null) return null;
            os = new FileOutputStream(out);
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) > 0) os.write(buf, 0, r);
            os.flush();
            return out.getAbsolutePath();
        } catch (Exception e) {
            out.delete();
            return null;
        } finally {
            try {
                if (in != null) in.close();
            } catch (Exception ignored) {
            }
            try {
                if (os != null) os.close();
            } catch (Exception ignored) {
            }
        }
    }

    public File newAudioFile() {
        return new File(mediaDir(), "audio_" + System.currentTimeMillis() + ".m4a");
    }

    private String extensionFor(String kind, String mime) {
        if (mime != null) {
            if (mime.contains("jpeg")) return "jpg";
            if (mime.contains("png")) return "png";
            if (mime.contains("webp")) return "webp";
            if (mime.contains("gif")) return "gif";
            if (mime.contains("quicktime")) return "mov";
            if (mime.contains("webm")) return "webm";
            if (mime.contains("3gpp")) return "3gp";
            if (mime.contains("mp4")) return "mp4";
            if (mime.contains("mpeg")) return "mp3";
            if (mime.contains("wav")) return "wav";
            if (mime.contains("aac")) return "m4a";
        }
        if ("image".equals(kind)) return "jpg";
        if ("video".equals(kind)) return "mp4";
        return "m4a";
    }

    private void load() {
        if (!dataFile.exists()) {
            loadedOk = true;
            return;
        }
        InputStream in = null;
        try {
            in = new java.io.FileInputStream(dataFile);
            byte[] buf = new byte[(int) dataFile.length()];
            int off = 0;
            while (off < buf.length) {
                int r = in.read(buf, off, buf.length - off);
                if (r < 0) break;
                off += r;
            }
            JSONArray arr = new JSONArray(new String(buf, 0, off, "UTF-8"));
            parseEntries(arr, entries);
            sort();
            loadedOk = true;
        } catch (Exception e) {
            // 数据损坏时保留原文件，避免误删媒体。
            File bak = new File(ctx.getFilesDir(), "entries-corrupt.json");
            dataFile.renameTo(bak);
            loadedOk = false;
        } finally {
            try {
                if (in != null) in.close();
            } catch (Exception ignored) {
            }
        }
    }

    private void parseEntries(JSONArray arr, List<Entry> out) {
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            Entry e = Entry.fromJson(o);
            if (e.date == null || e.date.length() != 10) continue;
            if (e.id == null || e.id.isEmpty()) e.id = UUID.randomUUID().toString();
            out.add(e);
            // 旧版同一天可能同时勾了做爱和自慰，迁移成两条独立记录。
            if (!o.has("type") && o.optBoolean("hadSex", false)
                    && o.optBoolean("hadMasturbation", false)) {
                Entry c = e.copy();
                c.id = UUID.randomUUID().toString();
                c.type = "mast";
                out.add(c);
            }
        }
    }

    private void save() {
        try {
            JSONArray arr = new JSONArray();
            for (Entry e : entries) arr.put(e.toJson());
            File tmp = new File(ctx.getFilesDir(), "entries.json.tmp");
            FileOutputStream os = new FileOutputStream(tmp);
            os.write(arr.toString().getBytes("UTF-8"));
            os.flush();
            os.close();
            if (dataFile.exists()) dataFile.delete();
            tmp.renameTo(dataFile);
        } catch (Exception ignored) {
        }
    }

    private void sort() {
        Collections.sort(entries, new Comparator<Entry>() {
            @Override
            public int compare(Entry a, Entry b) {
                int c = b.date.compareTo(a.date);
                if (c != 0) return c;
                int t = Long.compare(a.createdAt, b.createdAt);
                if (t != 0) return t;
                return Long.compare(a.updatedAt, b.updatedAt);
            }
        });
    }

    private void cleanupOrphans() {
        Set<String> used = new HashSet<>();
        for (Entry e : entries) {
            for (Media m : e.media) {
                if (m.path != null) used.add(new File(m.path).getName());
            }
        }
        File[] files = mediaDir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (!used.contains(f.getName())) f.delete();
        }
    }

    /** 给后台上传用的快照，保留 id，避免上传时 UI 继续修改 entries。 */
    public List<Entry> snapshotEntries() {
        List<Entry> out = new ArrayList<>();
        for (Entry e : entries) {
            Entry c = e.copy();
            c.id = e.id;
            c.media = new ArrayList<>(e.media);
            out.add(c);
        }
        return out;
    }

    /** 导出 entries.json 和所有被引用的媒体为一个 zip。 */
    public void exportZip(OutputStream out) throws Exception {
        exportZip(out, snapshotEntries());
    }

    public void exportZip(OutputStream out, List<Entry> list) throws Exception {
        ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(out));
        JSONArray arr = new JSONArray();
        for (Entry e : list) arr.put(e.toJson());
        zip.putNextEntry(new ZipEntry("entries.json"));
        zip.write(arr.toString().getBytes("UTF-8"));
        zip.closeEntry();

        Set<String> written = new HashSet<>();
        for (Entry e : list) {
            for (Media m : e.media) {
                if (m.path == null) continue;
                File f = new File(m.path);
                if (!f.isFile()) continue;
                String base = f.getName();
                if (!written.add(base)) continue;
                zip.putNextEntry(new ZipEntry("media/" + base));
                FileInputStream in = new FileInputStream(f);
                copy(in, zip);
                in.close();
                zip.closeEntry();
            }
        }
        zip.finish();
        zip.flush();
    }

    /** 用备份覆盖当前数据，返回导入的记录数。 */
    public int importZip(InputStream in) throws Exception {
        File tmp = new File(ctx.getFilesDir(), "import_tmp");
        deleteRecursive(tmp);
        if (!tmp.mkdirs()) throw new Exception("无法创建临时目录");

        ZipInputStream zip = new ZipInputStream(new BufferedInputStream(in));
        ZipEntry ze;
        while ((ze = zip.getNextEntry()) != null) {
            if (ze.isDirectory()) continue;
            String name = ze.getName();
            if ("entries.json".equals(name)) {
                FileOutputStream os = new FileOutputStream(new File(tmp, "entries.json"));
                copy(zip, os);
                os.close();
            } else if (name.startsWith("media/")) {
                String base = new File(name).getName();
                if (base.length() == 0) continue;
                File dir = new File(tmp, "media");
                if (!dir.exists()) dir.mkdirs();
                FileOutputStream os = new FileOutputStream(new File(dir, base));
                copy(zip, os);
                os.close();
            }
        }

        File entriesTmp = new File(tmp, "entries.json");
        if (!entriesTmp.isFile()) throw new Exception("备份里没有 entries.json");
        JSONArray arr = new JSONArray(readText(entriesTmp));

        File newMedia = new File(ctx.getFilesDir(), "media_import_" + System.currentTimeMillis());
        if (!newMedia.mkdirs()) throw new Exception("无法创建媒体目录");
        List<Entry> imported = new ArrayList<>();
        List<Entry> parsed = new ArrayList<>();
        parseEntries(arr, parsed);
        for (Entry e : parsed) {
            List<Media> ms = new ArrayList<>();
            for (Media m : e.media) {
                if (m.path == null) continue;
                String base = new File(m.path).getName();
                File src = new File(new File(tmp, "media"), base);
                if (!src.isFile()) continue;
                File dst = new File(newMedia, base);
                if (!dst.exists()) copyFile(src, dst);
                ms.add(new Media(m.kind, new File(mediaDir, base).getAbsolutePath()));
            }
            e.media = ms;
            imported.add(e);
        }

        File oldMedia = new File(ctx.getFilesDir(), "media_old_" + System.currentTimeMillis());
        boolean moved = false;
        if (mediaDir.exists()) {
            if (!mediaDir.renameTo(oldMedia)) throw new Exception("无法备份当前媒体");
            moved = true;
        }
        if (!newMedia.renameTo(mediaDir)) {
            if (moved) oldMedia.renameTo(mediaDir);
            throw new Exception("无法替换媒体目录");
        }

        entries.clear();
        entries.addAll(imported);
        sort();
        save();
        if (moved) deleteRecursive(oldMedia);
        deleteRecursive(tmp);
        return imported.size();
    }

    private static void copy(InputStream in, OutputStream out) throws Exception {
        byte[] buf = new byte[8192];
        int r;
        while ((r = in.read(buf)) > 0) out.write(buf, 0, r);
    }

    private static void copyFile(File src, File dst) throws Exception {
        FileInputStream in = new FileInputStream(src);
        FileOutputStream out = new FileOutputStream(dst);
        copy(in, out);
        in.close();
        out.close();
    }

    private static String readText(File f) throws Exception {
        FileInputStream in = new FileInputStream(f);
        byte[] data = new byte[(int) f.length()];
        int off = 0;
        while (off < data.length) {
            int r = in.read(data, off, data.length - off);
            if (r < 0) break;
            off += r;
        }
        in.close();
        return new String(data, 0, off, "UTF-8");
    }

    private static void deleteRecursive(File f) {
        if (f == null || !f.exists()) return;
        if (f.isDirectory()) {
            File[] kids = f.listFiles();
            if (kids != null) {
                for (File k : kids) deleteRecursive(k);
            }
        }
        f.delete();
    }

    public static String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new java.util.Date());
    }
}
