package com.tenderbot.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaController {

    @RequestMapping(value = { 
        "/", "/tenders", "/tenders/**", "/suppliers", "/logs", "/config", "/login", "/login/**", "/users", "/users/**"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
