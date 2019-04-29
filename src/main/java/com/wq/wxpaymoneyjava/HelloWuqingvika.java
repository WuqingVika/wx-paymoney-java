package com.wq.wxpaymoneyjava;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloWuqingvika {
    @RequestMapping("/hello")
    public String hello(){
        return "hello,wq!";
    }
}
