public class VulnerableExample {
    public void insecureMethod() {
        String password = "123456";
        try {
            Runtime.getRuntime().exec("rm -rf");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}