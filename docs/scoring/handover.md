# TutorLink - Tài liệu bàn giao: xếp hạng ứng viên bằng vector database

Phạm vi: module `application.matching` (backend Spring Boot), dữ liệu test và các thay đổi liên quan ở frontend.

## 1. Tổng quan

### 1.1. Bài toán

Học viên đăng bài tìm gia sư (`FIND_TUTOR`), gia sư ứng tuyển và hệ thống tạo `Application` ở trạng thái `PENDING`. Khi một bài có nhiều ứng viên, học viên cần được gợi ý ra **top K gia sư phù hợp nhất** (mặc định K = 5), phần còn lại nằm trong danh sách `others`.

### 1.2. Thay đổi so với thiết kế cũ

| Hạng mục | Trước | Sau |
| --- | --- | --- |
| Chấm điểm ngữ nghĩa | Gọi LLM (Spring AI chat) mỗi lần xếp hạng | Embedding một lần khi gia sư cập nhật hồ sơ, so khớp vector bằng pgvector |
| Khớp tiếng Anh và tiếng Việt | Phải tự duy trì từ điển đồng nghĩa | Mô hình embedding đa ngôn ngữ (`bge-m3`), không cần từ điển |
| Cấu trúc code | Một service lớn chứa toàn bộ logic chấm điểm | Mỗi tiêu chí là một `MatchSignal`, service chỉ điều phối |
| Cấu hình | Rải rác trong code | Tập trung trong `application.properties` (`matching.*`, `spring.ai.*`) |
| Khi vector lỗi | Không áp dụng | Vẫn xếp hạng bằng các tiêu chí luật, người dùng không thấy lỗi |

### 1.3. Điều kiện tiên quyết của luồng nghiệp vụ

1. Học viên đăng bài `FIND_TUTOR`.
2. Gia sư ứng tuyển, sinh ra `Application` trạng thái `PENDING`.
3. Chỉ sau đó mới gọi xếp hạng. Vector chỉ chấm điểm cho đúng nhóm gia sư đã ứng tuyển bài đó, không quét toàn bộ bảng gia sư.

## 2. Kiến trúc và luồng xử lý

```
rankApplications(postId)
  |- kiểm tra quyền: bài FIND_TUTOR và đúng chủ bài
  |- lấy các Application PENDING
  |- số ứng viên <= topN  -> trả hết theo thời gian ứng tuyển (ranked = false)
  |- số ứng viên >  topN  -> buildRanked:
       MatchContextFactory   nạp theo lô: số review, số lớp đã nhận, điểm vector
         |- SemanticMatcher -> VectorStore (pgvector), lọc theo id các ứng viên
       WeightedScorer        duyệt List<MatchSignal>, cộng có trọng số
       MismatchPenaltyPolicy nhân hệ số phạt (môn, cấp, hình thức, khu vực, lịch)
       sắp xếp theo điểm giảm dần, hòa thì theo thời gian ứng tuyển
       tách thành top K và others
```

Luồng index (gia sư thành vector):

```
Gia sư cập nhật hồ sơ -> publish TutorProfileChangedEvent
  -> TutorIndexingListener (@Async, sau commit)
  -> TutorVectorIndexer.index(tutorId)
  -> TutorProfileTextBuilder dựng văn bản -> EmbeddingModel -> VectorStore (upsert theo id gia sư)
```

### 2.1. Quyết định thiết kế quan trọng

- **Không dùng index HNSW** (`index-type=NONE`). Ta luôn lọc theo danh sách ứng viên của một bài (vài chục gia sư). Với pgvector, HNSW lọc sau khi quét index nên có thể trả thiếu kết quả khi bộ lọc hẹp. Quét tuần tự trên tập đã lọc vừa nhanh vừa chính xác.
- **Tín hiệu thiếu dữ liệu thì bị bỏ qua và chuẩn hóa lại trọng số.** Ví dụ gia sư chưa được index thì mất tín hiệu `semantic`, điểm của họ được tính trên các tín hiệu còn lại, không bị 0 điểm.
- **Điểm cosine thô được co giãn** về \[0, 1\] bằng `floor` và `ceiling`, vì cosine của `bge-m3` thường chỉ dao động khoảng 0.35 đến 0.85.
- **Không loại hẳn ứng viên không khớp**, chỉ nhân hệ số phạt, để học viên vẫn xem được.
- **Tách rõ phần luật và phần vector**: service xếp hạng chỉ phụ thuộc interface `SemanticMatcher`, không biết Spring AI.

## 3. Cấu trúc mã nguồn

Gốc package: `org.group3.tutorlink`. Ký hiệu: `[+]` mới, `[~]` sửa, `[-]` xóa.

```
common/config
  MatchingProperties.java                     [~]
  AsyncConfig.java                            [+]  @EnableAsync
  SynonymProperties.java                      [-]
features/ai/service
  TutorMatchingAiService (+ impl)             [-]
features/user/service/impl
  TutorServiceImpl.java                       [~]  publish TutorProfileChangedEvent sau khi lưu hồ sơ
features/application
  dto/projection/TutorAcceptedRow.java        [+]
  dto/response/MatchBreakdown.java            [-]
  dto/response/ScoredApplicationResponse.java [~]  breakdown: Map<String, Double>
  repository/ApplicationRepository.java       [~]  thêm countByTutorIdsAndStatus
  service/impl/ApplicationRankingServiceImpl  [~]  chỉ còn điều phối
  matching/
    AreaMatcher, AvailabilityChecker          (giữ nguyên)
    KeywordMatcher, SynonymNormalizer         [-]
    context/    MatchContext, MatchContextFactory                    [+]
    signal/     MatchSignal, PriceSignal, RatingSignal,
                ExperienceSignal, TrustSignal, SemanticSignal,
                ExpectedFeeResolver                                  [+]
    scoring/    WeightedScorer, WeightedScore, ScoredCandidate,
                MismatchPenaltyPolicy                                [+]
    semantic/   SemanticMatcher, VectorStoreSemanticMatcher,
                PostQueryTextBuilder, TutorProfileTextBuilder,
                SemanticText, TutorDocumentFields,
                TutorVectorIndexer, VectorStoreTutorIndexer,
                TutorProfileChangedEvent, TutorIndexingListener,
                TutorReindexRunner                                   [+]
common/config/InitData.java                   [~]  thêm @Order(1)
resources/data/seed-data.json                 [~]  thêm dữ liệu test xếp hạng
```

### 3.1. Trách nhiệm từng thành phần

| Thành phần | Trách nhiệm |
| --- | --- |
| `ApplicationRankingServiceImpl` | Kiểm tra quyền, chọn nhánh có/không xếp hạng, ghép kết quả thành response |
| `MatchContextFactory` | Nạp dữ liệu theo lô cho cả danh sách ứng viên (tránh N+1); bắt lỗi vector để fallback |
| `WeightedScorer` | Duyệt các `MatchSignal`, cộng có trọng số, chuẩn hóa khi thiếu tín hiệu |
| `MatchSignal` (interface) | Một tiêu chí chấm điểm, trả `OptionalDouble` (rỗng nghĩa là thiếu dữ liệu) |
| `MismatchPenaltyPolicy` | Phát hiện không khớp và tính hệ số phạt |
| `ExpectedFeeResolver` | Quy đổi phí gia sư (theo giờ) về đơn vị của bài đăng (buổi, tháng) |
| `SemanticMatcher` (interface) | Trả điểm tương đồng \[0, 1\] giữa bài đăng và từng gia sư |
| `VectorStoreSemanticMatcher` | Cài đặt bằng Spring AI `VectorStore`, lọc theo id ứng viên, co giãn điểm |
| `TutorVectorIndexer` | Index và xóa hồ sơ gia sư trong vector store |
| `TutorIndexingListener` | Lắng nghe sự kiện đổi hồ sơ, index bất đồng bộ sau commit |
| `TutorReindexRunner` | Index lại toàn bộ gia sư khi khởi động (chỉ bật khi cần) |

### 3.2. Đối chiếu với SOLID

| Nguyên tắc | Áp dụng |
| --- | --- |
| S | Mỗi class một việc: chấm điểm, phạt, nạp dữ liệu, dựng văn bản, index đều tách riêng |
| O | Thêm tiêu chí mới chỉ cần thêm một `@Component implements MatchSignal` và một dòng trọng số, không sửa code cũ |
| L | Mọi `MatchSignal` có thể thay thế nhau trong `WeightedScorer` |
| I | `SemanticMatcher` và `TutorVectorIndexer` là hai interface nhỏ, tách biệt đọc và ghi |
| D | Service xếp hạng phụ thuộc `SemanticMatcher`, không phụ thuộc Spring AI |

## 4. Công thức chấm điểm

### 4.1. Các tín hiệu và trọng số mặc định

| Khóa | Ý nghĩa | Trọng số |
| --- | --- | --- |
| `price` | Phí so với ngân sách bài đăng (trong khoảng: 1.0; thấp hơn: `below-budget-score`; vượt: giảm dần về 0) | 0.30 |
| `rating` | Điểm trung bình Bayesian, gia sư ít review bị kéo về trung bình hệ thống | 0.25 |
| `experience` | Số năm kinh nghiệm, chặn trên bởi `experience-cap-years` | 0.15 |
| `trust` | Số lớp đã nhận, chặn trên bởi `trust.accepted-cap` | 0.10 |
| `semantic` | Độ tương đồng vector giữa nội dung bài đăng và hồ sơ gia sư, đã co giãn | 0.20 |

Điểm cơ sở = tổng (trọng số x điểm) chia cho tổng trọng số của các tín hiệu **có dữ liệu**.

### 4.2. Hệ số phạt

Điểm cơ sở được nhân với tích các hệ số của từng cảnh báo: `SUBJECT_MISMATCH`, `LEVEL_MISMATCH`, `MODE_MISMATCH`, `AREA_MISMATCH`, `AVAILABILITY_MISMATCH`. Thiếu dữ liệu (gia sư cũ chưa khai cấp học hoặc hình thức dạy) được coi là trung tính, không phạt.

### 4.3. Quy đổi phí

Ưu tiên `proposedFee` do gia sư nhập (đã theo đơn vị của bài). Nếu không có thì quy giá theo giờ trong hồ sơ: theo buổi = giá giờ x số phút / 60; theo tháng = theo buổi x số buổi mỗi tuần x `weeks-per-month`. Thiếu dữ liệu để quy đổi hoặc đơn vị lạ thì chấm trung tính (`no-budget-score`).

### 4.4. Hòa điểm

Cùng điểm thì ưu tiên người ứng tuyển sớm hơn (`appliedAt`).

## 5. Cấu hình

### 5.1. Dependency (`pom.xml`)

- `spring-ai-starter-vector-store-pgvector`
- `spring-ai-starter-model-ollama` (chỉ dùng cho embedding)
- Giữ starter Gemini nếu còn dùng chat ở chỗ khác. Xóa starter chat không dùng nữa.
- Tên artifact theo Spring AI 1.0.x; phiên bản khác có thể đặt tên khác.

### 5.2. `application.properties`

```properties
# Chọn nhà cung cấp cho từng loại model (tránh lỗi hai bean ChatModel)
spring.ai.model.chat=google-genai
spring.ai.model.embedding=ollama

# Chat: Gemini
spring.ai.google.genai.api-key=${GEMINI_API_KEY}
spring.ai.google.genai.chat.model=gemini-2.5-flash
spring.ai.google.genai.chat.temperature=0.2
spring.ai.google.genai.chat.thinking-budget=0

# Embedding: Ollama (ollama pull bge-m3)
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.embedding.options.model=bge-m3
spring.ai.ollama.init.pull-model-strategy=when_missing

# Vector store: pgvector
spring.ai.vectorstore.pgvector.initialize-schema=true
spring.ai.vectorstore.pgvector.table-name=tutor_vector_store
spring.ai.vectorstore.pgvector.dimensions=1024
spring.ai.vectorstore.pgvector.distance-type=COSINE_DISTANCE
spring.ai.vectorstore.pgvector.index-type=NONE

# Matching
matching.top-n=5
matching.weights.price=0.30
matching.weights.rating=0.25
matching.weights.experience=0.15
matching.weights.trust=0.10
matching.weights.semantic=0.20
matching.trust.accepted-cap=10
matching.semantic.floor=0.35
matching.semantic.ceiling=0.85
matching.semantic.reindex-on-startup=false
```

Các khóa còn lại (`matching.min-votes`, `global-avg-rating`, `max-rating`, `experience-cap-years`, `weeks-per-month`, `price.*`, `penalty.*`) giữ nguyên theo file cấu hình hiện tại.

### 5.3. Ý nghĩa các khóa cần chú ý

| Khóa | Ghi chú |
| --- | --- |
| `spring.ai.vectorstore.pgvector.dimensions` | Phải khớp mô hình embedding (`bge-m3` là 1024). Đổi mô hình thì phải đổi số này và index lại toàn bộ gia sư |
| `matching.semantic.floor`, `ceiling` | Cosine thô tương ứng với điểm 0 và 1. Cần chỉnh theo dữ liệu thật |
| `matching.semantic.reindex-on-startup` | Bật một lần để index gia sư đã có, rồi tắt |
| `matching.top-n` | Số ứng viên ở `top`; không vượt quá số này thì không chấm điểm |

### 5.4. Hạ tầng cần có

- PostgreSQL có extension pgvector (ví dụ image `pgvector/pgvector:pg16`).
- Ollama chạy được với model `bge-m3`.
- Biến môi trường `GEMINI_API_KEY` nếu còn dùng chat.
- Cần `@EnableAsync` trên một class cấu hình để listener index chạy bất đồng bộ.

## 6. Thay đổi hợp đồng API và frontend

- `ScoredApplicationResponse.breakdown` đổi từ object cố định (`MatchBreakdown`) sang `Map<String, Double>`.
- Các khóa hiện có: `price`, `rating`, `experience`, `trust`, `semantic`. Khóa `semantic` có thể vắng mặt khi gia sư chưa được index hoặc vector lỗi.
- Trường `experienceYears` không còn trong breakdown; frontend lấy từ hồ sơ gia sư.
- Frontend cần sửa type: `breakdown?: Record<string, number>` và phần hiển thị lý do xếp hạng không được giả định đủ mọi khóa.

## 7. Dữ liệu test

### 7.1. Nạp dữ liệu

1. Đặt `seed-data.json` vào `src/main/resources/data/`.
2. `InitData` chỉ chạy khi DB trống và chỉ chạy ở profile `dev`, nên cần xóa dữ liệu dev trước khi nạp lại.
3. `InitData` có `@Order(1)` và `TutorReindexRunner` phải có `@Order(2)` để index diễn ra sau khi seed xong.
4. Chạy lần đầu với `matching.semantic.reindex-on-startup=true`, rồi tắt. Seeder dùng `em.persist` nên không bắn sự kiện đổi hồ sơ, vì vậy cần bước này để các gia sư vào vector store.

### 7.2. Tài khoản

| Vai trò | Tài khoản | Mật khẩu |
| --- | --- | --- |
| Học viên test | `match.student1@gmail.com`, `match.student2@gmail.com` | `Student@123` |
| Gia sư test | `match.tutor1` đến `match.tutor20@gmail.com` | `Tutor@123` |
| Tài khoản mặc định | `admin@gmail.com`, `student@gmail.com`, `tutor@gmail.com` | `Admin@123`, `Student@123`, `Tutor@123` |

### 7.3. Quy mô dữ liệu sau khi gộp

31 gia sư, 11 học viên, 70 bài đăng, 122 application, 25 review. Phần thêm cho test xếp hạng: 2 học viên, 20 gia sư, 13 bài đăng (3 bài chính, 10 bài lịch sử đã đóng), 43 application và 12 review.

### 7.4. Ba bài đăng chính

| Bài | Mã bài | Chủ bài | Ứng viên | Mục đích |
| --- | --- | --- | --- | --- |
| A | `a9439e8c-3401-504a-88f0-c83d29cb60df` | `match.student1` | 13 | Toán lớp 9, học tại nhà, Phường 12, 150k–250k mỗi buổi. Kịch bản chính |
| B | `31b4cbb1-a75d-53a7-9a3f-313d9a98fcab` | `match.student2` | 8 | Bài viết bằng tiếng Anh tìm gia sư IELTS, online, 200k–350k mỗi giờ. Kiểm tra khớp chéo ngôn ngữ |
| C | `db56c821-2ee7-5567-8cfd-bc89b664b147` | `match.student2` | 3 | Piano. Chỉ 3 ứng viên nên không chấm điểm |

### 7.5. Bài A: mỗi gia sư kiểm tra một điều

| Gia sư | Tình huống | Kỳ vọng |
| --- | --- | --- |
| `match.tutor1` | Khớp hoàn toàn, 8 review, trung bình 4.75 | Top 1–2 |
| `match.tutor2` | Bio tiếng Anh về Toán | `semantic` cao dù khác ngôn ngữ |
| `match.tutor3` | 5 sao nhưng chỉ 1 review | Rating Bayesian thấp hơn `tutor1` |
| `match.tutor4` | 10 lớp đã hoàn thành, 9 năm kinh nghiệm | `trust` tối đa (sau khi sửa mục 8.1) |
| `match.tutor5` | Không có bio | Vẫn xếp hạng được, thiếu phần nội dung bio |
| `match.tutor6` | Dạy Ngữ văn | `SUBJECT_MISMATCH`, cuối bảng |
| `match.tutor7` | Sai môn nhưng bio nói về Toán THCS | Bị phạt sai môn, `semantic` khá cao |
| `match.tutor8` | Chỉ dạy THPT và đại học | `LEVEL_MISMATCH` |
| `match.tutor9` | Chỉ dạy online | `MODE_MISMATCH` |
| `match.tutor10` | Khu vực Quận 1 và Quận 7 | `AREA_MISMATCH` |
| `match.tutor11` | Đề xuất 400k, vượt ngân sách | `price` khoảng 0.4 |
| `match.tutor12` | Không có `proposedFee`, giá giờ 110k | Quy đổi ra 165k mỗi buổi, nằm trong ngân sách |
| `match.tutor13` | Chỉ rảnh 1 buổi mỗi tuần | `AVAILABILITY_MISMATCH` (tùy `AvailabilityChecker`) |

### 7.6. Bài B: kiểm tra tiếng Anh

- `match.tutor14`: bio tiếng Anh, môn IELTS và Tiếng Anh, dự kiến top 1.
- `match.tutor15`: bio tiếng Việt về luyện thi IELTS nhưng môn là Tiếng Anh, bị phạt sai môn nhưng `semantic` vẫn cao. Cặp này kiểm tra việc vector hiểu cùng nghĩa giữa hai ngôn ngữ.
- `match.tutor16` và `tutor2` (có sẵn): nhóm đầu.
- `match.tutor17` và `match.tutor18`: `semantic` thấp (tiếng Anh trẻ em, lập trình Python).

Lưu ý: cột "kỳ vọng" là dự đoán. Mình chưa thấy `AreaMatcher` và `AvailabilityChecker` nên các cảnh báo khu vực và lịch có thể khác. Hãy xem phân bố điểm `semantic` thực tế rồi chỉnh `floor` và `ceiling` trước khi kết luận thứ tự đúng hay sai.

### 7.7. Cách kiểm tra nhanh

Đăng nhập `match.student1@gmail.com`, gọi API xếp hạng với mã bài A. Kết quả mong đợi: `ranked = true`, 5 gia sư trong `top`, 8 gia sư trong `others`, mỗi phần tử có `breakdown` gồm `price`, `rating`, `experience`, `trust`, `semantic`. Gọi bài C thì `ranked = false` và trả đủ 3 ứng viên theo thời gian ứng tuyển.

## 8. Việc còn lại và vấn đề đã biết

### 8.1. Cần sửa trước khi nghiệm thu

1. **Độ tin cậy chưa tính lớp đã hoàn thành.** `ApplicationStatus` có `ACCEPTED`, `CONFIRMED`, `FINISHED`, nhưng `MatchContextFactory.countAccepted` đang chỉ đếm `ACCEPTED`. Cần đếm cả ba trạng thái: đổi query trong `ApplicationRepository` thành `a.applicationStatus in :statuses` với tham số `Collection<ApplicationStatus>`, và truyền danh sách ba trạng thái từ `MatchContextFactory`. Chưa sửa thì `tutor4` trong dữ liệu test không đạt điểm tin cậy cao.
2. **`TutorReindexRunner` cần `@Order(2)`** (kèm import `org.springframework.core.annotation.Order`).
3. **`TutorServiceImpl` (hoặc service cập nhật hồ sơ gia sư) phải publish `TutorProfileChangedEvent`** sau khi lưu, nếu không gia sư sửa hồ sơ sẽ không được index lại.
4. **`@EnableAsync`** phải có trên một class cấu hình.

### 8.2. Cần xác nhận với mã nguồn thực tế

Các điểm sau được viết dựa trên suy đoán vì chưa xem file gốc:

- Tên field `bio` trong `Tutor` và việc nó có cho phép `null` hay không (dữ liệu test có `match.tutor5` với `bio = null`; nếu cột bắt buộc, điền một câu ngắn).
- Tên field trạng thái trong entity `Application` (query dùng `applicationStatus`).
- Các giá trị mặc định trong `MatchingProperties` (`min-votes`, `global-avg-rating`, `penalty.*`, `price.*`, `weeks-per-month`) là giá trị dựng lại, cần đối chiếu với file cấu hình cũ.
- `BudgetUnit` có đủ `PER_HOUR`, `PER_SESSION`, `PER_MONTH`; `ExpectedFeeResolver` coi đơn vị khác là thiếu dữ liệu.
- Hành vi thực tế của `AreaMatcher` và `AvailabilityChecker`.

### 8.3. Hạng mục chưa làm

- **Tín hiệu khu vực theo bậc** (cùng phường, cùng tỉnh, khác tỉnh) chưa có. Hiện khu vực chỉ tác động qua cảnh báo `AREA_MISMATCH`. Có thể thêm bằng một `MatchSignal` mới mà không sửa code cũ.
- **Test tự động**: nên có `WeightedScorerTest` (tín hiệu vắng mặt thì chuẩn hóa đúng), `VectorStoreSemanticMatcherTest` (mock `VectorStore`, kiểm tra co giãn điểm) và một test tích hợp dùng Testcontainers pgvector để kiểm tra "math" và "toán" ra cùng gia sư.
- **Dọn dẹp**: xóa hẳn `TutorMatchingAiService` và bản cài đặt, `KeywordMatcher`, `SynonymNormalizer`, `SynonymProperties`, `MatchBreakdown`, khóa `ai-weights` trong cấu hình và các test liên quan tới AI cũ.
- **Frontend**: cập nhật type `breakdown` và giao diện hiển thị lý do xếp hạng.

### 8.4. Vấn đề có sẵn trong dữ liệu gốc

File `seed-data.json` gốc có 14 cặp (bài đăng, gia sư) bị trùng ở bảng application. Phần này không do thay đổi lần này gây ra, nhưng sẽ lỗi nếu sau này thêm ràng buộc unique.

## 9. Rủi ro và vận hành

| Rủi ro | Mức độ | Cách xử lý |
| --- | --- | --- |
| Ollama hoặc pgvector lỗi lúc xếp hạng | Thấp | Đã có fallback: bỏ tín hiệu `semantic`, xếp hạng bằng các tiêu chí luật, chỉ ghi log cảnh báo |
| Gia sư chưa được index | Trung bình | Mất tín hiệu `semantic` cho riêng gia sư đó; chạy `TutorReindexRunner` để bù |
| Đổi mô hình embedding | Cao | Phải đổi `dimensions`, xóa bảng vector và index lại toàn bộ gia sư |
| `floor` và `ceiling` không hợp dữ liệu thật | Trung bình | Chạy vài bài thật, xem phân bố cosine thô rồi chỉnh trong cấu hình |
| Bio gia sư quá ngắn hoặc trống | Trung bình | Vector chỉ dựa vào môn và cấp học; nên khuyến khích gia sư điền bio |
| Độ trễ khi gọi embedding cho mỗi lần xếp hạng | Thấp | Chỉ embed một câu truy vấn mỗi lần; nếu cần có thể cache theo mã bài đăng |
| Phụ thuộc phiên bản Spring AI | Trung bình | Tên khóa cấu hình và artifact khác nhau giữa các phiên bản; ghi lại phiên bản đang dùng khi nghiệm thu |

## 10. Danh sách kiểm tra bàn giao

- [ ] Đã có PostgreSQL với pgvector và Ollama với `bge-m3`
- [ ] `application.properties` đã có đủ các khóa ở mục 5.2, đã thêm `spring.ai.model.chat`
- [ ] Ứng dụng khởi động không còn lỗi hai bean `ChatModel`
- [ ] Đã áp dụng các sửa ở mục 8.1
- [ ] Đã xóa các class AI cũ và từ điển đồng nghĩa
- [ ] Đã nạp `seed-data.json` và chạy index một lần
- [ ] Bài A trả `ranked = true`, 5 phần tử trong `top`, 8 trong `others`
- [ ] Bài C trả `ranked = false`, đủ 3 ứng viên
- [ ] Bài B: `match.tutor14` và `match.tutor15` nằm trong nhóm đầu
- [ ] Tắt `matching.semantic.reindex-on-startup` sau lần index đầu
- [ ] Frontend đã cập nhật type và phần hiển thị breakdown
- [ ] Đã viết các test ở mục 8.3
