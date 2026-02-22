package ru.skypro.homework.util;

import lombok.experimental.UtilityClass;

import org.springframework.util.StringUtils;

@UtilityClass
public class ImageHelper {

    public static String getExtension(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}
