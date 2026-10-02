package org.group3.tutorlink.features.subject.service.impl;

import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.features.subject.dto.request.SubjectRequest;
import org.group3.tutorlink.features.subject.dto.response.SubjectResponse;
import org.group3.tutorlink.features.subject.entity.Subject;
import org.group3.tutorlink.features.subject.exception.SubjectInUseException;
import org.group3.tutorlink.features.subject.exception.SubjectNotFoundException;
import org.group3.tutorlink.features.subject.mapper.SubjectMapper;
import org.group3.tutorlink.features.subject.repository.SubjectRepository;
import org.group3.tutorlink.features.subject.service.SubjectService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubjectServiceImpl implements SubjectService {

    private final SubjectRepository subjectRepository;
    private final SubjectMapper subjectMapper;

    @Override
    public List<SubjectResponse> getAllSubjects() {
        return subjectRepository.findAll().stream()
                .map(subjectMapper::toSubjectResponse)
                .collect(Collectors.toList());
    }

    @Override
    public SubjectResponse getSubjectById(UUID id) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new SubjectNotFoundException());
        return subjectMapper.toSubjectResponse(subject);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public SubjectResponse createSubject(SubjectRequest request) {
        Subject subject = subjectMapper.toSubject(request);
        subject.setId(UUID.randomUUID());
        Subject savedSubject = subjectRepository.save(subject);
        return subjectMapper.toSubjectResponse(savedSubject);
    }

    @Override
    @Transactional
    public SubjectResponse updateSubject(UUID id, SubjectRequest request) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new SubjectNotFoundException());

        subjectMapper.updateSubjectFromRequest(request, subject);
        Subject updatedSubject = subjectRepository.save(subject);

        return subjectMapper.toSubjectResponse(updatedSubject);
    }

    @Override
    @Transactional
    public void deleteSubject(UUID id) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new SubjectNotFoundException());

        if (subjectRepository.isUsedByTutor(id)) {
            throw new SubjectInUseException();
        }

        subjectRepository.delete(subject);
    }
}