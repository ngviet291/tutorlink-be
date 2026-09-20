package org.group3.tutorlink.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.Set;

@Entity
@Table(name = "admins")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Admin extends User {

    private String department;

    // Admin 1 -- 0..* Application (accepts / processes)
    @OneToMany(mappedBy = "processedByAdmin")
    private Set<Application> processedApplication;

    // Admin 1 -- 0..* RevenueReport
    @OneToMany(mappedBy = "generatedByAdmin")
    private Set<RevenueReport> revenueReports ;

    @OneToMany(mappedBy = "createdByAdmin")
    private Set<MatchingFeeConfig> matchingFeeConfigs;
}
