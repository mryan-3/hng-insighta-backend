package com.hng.stage3.utils;

import java.util.HashMap;
import java.util.Map;

public class CountryCodeMapper {
    private static final Map<String, String> countryMap = new HashMap<>();

    static {
        countryMap.put("nigeria", "NG");
        countryMap.put("kenya", "KE");
        countryMap.put("angola", "AO");
        countryMap.put("ghana", "GH");
        countryMap.put("tanzania", "TZ");
        countryMap.put("uganda", "UG");
        countryMap.put("south africa", "ZA");
        countryMap.put("egypt", "EG");
        countryMap.put("ethiopia", "ET");
        countryMap.put("morocco", "MA");
        countryMap.put("algeria", "DZ");
        countryMap.put("united states", "US");
        countryMap.put("united kingdom", "GB");
        countryMap.put("canada", "CA");
        countryMap.put("germany", "DE");
        countryMap.put("france", "FR");
    }

    public static String getCode(String name) {
        if (name == null) return null;
        return countryMap.get(name.toLowerCase().trim());
    }
}
