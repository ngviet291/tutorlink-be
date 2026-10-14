package org.group3.tutorlink.common.service;


import java.util.concurrent.CompletableFuture;

public interface MediaStorageService {

    CompletableFuture<UploadMediaResponse> uploadImage(byte[] data, String folder);

    CompletableFuture<UploadMediaResponse> uploadVideo(byte[] data, String folder);

    void delete(String publicId);
}
