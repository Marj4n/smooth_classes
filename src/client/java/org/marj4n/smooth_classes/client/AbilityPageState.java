package org.marj4n.smooth_classes.client;

/** Client-side selector deciding what the three shared ability keys currently control. */
public final class AbilityPageState {
    public enum Page { CLASS, ORIGIN }

    private static Page page = Page.CLASS;

    private AbilityPageState() {}

    public static Page page() { return page; }
    public static boolean isClassPage() { return page == Page.CLASS; }
    public static boolean isOriginPage() { return page == Page.ORIGIN; }
    public static void toggle() { page = page == Page.CLASS ? Page.ORIGIN : Page.CLASS; }
    public static void setClassPage() { page = Page.CLASS; }
    public static void setOriginPage() { page = Page.ORIGIN; }
    public static void reset() { page = Page.CLASS; }
}
