package shipwrights.dataplanets.texturing;

public class Maths {
    static int clamp(int v, int max) { return Math.max(0, Math.min(max-1, v)); }

    static int clamp255(int v)         { return clamp(v,256); }

    static double lerp(double t, double a, double b) { return a + t * (b - a); }

    static int clamp(int v) { return Math.max(0, Math.min(255, v)); }

    static int lerp(int a, int b, float t) {
        return clamp(Math.round(a + t * (b - a)));
    }
}
