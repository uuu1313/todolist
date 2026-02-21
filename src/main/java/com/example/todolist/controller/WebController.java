package com.example.todolist.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class WebController {

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/lists/{token}")
    public String list(@PathVariable String token, Model model) {
        model.addAttribute("token", token);
        return "list";
    }

    @GetMapping("/lists/{token}/settings")
    public String listSettings(@PathVariable String token, Model model) {
        model.addAttribute("token", token);
        return "list-settings";
    }

    @GetMapping("/my-lists")
    public String myLists() {
        return "my-lists";
    }
}
