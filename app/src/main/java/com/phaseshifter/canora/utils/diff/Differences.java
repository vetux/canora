package com.phaseshifter.canora.utils.diff;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Helper Class for debugging test assertions.
 */
public class Differences {
    public static <T> String getDifference(T obj1, T obj2) {
        StringBuilder sbuild = new StringBuilder();
        try {
            List<Field> fields = new ArrayList<>(Arrays.asList(obj1.getClass().getDeclaredFields()));
            if (obj1.getClass().getSuperclass() != null)
                fields.addAll(Arrays.asList(obj1.getClass().getSuperclass().getDeclaredFields()));
            for (int i = 0; i < fields.size(); i++) {
                fields.get(i).setAccessible(true);
                if (!Objects.equals(fields.get(i).get(obj1), fields.get(i).get(obj2))) {
                    sbuild.append("\nDiffering Field ").append(i)
                            .append("\nNAME:(").append(fields.get(i).getName()).append(")")
                            .append("\nTYPE:(").append(fields.get(i).getType().toString()).append(")")
                            .append("\nVALUE OBJ1:(").append(fields.get(i).get(obj1) == null ? null : fields.get(i).get(obj1).toString()).append(")")
                            .append("\nVALUE OBJ2:(").append(fields.get(i).get(obj2) == null ? null : fields.get(i).get(obj2).toString()).append(")")
                            .append("\n");
                }
                fields.get(i).setAccessible(false);
            }
        } catch (Exception e) {
            sbuild.append("\nERROR: ")
                    .append(e.toString())
                    .append("\n");
        }
        return sbuild.toString();
    }
}