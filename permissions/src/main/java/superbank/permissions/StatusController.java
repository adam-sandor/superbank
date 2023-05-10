package superbank.permissions;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusController {

    @GetMapping("/status")
    ResponseEntity<String> status() {
        return ResponseEntity.ok("OK");
    }
}
