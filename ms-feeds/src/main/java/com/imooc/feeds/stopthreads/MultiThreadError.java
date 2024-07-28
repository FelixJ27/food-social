package com.imooc.feeds.stopthreads;

import java.util.HashMap;
import java.util.Map;

public class MultiThreadError {

    private Map<String, String> states;

    public MultiThreadError() {
        states = new HashMap<>();
        states.put("1", "周一");
        states.put("2", "周二");
        states.put("3", "周三");
    }

    public Map<String, String> getStates() {
        return states;
    }

    public Map<String, String> getStatesImproved() {
        return new HashMap<>(states);
    }

    public static void main(String[] args) {
        MultiThreadError multiThreadError = new MultiThreadError();
        //Map<String, String> states = multiThreadError.getStates();
        System.out.println(multiThreadError.getStatesImproved().get("1"));
        multiThreadError.getStatesImproved().remove("1");
        System.out.println(multiThreadError.getStatesImproved().get("1"));
    }
}
