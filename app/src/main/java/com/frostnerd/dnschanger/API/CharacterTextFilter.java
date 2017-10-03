package com.frostnerd.dnschanger.API;

import android.text.InputFilter;
import android.text.Spanned;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * Code may NOT be used without proper permission, neither in binary nor in source form.
 * All redistributions of this software in source code must retain this copyright header
 * All redistributions of this software in binary form must visibly inform users about usage of this software
 * <p>
 * development@frostnerd.com
 */
public class CharacterTextFilter implements InputFilter {
    private final InputFilter impl;

    public CharacterTextFilter(final Set<Character> allowedChars){
        impl = new InputFilter(){
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                StringBuilder builder = new StringBuilder();
                for(int i = 0; i < source.length(); i++){
                    char c = source.charAt(i);
                    if(!allowedChars.contains(c))builder.append(c);
                }
                return builder.length() == end-start ? null : builder.toString();
            }
        };
    }

    public CharacterTextFilter(final Pattern pattern){
        impl = new InputFilter(){
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                Matcher matcher = pattern.matcher("");
                StringBuilder builder = new StringBuilder();
                for(int i = 0; i < source.length(); i++){
                    char c = source.charAt(i);
                    if(matcher.reset(c + "").matches())builder.append(c);
                }
                return builder.length() == end-start ? null : builder.toString();
            }
        };
    }

    @Override
    public CharSequence filter(CharSequence charSequence, int start, int end, Spanned dest, int dstart, int dend) {
        return impl.filter(charSequence, start, end, dest, dstart, dend);
    }
}
