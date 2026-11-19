package org.group3.tutorlink.features.chatbot.config;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class Scoring {

    // Trọng số các tiêu chí (chỉ tiêu chí đang áp dụng mới được tính)
    @Min(0)
    private double subjectWeight = 40;
    @Min(0)
    private double distanceWeight = 35;
    @Min(0)
    private double levelWeight = 15;
    @Min(0)
    private double recencyWeight = 10;

    // Độ giống tên môn tối thiểu (0..1); thấp hơn thì loại bài
    private double minSubjectSimilarity = 0.5;

    // Bán kính tìm quanh phường/xã người dùng nói tới; không có bài nào thì mở rộng sang bán kính lớn hơn
    @Min(1)
    private double radiusKm = 10;
    @Min(1)
    private double expandedRadiusKm = 30;

    // Độ giống (0..1) khi tên môn này nằm trọn trong tên môn kia, ví dụ "Toán" và "Toán cao cấp"
    private double containsSimilarity = 0.9;

    // Điểm vị trí (0..1) cho bài không phụ thuộc vị trí (dạy online / chưa có địa chỉ)
    private double locationFreeScore = 0.5;

    // Điểm độ mới (0..1) khi bài không có ngày tạo
    private double unknownRecencyScore = 0.5;

    // Điểm độ mới giảm một nửa sau bấy nhiêu ngày
    private double recencyHalfLifeDays = 14;

    // Lấy tối thiểu bấy nhiêu bài mới nhất làm ứng viên trước khi chấm điểm
    @Min(1)
    private int minCandidatePool = 500;
}