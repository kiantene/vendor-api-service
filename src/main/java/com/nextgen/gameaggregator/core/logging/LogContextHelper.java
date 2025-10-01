package com.nextgen.gameaggregator.core.logging;

class LogContextHelper {

    private LogContextHelper() {}

    public static Throwable findRootCause(Throwable t) {
        if (t == null) return null;
        // guard against cycles
        var seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Throwable, Boolean>());
        Throwable cur = t;
        while (cur.getCause() != null && !seen.contains(cur.getCause())) {
            seen.add(cur);
            cur = cur.getCause();
        }
        return cur;
    }

    /**
     * Build a safe stack trace string with bounded size and suppressed causes.
     * @param t the throwable
     * @param maxFrames max total frames to include (across causes)
     */
    public static String buildStackTraceString(Throwable t, int maxFrames) {
        StringBuilder sb = new StringBuilder(4096);
        int remaining = Math.max(1, maxFrames);

        // Walk the cause chain
        Throwable cur = t;
        while (cur != null && remaining > 0) {
            sb.append(cur.getClass().getName())
                    .append(": ")
                    .append(String.valueOf(cur.getMessage()))
                    .append('\n');

            StackTraceElement[] frames = cur.getStackTrace();
            int take = Math.min(frames.length, remaining);
            for (int i = 0; i < take; i++) {
                sb.append("\tat ").append(frames[i]).append('\n');
            }
            remaining -= take;

            // suppressed
            for (Throwable sup : cur.getSuppressed()) {
                if (remaining <= 0) break;
                sb.append("\tSuppressed: ").append(sup.getClass().getName())
                        .append(": ").append(String.valueOf(sup.getMessage())).append('\n');
                StackTraceElement[] sFrames = sup.getStackTrace();
                int sTake = Math.min(sFrames.length, remaining);
                for (int i = 0; i < sTake; i++) {
                    sb.append("\t\tat ").append(sFrames[i]).append('\n');
                }
                remaining -= sTake;
            }

            cur = cur.getCause();
            if (cur != null) {
                sb.append("Caused by: ");
            }
        }
        if (remaining == 0) {
            sb.append("... stack trace truncated ...");
        }
        return sb.toString();
    }
}
