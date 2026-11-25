package org.group3.tutorlink.features.post.recommendation.semantic;

import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.features.application.matching.semantic.PostQueryTextBuilder;
import org.group3.tutorlink.features.post.entity.Post;
import org.group3.tutorlink.features.post.repository.PostRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class VectorStorePostIndexer implements PostVectorIndexer {
    private final PostRepository postRepository;
    private final VectorStore vectorStore;
    private final PostQueryTextBuilder textBuilder;

    @Override
    @Transactional(readOnly = true)
    public void index(UUID postId) {
        // findByIdForVisibilityCheck bỏ qua bài REMOVED nên bài đã gỡ sẽ rơi vào nhánh xóa
        postRepository.findByIdForVisibilityCheck(postId)
                .map(this::toDocument)
                .ifPresentOrElse(
                        document -> vectorStore.add(List.of(document)),
                        () -> remove(postId));
    }

    @Override
    public void remove(UUID postId) {
        vectorStore.delete(List.of(postId.toString()));
    }

    private Document toDocument(Post post) {
        String id = post.getId().toString();
        return Document.builder()
                .id(id)
                .text(textBuilder.build(post))
                .metadata(Map.of(PostDocumentFields.POST_ID, id))
                .build();
    }
}