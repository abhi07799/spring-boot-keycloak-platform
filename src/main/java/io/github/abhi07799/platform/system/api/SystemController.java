package io.github.abhi07799.platform.system.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class SystemController {

    @GetMapping("/ping")
    public PingResponse ping() {
        return new PingResponse("pong");
    }

    public record PingResponse(String message) {
    }
}
