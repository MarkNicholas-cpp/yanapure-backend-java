package com.yanapure.app.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class DbController {
    private final JdbcTemplate jdbc;

    public DbController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/db/ping")
    public Map<String, Object> ping() {
        Integer one = jdbc.queryForObject("select 1", Integer.class);
        return Map.of("ok", true, "db", one);
    }
}
