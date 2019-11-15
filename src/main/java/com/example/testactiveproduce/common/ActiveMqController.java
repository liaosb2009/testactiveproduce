package com.example.testactiveproduce.common;

import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 * @ClassName:ActiveMqController
 * @Description TODO
 * @Author liao
 * @Time 2019/9/11 20:24
 */
@RestController
@RequestMapping("/send")
public class ActiveMqController {

    @Autowired
    private ActiveMqTran activeMqTran;

    @GetMapping("/s1")
    public void test(){
        activeMqTran.start();
    }

}
