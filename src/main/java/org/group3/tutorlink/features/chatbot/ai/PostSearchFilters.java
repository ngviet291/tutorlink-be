package org.group3.tutorlink.features.chatbot.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.group3.tutorlink.features.post.enums.EducationLevel;
import org.group3.tutorlink.features.post.enums.PostType;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostSearchFilters {

    private String subjectName;

    private String province;

    private String area;

    private EducationLevel educationLevel;

    private PostType postType;

    private Integer minExperienceYears;

    private Double minRate;

    private Double maxRate;

    private Double minRating;
}
