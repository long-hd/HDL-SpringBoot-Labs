package com.hdl.springboot.lab01.springsecurity.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestController {

    @GetMapping("/demo")
    public String demo() {
        return "Example Response";
    }

    @GetMapping("/home")
    public String home() {
        return "I am the homepage.";
    }

    @GetMapping("/admin")
    public String admin() {
        return "I am the administrator.";
    }

    @GetMapping("/normal")
    public String normal() {
        return "I am a standard user.";
    }

}
