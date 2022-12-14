package com.example.addon.util.other;

public class Wrapper {
    public static int randomNum(int min, int max) {
        return min + (int) (Math.random() * ((max - min) + 1));
    }
}
