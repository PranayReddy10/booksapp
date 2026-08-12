package com.jntuh.util;

/**
 * JNTUH grade helpers used for instant client-side feedback in the
 * Add/Edit result screen. The server still computes/validates authoritatively.
 */
public final class GradeUtil {

    private GradeUtil() {
    }

    /** Grade spinner order shared by the UI. */
    public static final String[] GRADES = {"O", "A+", "A", "B+", "B", "C", "F", "Ab"};

    /** Grade -> value (O=10 ... F/Ab=0). Returns null for unknown grades. */
    public static Integer gradeValue(String grade) {
        if (grade == null) return null;
        switch (grade.trim().toUpperCase()) {
            case "O":  return 10;
            case "A+": return 9;
            case "A":  return 8;
            case "B+": return 7;
            case "B":  return 6;
            case "C":  return 5;
            case "F":
            case "AB": return 0;
            default:   return null;
        }
    }

    /** grade points = credits x grade value. Null if either is missing/invalid. */
    /**
     * JNTUH "Grade Point (Gi)" = the grade value only (A+ -> 9), NOT multiplied
     * by credits. This is what the memo shows per subject and what the Pts column
     * displays. The credit-weighting (Gi x Ci) happens only inside SGPA/CGPA.
     * The credits parameter is kept for call-site compatibility but unused.
     */
    public static Double gradePoints(String grade, Double credits) {
        Integer v = gradeValue(grade);
        if (v == null) return null;
        return (double) v;
    }

    /** F / Ab count as a backlog. */
    public static boolean isFailGrade(String grade) {
        if (grade == null) return false;
        String g = grade.trim().toUpperCase();
        return g.equals("F") || g.equals("AB");
    }
}
