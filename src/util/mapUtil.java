package util;

import java.util.LinkedHashMap;
import java.util.Map;

public class mapUtil
{
    private static final MyComparator myComparator = new MyComparator();

    public static Map<String, String[]> sortMapByValueSize(Map<String, String[]> map)
    {
        LinkedHashMap<String, String[]> sortedMap = new LinkedHashMap<>();

        map.entrySet().stream().sorted(Map.Entry.comparingByValue(myComparator)).forEachOrdered(x -> sortedMap.put(x.getKey(), x.getValue()));

        return sortedMap;
    }
}
