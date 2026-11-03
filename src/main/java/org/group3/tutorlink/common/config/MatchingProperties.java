package org.group3.tutorlink.common.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "app.matching")
public class MatchingProperties {

    @Positive private double weeksPerMonth;
    @Positive private int topN;
    @Positive private double globalAvgRating;
    @Positive private int minVotes;
    @Positive private double maxRating;
    @Positive private double experienceCapYears;

    @Valid @NotNull private Weights weights = new Weights();
    @Valid @NotNull private Weights aiWeights = new Weights();
    @Valid @NotNull private Penalty penalty = new Penalty();
    @Valid @NotNull private Price price = new Price();

    @Getter
    @Setter
    public static class Weights {
        private double price;
        private double rating;
        private double experience;
        private double semantic;
    }



    @Getter
    @Setter
    public static class Price {
        private double noBudgetScore;
        private double belowBudgetScore;
    }

    @Getter
    @Setter
    public static class Penalty {
        private double subject;
        private double level;
        private double mode;
        private double area;
        private double availability;
    }
}