package org.xdty.callerinfo.utils;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.Settings.Secure;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;

import org.xdty.callerinfo.R;
import org.xdty.callerinfo.activity.MarkActivity;
import org.xdty.callerinfo.model.TextColorPair;
import org.xdty.callerinfo.model.setting.Setting;
import org.xdty.callerinfo.model.setting.SettingImpl;
import org.xdty.callerinfo.service.FloatWindow;
import org.xdty.phone.number.model.INumber;
import org.xdty.phone.number.model.Type;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;
import java.util.UUID;

public class Utils {

    public static final int NOTIFICATION_MARK = 0x01;
    private static final String TAG = Utils.class.getSimpleName();

    public static void showTextWindow(Context context, int resId, int frontType) {
        Bundle bundle = new Bundle();
        bundle.putString(FloatWindow.NUMBER_INFO, context.getString(resId));
        bundle.putInt(FloatWindow.WINDOW_COLOR, ContextCompat.getColor(context,
                R.color.colorPrimary));
        Log.d(TAG, "showTextWindow: " + Utils.bundleToString(bundle));
        FloatWindow.show(context, FloatWindow.class, frontType);
        FloatWindow.sendData(context, FloatWindow.class,
                frontType, 0, bundle, FloatWindow.class, 0);
    }

    public static void sendData(Context context, String key, int value, int frontType) {
        Bundle bundle = new Bundle();
        bundle.putInt(key, value);
        FloatWindow.show(context, FloatWindow.class, frontType);
        FloatWindow.sendData(context, FloatWindow.class,
                frontType, 0, bundle, FloatWindow.class, 0);
    }

    public static void closeWindow(Context context) {
        Log.d(TAG, "closeWindow");
        FloatWindow.closeAll(context, FloatWindow.class);
    }

    public static void showWindow(Context context, INumber number, int frontType) {

        TextColorPair textColor = Utils.getTextColorPair(context, number);

        Bundle bundle = new Bundle();
        bundle.putString(FloatWindow.NUMBER_INFO, textColor.text);
        bundle.putInt(FloatWindow.WINDOW_COLOR, textColor.color);
        Log.d(TAG, "showWindow: " + Utils.bundleToString(bundle));
        FloatWindow.show(context, FloatWindow.class,
                frontType);
        FloatWindow.sendData(context, FloatWindow.class,
                frontType, 0, bundle, FloatWindow.class, 0);
    }

    public static TextColorPair getTextColorPair(Context context, INumber number) {

        String province = number.getProvince();
        String city = number.getCity();
        String operators = number.getProvider();
        return Utils.getTextColorPair(context, number.getType().getText(), province, city,
                operators, number.getName(), number.getCount());
    }

    private static TextColorPair getTextColorPair(Context context, String type, String province,
            String city, String operators, String name, int count) {

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        if (province == null && city == null && operators == null) {
            province = context.getResources().getString(R.string.unknown);
            city = "";
            operators = "";
        }

        if (!TextUtils.isEmpty(province) && !TextUtils.isEmpty(city) && province.equals(city)) {
            city = "";
        }

        TextColorPair t = new TextColorPair();
        switch (Type.fromString(type)) {
            case NORMAL:
                t.text = context.getResources().getString(
                        R.string.text_normal, province, city, operators);
                t.color = preferences.getInt("color_normal",
                        ContextCompat.getColor(context, R.color.blue_light));
                break;
            case POI:
                t.color = preferences.getInt("color_poi",
                        ContextCompat.getColor(context, R.color.orange_dark));
                t.text = context.getResources().getString(
                        R.string.text_poi, name);
                break;
            case REPORT:
                t.color = preferences.getInt("color_report",
                        ContextCompat.getColor(context, R.color.red_light));
                if (count == 0) {
                    t.text = name;
                } else {
                    t.text = context.getResources().getString(
                            R.string.text_report, province, city, operators,
                            count, name);
                }
                break;
        }

        t.text = t.text.trim();

        if (t.text.isEmpty() || t.text.contains(context.getString(R.string.baidu_advertising))) {
            t.text = context.getString(R.string.unknown);
        }

        return t;
    }

    public static boolean isContactExists(Context context, String number) {
        Uri lookupUri = Uri.withAppendedPath(
                PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(number));
        String[] mPhoneNumberProjection = {
                PhoneLookup._ID, PhoneLookup.NUMBER, PhoneLookup.DISPLAY_NAME
        };
        Cursor cur = context.getContentResolver().query(lookupUri, mPhoneNumberProjection, null,
                null, null);
        if (cur != null) {
            try {
                if (cur.moveToFirst()) {
                    return true;
                }
            } finally {
                cur.close();
            }
        }
        return false;
    }

    public static String getContactName(Context context, String number) {
        Uri lookupUri = Uri.withAppendedPath(
                PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(number));
        String[] mPhoneNumberProjection = {
                PhoneLookup._ID, PhoneLookup.NUMBER, PhoneLookup.DISPLAY_NAME
        };
        Cursor cur = context.getContentResolver().query(lookupUri, mPhoneNumberProjection, null,
                null, null);
        if (cur != null) {
            try {
                if (cur.moveToFirst()) {
                    return cur.getString(cur.getColumnIndex(PhoneLookup.DISPLAY_NAME));
                }
            } finally {
                cur.close();
            }
        }
        return "";
    }

    public static String getDate(long time) {
        Calendar calendar = Calendar.getInstance();
        TimeZone tz = TimeZone.getDefault();
        calendar.add(Calendar.MILLISECOND, tz.getOffset(calendar.getTimeInMillis()));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        java.util.Date currentTimeZone = new java.util.Date(time);
        return sdf.format(currentTimeZone);
    }

    public static String readableDate(Context context, long time) {
        String result;
        long current = System.currentTimeMillis();
        if (current - time < 60 * 60 * 24 * 1000) {
            result = context.getString(R.string.readable_today);
        } else if (current - time < 2 * 60 * 60 * 24 * 1000) {
            result = context.getString(R.string.readable_yesterday);
        } else {
            result = DateFormat.format(context.getString(R.string.readable_date),
                    new Date(time)).toString();
        }
        return result;
    }

    public static String readableTime(Context context, long time) {
        String result;
        int seconds = (int) (time / 1000) % 60;
        int minutes = (int) ((time / (1000 * 60)) % 60);
        int hours = (int) ((time / (1000 * 60 * 60)) % 24);
        if (time < 60000) {
            result = context.getString(R.string.readable_second, seconds);
        } else if (time < 3600000) {
            result = context.getString(R.string.readable_minute, minutes, seconds);
        } else {
            result = context.getString(R.string.readable_hour, hours, minutes, seconds);
        }
        return result;
    }

    public static String mask(String s) {
        return s.replaceAll("([0-9]|[a-f])", "*");
    }

    public static void checkLocale(Context context) {

        if (SettingImpl.getInstance().isForceChinese()) {
            Locale locale = new Locale("zh");
            Locale.setDefault(locale);
            Configuration config = context.getResources().getConfiguration();
            config.locale = locale;
            context.getResources().updateConfiguration(config,
                    context.getResources().getDisplayMetrics());
        }
    }

    public static boolean isAppInstalled(Context context, String packageName) {
        try {
            context.getPackageManager().getApplicationInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static int getVersionCode(Context context, String packageName) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
            return info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            return 0;
        }
    }

    public static String bundleToString(Bundle bundle) {
        StringBuilder out = new StringBuilder("Bundle[");

        if (bundle == null) {
            out.append("null");
        } else {
            boolean first = true;
            for (String key : bundle.keySet()) {
                if (!first) {
                    out.append(", ");
                }

                out.append(key).append('=');

                Object value = bundle.get(key);

                if (value instanceof int[]) {
                    out.append(Arrays.toString((int[]) value));
                } else if (value instanceof byte[]) {
                    out.append(Arrays.toString((byte[]) value));
                } else if (value instanceof boolean[]) {
                    out.append(Arrays.toString((boolean[]) value));
                } else if (value instanceof short[]) {
                    out.append(Arrays.toString((short[]) value));
                } else if (value instanceof long[]) {
                    out.append(Arrays.toString((long[]) value));
                } else if (value instanceof float[]) {
                    out.append(Arrays.toString((float[]) value));
                } else if (value instanceof double[]) {
                    out.append(Arrays.toString((double[]) value));
                } else if (value instanceof String[]) {
                    out.append(Arrays.toString((String[]) value));
                } else if (value instanceof CharSequence[]) {
                    out.append(Arrays.toString((CharSequence[]) value));
                } else if (value instanceof Parcelable[]) {
                    out.append(Arrays.toString((Parcelable[]) value));
                } else if (value instanceof Bundle) {
                    out.append(bundleToString((Bundle) value));
                } else {
                    out.append(value);
                }

                first = false;
            }
        }

        out.append("]");
        return out.toString();
    }

    public static String getDeviceId(Context context) {
        final TelephonyManager tm =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        final String tmDevice, tmSerial, androidId;
        tmDevice = "" + tm.getDeviceId();
        tmSerial = "" + tm.getSimSerialNumber();
        androidId = "" + Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);

        UUID deviceUuid = new UUID(androidId.hashCode(),
                ((long) tmDevice.hashCode() << 32) | tmSerial.hashCode());
        return deviceUuid.toString();
    }

    public static void showMarkNotification(Context context, String number) {
        Intent intent = new Intent(context, MarkActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        Setting setting = SettingImpl.getInstance();
        setting.addPaddingMark(number);

        ArrayList<String> list = setting.getPaddingMarks();
        String numbers = TextUtils.join(", ", list);

        NotificationManager manager = (NotificationManager) context.getSystemService(
                Context.NOTIFICATION_SERVICE);

        int requestCode = new Random().nextInt();
        PendingIntent pIntent = PendingIntent.getActivity(context, requestCode, intent, 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.status_icon)
                .setContentIntent(pIntent)
                .setContentTitle(context.getString(R.string.mark_number))
                .setContentText(numbers)
                .setAutoCancel(true)
                .setContentIntent(pIntent);
        manager.notify(NOTIFICATION_MARK, builder.build());
    }

    public static void startMarkActivity(Context context, String number) {
        Intent intent = new Intent(context, MarkActivity.class);
        intent.putExtra(MarkActivity.NUMBER, number);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static int typeFromString(Context context, String type) {
        ArrayList<String> types = new ArrayList<>(
                Arrays.asList(context.getResources().getStringArray(R.array.mark_type_source)));
        for (String t : types) {
            if (t.contains(type)) {
                return types.indexOf(t);
            }

            ArrayList<String> ts = new ArrayList<>(Arrays.asList(t.split("|")));
            for (String s : ts) {
                if (type.contains(s)) {
                    return types.indexOf(t);
                }
            }
        }
        Log.e(TAG, "typeFromString failed: " + type);
        return -1;
    }
}
