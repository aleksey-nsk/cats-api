package com.example.catsapi.controller;

import com.example.catsapi.dto.CatDto;
import com.example.catsapi.entity.Cat;
import com.example.catsapi.service.CatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cat")
@RequiredArgsConstructor
public class CatController {

    private final CatService catService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<Cat> findAll() {
        return catService.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Cat save(@RequestBody CatDto catDto) {
        return catService.save(catDto);
    }
}
