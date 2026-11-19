package com.pashe.app.medicine.catalog;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/medicine/catalog")
public class MedicineCatalogController {

    private final MedicineCatalogService service;

    public MedicineCatalogController(
            MedicineCatalogService service) {
        this.service = service;
    }

    @GetMapping
    public List<MedicineCatalogItem> search(
            @RequestParam(defaultValue = "") String query) {

        return service.search(query);
    }

    @GetMapping("/{id}")
    public MedicineCatalogItem getMedicine(
            @PathVariable UUID id) {

        return service.getById(id);
    }
}