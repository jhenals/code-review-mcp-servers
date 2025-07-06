// UserController.java - Multiple vulnerabilities
@RestController
public class UserController {

    @GetMapping("/user/{id}")
    public ResponseEntity<User> getUser(@PathVariable String id) {
        // SQL injection through direct concatenation
        String query = "SELECT * FROM users WHERE id = " + id;
        User user = userService.findByQuery(query);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file) {
        // Path traversal vulnerability
        String filename = file.getOriginalFilename();
        String uploadPath = "/uploads/" + filename;
        // No validation of filename
        return uploadPath;
    }
}
