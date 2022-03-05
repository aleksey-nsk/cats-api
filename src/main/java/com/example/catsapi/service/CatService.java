package com.example.catsapi.service;

import com.example.catsapi.dto.CatDto;
import com.example.catsapi.entity.Cat;

import java.util.List;

/**
 * @author Aleksey Zhdanov
 * @version 1
 */
public interface CatService {

    /**
     * <p>Возвращает список котов</p>
     *
     * @return Список котов
     */
    List<Cat> findAll();

    /**
     * <p>Добавляет нового кота</p>
     *
     * @param catDto Данные кота для добавления
     * @return Сохранённый в БД кот
     */
    Cat save(CatDto catDto);
}
