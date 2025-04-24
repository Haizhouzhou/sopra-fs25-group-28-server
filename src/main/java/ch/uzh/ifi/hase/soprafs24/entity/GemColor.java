package ch.uzh.ifi.hase.soprafs24.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum GemColor {
    WHITE,
    BLUE,
    GREEN,
    RED,
    BLACK,
    GOLD;

    @JsonCreator
    public static GemColor fromString(String key){
        return GemColor.valueOf(key.toUpperCase());
    }
}
