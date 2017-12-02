package server;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class StateMachine {

    private HashMap<String, String> storedData = new HashMap<String, String>();

    public void put(String key, String value) {
        storedData.put(key, value);
    }

    public String get(String key) {
        return storedData.get(key);
    }

    public void del(String key) {
        storedData.remove(key);
    }

    public String list() {
        StringBuilder stringBuilder = new StringBuilder();
        Iterator it = storedData.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            stringBuilder.append(pair.getKey()).append(" -> ").append(pair.getValue());
            it.remove(); // avoids a ConcurrentModificationException
        }
        return stringBuilder.toString();
    }

    public String cas(String key, String oldValue, String newValue) {
        String x = storedData.get(key);
        if(x.equalsIgnoreCase(oldValue))
            storedData.put(key, newValue);
        return x;
    }

}
