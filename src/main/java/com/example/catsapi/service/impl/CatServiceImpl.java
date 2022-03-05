package com.example.catsapi.service.impl;

import com.example.catsapi.dto.CatDto;
import com.example.catsapi.entity.Cat;
import com.example.catsapi.repository.CatRepository;
import com.example.catsapi.service.CatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Log4j2
@RequiredArgsConstructor
public class CatServiceImpl implements CatService {

    private final CatRepository catRepository;

    @Override
    public List<Cat> findAll() {
        List<Cat> list = catRepository.findAll();
        log.debug("Список всех котов: " + list);
        return list;
    }

    @Override
    public Cat save(CatDto catDto) {
        Cat cat = new Cat()
                .setId(UUID.randomUUID().toString())
                .setName(catDto.getName())
                .setBirthDate(catDto.getBirthDate())
                .setCreatedAt(LocalDateTime.now());

        Cat saved = catRepository.save(cat);
        log.debug("В БД сохранён кот: " + saved);
        return saved;
    }
}
