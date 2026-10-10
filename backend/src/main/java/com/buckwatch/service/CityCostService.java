package com.buckwatch.service;

import com.buckwatch.model.CityCost;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PostConstruct;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class CityCostService {
    private final ResourceLoader loader;
    @Value("${COST_INDEX_SOURCE:}") private String source;
    @Value("${COST_INDEX_OBSERVATION_DATE:}") private String observationDate;
    private Map<String, CityCost> cities = Map.of();
    public CityCostService(ResourceLoader loader) { this.loader = loader; }

    @PostConstruct
    public void load() {
        Resource resource = loader.getResource("classpath:data/Cost_of_living_index.csv");
        Map<String, CityCost> records = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            for (CSVRecord row : CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setTrim(true).get().parse(reader)) {
                String city = row.get("City").trim();
                positive(row, "Rank");
                positive(row, "Cost of Living Plus Rent Index");
                positive(row, "Local Purchasing Power Index");
                CityCost cost = new CityCost(city, number(row, "Cost of Living Index"),
                        number(row, "Rent Index"), number(row, "Groceries Index"), number(row, "Restaurant Price Index"));
                if (city.isBlank() || records.putIfAbsent(city, cost) != null)
                    throw new IllegalStateException("Blank or duplicate canonical city: " + city);
            }
        } catch (IOException | RuntimeException e) { throw new IllegalStateException("Unable to load city index CSV", e); }
        if (records.isEmpty()) throw new IllegalStateException("City index CSV is empty");
        cities = Collections.unmodifiableMap(records);
    }
    private BigDecimal number(CSVRecord row, String field) {
        BigDecimal value = new BigDecimal(row.get(field).trim());
        if (value.signum() <= 0) throw new IllegalStateException("Invalid non-positive index in " + field + " for " + row.get("City"));
        return value;
    }
    private BigDecimal positive(CSVRecord row, String field) { return number(row, field); }
    public CityCost resolve(String text) {
        if (text == null || text.isBlank()) throw new ScenarioException("INVALID_CITY", "Destination city is required.");
        List<CityCost> matches = cities.values().stream().filter(c -> c.city().equalsIgnoreCase(text.trim())).toList();
        if (matches.size() == 1) return matches.getFirst();
        List<String> choices = cities.values().stream().filter(c -> c.city().toLowerCase(Locale.ROOT).contains(text.trim().toLowerCase(Locale.ROOT))).map(CityCost::city).limit(10).toList();
        if (choices.size() == 1) return cities.get(choices.getFirst());
        if (!choices.isEmpty()) throw new ScenarioException("AMBIGUOUS_CITY", "Choose a supported city from the matching options.", choices);
        throw new ScenarioException("UNSUPPORTED_CITY", "City is not in the supported cost-of-living dataset.");
    }
    public List<String> search(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return cities.values().stream().map(CityCost::city).filter(c -> c.toLowerCase(Locale.ROOT).contains(q)).limit(20).toList();
    }
    public Collection<String> allCities() { return cities.keySet(); }
    public CityCost get(String name) { return resolve(name); }
    public Map<String,String> metadata() { Map<String,String> result=new HashMap<>(); result.put("source",source); result.put("observationDate",observationDate); result.put("observationDateAvailableInCsv","false"); return result; }
}
