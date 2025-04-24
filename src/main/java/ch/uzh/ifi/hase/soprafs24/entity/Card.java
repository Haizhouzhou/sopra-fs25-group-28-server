// package ch.uzh.ifi.hase.soprafs24.entity;

// import java.util.HashMap;
// import java.util.Map;

// import javax.persistence.CollectionTable;
// import javax.persistence.Column;
// import javax.persistence.ElementCollection;
// import javax.persistence.Entity;
// import javax.persistence.EnumType;
// import javax.persistence.Enumerated;
// import javax.persistence.Id;
// import javax.persistence.JoinColumn;
// import javax.persistence.MapKeyColumn;
// import javax.persistence.Table;

// @Entity
// @Table(name = "CARD")
// public class Card extends Item {

//     @Id
//     private Long id;

//     @Enumerated(EnumType.STRING)
//     @Column(nullable = false)
//     private GemColor color;

//     @Column(nullable = false)
//     private int tier;

//     @ElementCollection
//     @CollectionTable(name = "CARD_COST", joinColumns = @JoinColumn(name = "card_id"))
//     @MapKeyColumn(name = "gem_type")
//     @Column(name = "amount")
//     private Map<String, Integer> cost;

//     public Card() {}

//     public Card(Long id, int points, GemColor color, int tier, Map<String, Integer> cost) {
//         super(points);
//         this.id = id;
//         this.color = color;
//         this.tier = tier;
//         this.cost = cost;
//     }

//     public Long getId() {
//         return id;
//     }

//     public GemColor getColor() {
//         return color;
//     }

//     public int getTier() {
//         return tier;
//     }

//     public Map<String, Integer> getCost() {
//         return cost;
//     }

//     public void setId(Long id) {
//         this.id = id;
//     }

//     public void setColor(GemColor color) {
//         this.color = color;
//     }

//     public void setTier(int tier) {
//         this.tier = tier;
//     }

//     public void setCost(Map<String, Integer> cost) {
//         this.cost = cost;
//     }

//     /**
//      * Utility method to convert string-keyed cost maps to GemColor-keyed ones.
//      */
//     public static Map<GemColor, Integer> convertStringCostToGemColor(Map<String, Integer> stringCost) {
//         Map<GemColor, Integer> convertedCost = new HashMap<>();
//         for (Map.Entry<String, Integer> entry : stringCost.entrySet()) {
//             try {
//                 GemColor gemColor = GemColor.valueOf(entry.getKey().toUpperCase());
//                 convertedCost.put(gemColor, entry.getValue());
//             } catch (IllegalArgumentException e) {
//                 System.err.println("Unknown gem color: " + entry.getKey());
//             }
//         }
//         return convertedCost;
//     }
// }
