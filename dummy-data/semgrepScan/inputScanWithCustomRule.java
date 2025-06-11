Map<String, Object> input = new HashMap<>();

Map<String, String> codeFileMap = Map.of(
        "filename", "VulnerableExample.java",
        "content", "public class VulnerableExample {\n" +
                "    public void insecureMethod() {\n" +
                "        String password = \"123456\";\n" +
                "        try {\n" +
                "            Runtime.getRuntime().exec(\"rm -rf /\");\n" +
                "        } catch (Exception e) {\n" +
                "            e.printStackTrace();\n" +
                "        }\n" +
                "    }\n" +
                "}"
);

input.put("code_file", codeFileMap);
input.put("config", "auto");  // or any config you want to test with
input.put("rule",
                  "rules:\n" +
                  "  - id: hardcoded-password\n" +
                  "    patterns:\n" +
                  "      - pattern: String $PASSWORD = \"$SECRET\"\n" +
                  "    message: \"Hardcoded password detected\"\n" +
                  "    severity: ERROR\n" +
                  "    languages: [java]\n" +
                  "    metadata:\n" +
                  "      category: security\n" +
                  "\n" +
                  "  - id: dangerous-exec\n" +
                  "    patterns:\n" +
                  "      - pattern: Runtime.getRuntime().exec($CMD)\n" +
                  "    message: \"Dangerous command execution detected\"\n" +
                  "    severity: ERROR\n" +
                  "    languages: [java]\n" +
                  "    metadata:\n" +
                  "      category: security\n"
);
