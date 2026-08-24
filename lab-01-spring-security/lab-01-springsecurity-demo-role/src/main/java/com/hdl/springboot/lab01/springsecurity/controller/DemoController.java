package com.hdl.springboot.lab01.springsecurity.controller;

import jakarta.annotation.security.PermitAll;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/demo")
public class DemoController {

    @PermitAll
    @GetMapping("/echo")
    public String demo() {
        return "Example Response";
    }

    @GetMapping("/home")
    public String home() {
        return "I am the homepage.";
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String admin() {
        return "I am the administrator.";
    }

    @GetMapping("/normal")
    @PreAuthorize("hasRole('NORMAL')")
    public String normal() {
        return "I am a standard user.";
    }

}
