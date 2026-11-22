package org.group3.tutorlink.features.application.matching.semantic;

import org.group3.tutorlink.common.utils.SemanticText;
import org.group3.tutorlink.features.post.entity.Post;
import org.springframework.stereotype.Component;

@Component
public class PostQueryTextBuilder {

    public String build(Post post) {
        return SemanticText.create()
                .line("Môn học", post.getSubject() == null ? null : post.getSubject().getName())
                .line("Cấp học", post.getEducationLevel() == null ? null : post.getEducationLevel().name())
                .line("Tiêu đề", post.getTitle())
                .line("Yêu cầu", post.getContent())
                .build();
    }
}