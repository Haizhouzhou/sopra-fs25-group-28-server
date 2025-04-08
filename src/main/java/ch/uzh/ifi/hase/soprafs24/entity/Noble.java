package ch.uzh.ifi.hase.soprafs24.entity;

import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;
import javax.persistence.Table;

@Entity
@Table(name = "NOBLE")
public class Noble extends Item {

    @Id
    private Long id;

    @ElementCollection
    @CollectionTable(name = "NOBLE_REQUIREMENT", joinColumns = @JoinColumn(name = "noble_id"))
    @MapKeyColumn(name = "gem_type")
    @Enumerated(EnumType.STRING)
    @Column(name = "amount")
    private Map<GemColor, Integer> requirements;

    public Noble() {}

    public Noble(Long id, int points, Map<GemColor, Integer> requirements) {
        super(points);
        this.id = id;
        this.requirements = requirements;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Map<GemColor, Integer> getRequirements() {
        return requirements;
    }

    public void setRequirements(Map<GemColor, Integer> requirements) {
        this.requirements = requirements;
    }

    /**
     * Utility to convert a string-keyed cost map to enum-keyed one.
     */
    public static Map<GemColor, Integer> convertRequirementMap(Map<String, Integer> stringMap) {
        return stringMap.entrySet().stream()
            .collect(Collectors.toMap(
                entry -> GemColor.valueOf(entry.getKey().toUpperCase()),
                Map.Entry::getValue
            ));
    }
}
