package utilities;

import java.sql.Timestamp;

public abstract class Debugger {

    public static void log(String log) {
        System.out.println(new Timestamp(System.currentTimeMillis()).toString() + " " + log);
    }
}
