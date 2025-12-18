package com.embabel.gekko.htmx;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/platform")
public class PlatformController {

    @SuppressWarnings("SameReturnValue")
    @GetMapping
    public String home() {
        return "common/platform";
    }
}
