package org.jetlinks.simulator.core;

public class ExceptionUtils {

    public static Throwable tryGetRealError(Throwable err) {

        while (err.getCause() != null) {
            err = err.getCause();
        }
        return err;
    }

    public static String getErrorMessage(Throwable err) {
        String msg = err.getLocalizedMessage();
        return msg == null ? err.getClass().getSimpleName() : msg;
    }
}
