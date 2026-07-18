package it.bd.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {
    @GetMapping(value = {"/", "/app", "/app/**"})
    public String index() {
        return "forward:/index.html";
    }
}
