package org.group3.tutorlink.features.ai.exception;


import org.group3.tutorlink.common.exception.AppException;

public class AiProviderException extends AppException {

    public AiProviderException() {
        super(ErrorCodeAi.AI_PROVIDER_ERROR);
    }
}