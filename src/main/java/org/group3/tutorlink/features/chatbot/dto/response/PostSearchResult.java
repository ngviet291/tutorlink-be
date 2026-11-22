package org.group3.tutorlink.features.chatbot.dto.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.features.chatbot.dto.response.PostSummaryResponse;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class PostSearchResult {

    private final List<PostSummaryResponse> posts;
    private final boolean relaxed;
}