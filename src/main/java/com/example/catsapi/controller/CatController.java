package com.example.catsapi.controller;

import com.example.catsapi.dto.CatDto;
import com.example.catsapi.entity.Cat;
import com.example.catsapi.service.CatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Log4j2
public class CatController {

    private final CatService catService;

    @GetMapping("/cat")
    @ResponseStatus(HttpStatus.OK)
    public List<Cat> findAll() {
        return catService.findAll();
    }

    @PostMapping("/cat")
    @ResponseStatus(HttpStatus.CREATED)
    public Cat save(@RequestBody CatDto catDto) {
        return catService.save(catDto);
    }

    @GetMapping("/ip")
    @ResponseStatus(HttpStatus.OK)
    public String getIP() {
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            String ip = localHost.getHostAddress();
            log.debug("Current IP address: " + ip);
            return ip;
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return "UnknownHostException";
        }
    }
}
