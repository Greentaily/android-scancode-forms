package com.example.forms;

public class CodeParser {
    public static String createQuery(String serial, String form, String data, int dataIdx, int fields) {
        StringBuilder query = new StringBuilder("Q1")
                .append(serial)
                .append("\u0001")
                .append(form)
                .append("\u0002");
        for (int i = 0; i < fields - 1; i++) {
            String field = (i == dataIdx ? data + '\t' : String.valueOf('\t'));
            query.append(field);
        }
        query.append('\r');
        return query.toString();
    }

    public static String[] parseQuery() {
        return null;
    }
}
