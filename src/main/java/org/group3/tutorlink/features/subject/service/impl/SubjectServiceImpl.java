package org.group3.tutorlink.features.subject.service.impl;

import lombok.RequiredArgsConstructor;
import org.group3.tutorlink.common.dto.response.CursorResponse;
import org.group3.tutorlink.common.utils.AppUtil;
import org.group3.tutorlink.features.subject.dto.request.SubjectRequest;
import org.group3.tutorlink.features.subject.dto.response.SubjectResponse;
import org.group3.tutorlink.features.subject.entity.Subject;
import org.group3.tutorlink.features.subject.exception.SubjectInUseException;
import org.group3.tutorlink.features.subject.exception.SubjectNotFoundException;
import org.group3.tutorlink.features.subject.mapper.SubjectMapper;
import org.group3.tutorlink.features.subject.repository.SubjectRepository;
import org.group3.tutorlink.features.subject.service.SubjectService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubjectServiceImpl implements SubjectService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;

    private final SubjectRepository subjectRepository;
    private final SubjectMapper subjectMapper;
    private final AppUtil appUtil;

    @Override
    @Transactional(readOnly = true)
    public CursorResponse<SubjectResponse> getAllSubjects(UUID cursor, int limit) {
        int size = limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);

        Pageable pageable = PageRequest.of(
                0,
                size + 1,
                Sort.by(Sort.Order.desc("id"))
        );

        List<Subject> subjects = subjectRepository.findPage(cursor, pageable);

        return appUtil.buildCursorResponse(
                subjects,
                size,
                Subject::getId,
                subjectMapper::toSubjectResponse
        );
    }

    @Override
    @Transactional(readOnly = true)
    public SubjectResponse getSubjectById(UUID id) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(SubjectNotFoundException::new);
        return subjectMapper.toSubjectResponse(subject);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ADMIN')")
    public SubjectResponse createSubject(SubjectRequest request) {
        Subject subject = subjectMapper.toSubject(request);
        subject.setId(appUtil.generateUUID());
        Subject savedSubject = subjectRepository.save(subject);
        return subjectMapper.toSubjectResponse(savedSubject);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ADMIN')")
    public SubjectResponse updateSubject(UUID id, SubjectRequest request) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(SubjectNotFoundException::new);

        subjectMapper.updateSubjectFromRequest(request, subject);
        Subject updatedSubject = subjectRepository.save(subject);

        return subjectMapper.toSubjectResponse(updatedSubject);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ADMIN')")
    public void deleteSubject(UUID id) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(SubjectNotFoundException::new);

        if (subjectRepository.isUsedByTutor(id)) {
            throw new SubjectInUseException();
        }

        subjectRepository.delete(subject);
    }
}
