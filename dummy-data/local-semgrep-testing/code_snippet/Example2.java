public class SemgrepAutoConfigTest {

    public static void main(String[] args) {
        // Example of hardcoded password - mcpsemgrep auto McpConfiguration may detect this
        String password = "password123";

        // Example of dangerous command execution
        try {
            Runtime.getRuntime().exec("rm -rf /tmp/test");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Example of printing to console
        System.out.println("Test complete");
    }
}
