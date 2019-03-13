package hive_clj;

public class HiveReporter {

    private String serializeSpan(HiveSpan span) {
        return "FOO";
    }

    public void reportSpan(HiveSpan span) {
        String payload = serializeSpan(span);
        String url = "http://hive.fmc.dev/api/report";

    }

}
