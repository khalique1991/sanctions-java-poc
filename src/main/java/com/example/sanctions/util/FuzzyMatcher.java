package com.example.sanctions.util;
import java.util.List;
public class FuzzyMatcher {
    public static int levenshtein(String s1, String s2){
        s1 = s1==null?"":s1; s2 = s2==null?"":s2;
        int[] prev = new int[s2.length()+1];
        for(int j=0;j<=s2.length();j++) prev[j]=j;
        for(int i=1;i<=s1.length();i++){
            int[] curr = new int[s2.length()+1]; curr[0]=i;
            for(int j=1;j<=s2.length();j++){
                int cost = s1.charAt(i-1)==s2.charAt(j-1)?0:1;
                curr[j] = Math.min(Math.min(curr[j-1]+1, prev[j]+1), prev[j-1]+cost);
            }
            prev = curr;
        }
        return prev[s2.length()];
    }
    public static String match(String name, List<String> candidates){
        int min = Integer.MAX_VALUE; String best = null;
        for(String c: candidates){ int d = levenshtein(name.toLowerCase(), c.toLowerCase()); if(d<min){min=d;best=c;} }
        return best;
    }
}