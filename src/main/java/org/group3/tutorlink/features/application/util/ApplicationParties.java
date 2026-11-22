package org.group3.tutorlink.features.application.util;

import org.group3.tutorlink.features.application.entity.Application;
import org.group3.tutorlink.features.post.entity.Post;
import org.group3.tutorlink.features.post.enums.PostType;
import org.group3.tutorlink.features.user.entity.User;

public final class ApplicationParties {

    private ApplicationParties() {
    }
    public static User applicantOf(Application application) {
        Post post = application.getPost();
        if (post != null && post.getType() == PostType.FIND_TUTOR) {
            return application.getTutor();
        }
        return application.getStudent();
    }
    public static User payerTutorOf(Application application) {
        if (application.getTutor() != null) {
            return application.getTutor();
        }
        return application.getPost() != null ? application.getPost().getAuthor() : null;
    }
}