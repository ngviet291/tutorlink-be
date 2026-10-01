package org.group3.tutorlink.features.post.dto.response;

import lombok.*;

import java.util.UUID;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubjectSummaryResponse {

    private UUID id;
    private String name;
}