package com.hng.stage3.utils;

import org.springframework.stereotype.Component;
import java.util.Locale;

@Component
public class CountryCodeMapper {
    
    public String getCountryName(String code) {
        if (code == null) return null;
        Locale locale = new Locale("", code);
        return locale.getDisplayCountry(Locale.ENGLISH);
    }

    public String getCode(String name) {
        if (name == null) return null;
        for (String iso : Locale.getISOCountries()) {
            Locale l = new Locale("", iso);
            if (l.getDisplayCountry(Locale.ENGLISH).equalsIgnoreCase(name)) {
                return iso;
            }
        }
        return null;
    }
}
