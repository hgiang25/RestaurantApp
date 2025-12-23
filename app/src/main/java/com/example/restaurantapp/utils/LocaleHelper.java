package com.example.restaurantapp.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

import java.util.Locale;

public class LocaleHelper {

    public static Context setLocale(Context context, String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration config = new Configuration(resources.getConfiguration());
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale);
            return context.createConfigurationContext(config);
        } else {
            config.locale = locale;
            resources.updateConfiguration(config, resources.getDisplayMetrics());
            return context;
        }
    }

    public static Context onAttach(Context context) {
        PreferenceManager prefs = new PreferenceManager(context);
        String lang = prefs.getLanguage();
        return setLocale(context, lang);
    }
    
    public static void applyLanguage(Context context) {
        PreferenceManager prefs = new PreferenceManager(context);
        String lang = prefs.getLanguage();
        setLocale(context, lang);
    }
}
