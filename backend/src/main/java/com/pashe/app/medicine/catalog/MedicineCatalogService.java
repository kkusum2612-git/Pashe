package com.pashe.app.medicine.catalog;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
public class MedicineCatalogService {

    private final MedicineCatalogRepository repository;

    public MedicineCatalogService(
            MedicineCatalogRepository repository) {
        this.repository = repository;
    }

    public List<MedicineCatalogItem> search(String query) {

        String term = normalize(query);

        return repository.findAllActive()
            .stream()
            .filter(medicine -> matches(medicine, term))
            .limit(30)
            .toList();
    }

    public MedicineCatalogItem getById(UUID id) {

        return repository.findAllActive()
            .stream()
            .filter(medicine -> medicine.id().equals(id))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Medicine not found"
            ));
    }

    private boolean matches(
            MedicineCatalogItem medicine,
            String term) {

        if (term.isEmpty()) {
            return true;
        }

        return normalize(medicine.brandName()).contains(term)
            || normalize(medicine.manufacturer()).contains(term)
            || medicine.ingredients()
                .stream()
                .anyMatch(ingredient ->
                    normalize(ingredient.name()).contains(term)
                );
    }

    private String normalize(String value) {
        return value == null
            ? ""
            : value.strip().toLowerCase(Locale.ROOT);
    }
}