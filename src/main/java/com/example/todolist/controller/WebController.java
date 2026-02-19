package com.example.todolist.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/lists/{token}")
    public String list() {
        return "list";
    }

    @GetMapping("/my-lists")
    public String myLists() {
        return "my-lists";
    }
}
