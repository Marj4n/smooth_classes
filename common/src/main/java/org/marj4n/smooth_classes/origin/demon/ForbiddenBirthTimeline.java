package org.marj4n.smooth_classes.origin.demon;

/** Shared presentation timing; no client decides world generation or birth credit. */
public final class ForbiddenBirthTimeline {
    public static final int AWAKEN=55,FALL_START=95,IMPACT=155,REVEAL=185,FIRST_BREATH=215,END=285,TIMEOUT=400;
    private ForbiddenBirthTimeline(){}
    public static double clamp(double v){return Math.max(0,Math.min(1,v));}
    public static double smooth(double v){v=clamp(v);return v*v*(3-2*v);}
    public static double fall(double tick){double t=clamp((tick-FALL_START)/(IMPACT-FALL_START));return t*t;}
    public static double emergence(double tick){return smooth((tick-REVEAL)/(FIRST_BREATH-REVEAL));}
    public static boolean hidden(double tick){return tick<REVEAL;}
}
