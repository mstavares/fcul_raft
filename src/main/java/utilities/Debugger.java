package utilities;

import java.sql.Timestamp;

/** Esta classe destina-se a enviar logs para a consola com um timestamp */
public abstract class Debugger {

    public static void log(String log) {
        System.out.println(new Timestamp(System.currentTimeMillis()).toString() + " " + log);
    }
}
