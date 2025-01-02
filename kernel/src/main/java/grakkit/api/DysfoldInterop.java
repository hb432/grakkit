package grakkit.api;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class DysfoldInterop {
    public Object get (Object object, String chain) {
        String[] fields = chain.split("\\.");
        for (String field : fields) {
            try {
                Field internal = object.getClass().getDeclaredField(field);
                internal.setAccessible(true);
                object = internal.get(object);
            } catch (Throwable error) {
                try {
                    String getter = "get" + field.substring(0, 1).toUpperCase() + field.substring(1);
                    Method internal = object.getClass().getDeclaredMethod(getter);
                    object = internal.invoke(object);
                } catch (Throwable error2) {
                    error2.printStackTrace();
                    return null;
                }
            }
        }
        return object;
    }
}