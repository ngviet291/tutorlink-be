# Lọc phường/xã + Vector Search cho Chatbot TutorLink

Đường dẫn BE tính từ:

```text
src/main/java/org/group3/tutorlink/features/
```

Các class dùng chung nằm dưới:

```text
src/main/java/org/group3/tutorlink/common/
```

FE tính từ:

```text
src/
```

---

# Tổng quan

Phần này gồm 2 phần:

- **Phần A:** thêm lọc `phường/xã` cho trang **Tìm Lớp** qua `GET /v1/posts`.
- **Phần B:** bổ sung **Vector Search** cho chatbot để tìm bài đăng theo ngữ nghĩa.

Làm **Phần A trước** vì độc lập với Vector Search. Sau khi A chạy ổn mới làm B.

---

# PHẦN A — Lọc theo phường/xã ở trang Tìm Lớp

## Hiện trạng

Entity `Address` đã có:

```text
province
ward
street
```

`PostForm` lấy tên phường/xã trực tiếp từ API địa chỉ nên giá trị lưu trong DB tương ứng với giá trị hiển thị ở dropdown FE.

Vì vậy với API tìm kiếm bài đăng thông thường, có thể lọc bằng:

```java
lower(ward) LIKE %ward%
```

Không cần bỏ dấu hoặc dùng `unaccent`.

> **Lưu ý:** Đây là logic của API `GET /v1/posts`, khác với chatbot. Chatbot có thể nhận `"Tân Phong"` trong khi DB lưu `"Phường Tân Phong"`, nên chatbot vẫn cần `LocationNormalizationService`.

---

## A1 — BE: `post/specification/PostSpecification.java`

Đường dẫn:

```text
src/main/java/org/group3/tutorlink/features/post/specification/PostSpecification.java
```

Đã có:

```java
public static Specification<Post> provinceContains(String province) {
    return (root, query, cb) -> !StringUtils.hasText(province) ? null
            : cb.like(
                    cb.lower(root.get("address").<String>get("province")),
                    like(province)
            );
}
```

Thêm ngay bên dưới:

```java
public static Specification<Post> wardContains(String ward) {
    return (root, query, cb) -> !StringUtils.hasText(ward) ? null
            : cb.like(
                    cb.lower(root.get("address").<String>get("ward")),
                    like(ward)
            );
}
```

Không cần thêm:

```java
unaccent(...)
Normalizer
normalizeLocationName(...)
```

vào `PostSpecification` cho phần A.

Lý do: dữ liệu `ward` của bài đăng và dữ liệu dropdown FE đều lấy từ cùng API địa chỉ.

---

# A2 — BE: `PostController.java`

Đường dẫn:

```text
src/main/java/org/group3/tutorlink/features/post/controller/PostController.java
```

Trong hàm:

```java
getAllPosts(...)
```

Thêm param `ward` ngay sau `province`.

Từ:

```java
@RequestParam(required = false) String province,
```

thành:

```java
@RequestParam(required = false) String province,
@RequestParam(required = false) String ward,
```

Sau đó truyền `ward` vào `postService.getAllPosts(...)` ngay sau `province`.

```java
.data(postService.getAllPosts(
        keyword,
        type,
        subjectName,
        province,
        ward,
        teachingMode,
        educationLevel,
        maxBudget,
        minBudget,
        authorId,
        cursor,
        limit
))
```

---

# A3 — BE: `PostService.java`

Đường dẫn:

```text
src/main/java/org/group3/tutorlink/features/post/service/PostService.java
```

Cập nhật method:

```java
CursorResponse<PostResponse> getAllPosts(
        String keyword,
        PostType type,
        String subjectName,
        String province,
        String ward,
        TeachingMode teachingMode,
        EducationLevel educationLevel,
        BigDecimal maxBudget,
        BigDecimal minBudget,
        UUID authorId,
        UUID cursor,
        int limit
);
```

`ward` phải nằm ngay sau `province` để thống nhất thứ tự với Controller và Implementation.

---

# A4 — BE: `PostServiceImpl.java`

Đường dẫn:

```text
src/main/java/org/group3/tutorlink/features/post/service/impl/PostServiceImpl.java
```

Trong method:

```java
getAllPosts(...)
```

đã thêm:

```java
String ward,
```

ngay sau:

```java
String province,
```

Trong `Specification`, thêm:

```java
.and(PostSpecification.wardContains(ward))
```

Đoạn đầy đủ:

```java
Specification<Post> spec = Specification
        .where(PostSpecification.keyword(keyword))
        .and(PostSpecification.hasStatus(PostStatus.PUBLISHED))
        .and(PostSpecification.hasType(type))
        .and(PostSpecification.hasSubject(subjectName))
        .and(PostSpecification.provinceContains(province))
        .and(PostSpecification.wardContains(ward))
        .and(PostSpecification.hasTeachingMode(teachingMode))
        .and(PostSpecification.hasEducationLevel(educationLevel))
        .and(PostSpecification.maxBudgetLessThanOrEqual(maxBudget))
        .and(PostSpecification.minBudgetGreaterThanOrEqual(minBudget))
        .and(PostSpecification.hasAuthorId(authorId))
        .and(PostSpecification.fetchSubject())
        .and(PostSpecification.cursor(cursor));
```

---

# A5 — FE: `features/post/types/post.types.ts`

Đường dẫn:

```text
src/features/post/types/post.types.ts
```

Thêm `ward` vào `PostFilter`:

```ts
export interface PostFilter {
  keyword?: string;
  type?: PostType;
  subjectName?: string;
  province?: string;
  ward?: string;
  teachingMode?: TeachingMode | "";

  // giữ nguyên các field còn lại
}
```

Với filter bài đăng của chính user:

```ts
export interface MyPostFilter
  extends Omit<PostFilter, "authorId" | "province" | "ward"> {
  postStatus?: PostStatus;
}
```

Với admin:

```ts
export type AdminPostFilter = Omit<
  PostFilter,
  "authorId" | "province" | "ward"
> & {
  status?: PostStatus | "";
};
```

Lý do loại `ward` khỏi `MyPostFilter` và `AdminPostFilter`:

- `GET /v1/posts` public có lọc tỉnh/phường.
- API bài của chính user không dùng filter location này.
- API admin hiện tại cũng không dùng filter location này.

---

# A6 — FE: `features/post/mapper/post.mapper.ts`

Đường dẫn:

```text
src/features/post/mapper/post.mapper.ts
```

Nếu `province` đã được thêm ở mục 1.9 thì chỉ cần thêm:

```ts
"ward"
```

Ví dụ:

```ts
export const LIST_KEYS = [
  ...COMMON,
  "authorId",
  "province",
  "ward",
] as const;
```

Không thay đổi các key khác.

---

# A7 — FE: `shared/components/ui/FilterSidebar.tsx`

Đường dẫn:

```text
src/shared/components/ui/FilterSidebar.tsx
```

## A7.1 — Thêm `ward`

```ts
export interface FilterValues {
  subjectId: string;
  province: string;
  ward?: string;
  teachingMode: TeachingMode | "";
  educationLevels: EducationLevel[];
  min: number;
  max: number;
}
```

Để tránh phá các nơi đang tạo `FilterValues`, `ward` nên là optional.

---

## A7.2 — `emptyFilter`

```ts
export function emptyFilter(rangeMax: number): FilterValues {
  return {
    subjectId: "",
    province: "",
    ward: "",
    teachingMode: "",
    educationLevels: [],
    min: 0,
    max: rangeMax,
  };
}
```

---

## A7.3 — Import `useWards`

Đổi:

```ts
import { useProvinces } from "@/shared/hooks/useAddress";
```

thành:

```ts
import {
  useProvinces,
  useWards,
} from "@/shared/hooks/useAddress";
```

---

## A7.4 — Tìm code tỉnh

Sau khi có:

```ts
const { data: provinces = [] } = useProvinces();
```

lấy code của tỉnh đang chọn:

```ts
const provinceCode = provinces.find(
  (p) => p.name === value.province
)?.code;
```

Sau đó:

```ts
const {
  data: wards = [],
  isFetching: wardsLoading,
} = useWards(provinceCode);
```

---

## A7.5 — Khi đổi tỉnh phải xóa phường

Từ:

```tsx
onChange={(e) =>
  set({
    province: e.target.value,
  })
}
```

thành:

```tsx
onChange={(e) =>
  set({
    province: e.target.value,
    ward: "",
  })
}
```

Việc này bắt buộc để tránh tình trạng:

```text
Tỉnh A
Phường X
```

sau đó user đổi thành:

```text
Tỉnh B
Phường X
```

trong khi `Phường X` thuộc tỉnh A.

---

## A7.6 — Thêm dropdown Phường/Xã

Đặt ngay sau dropdown tỉnh:

```tsx
{!hideProvince && value.province && (
  <div>
    <label className="mb-1 block text-xs font-semibold text-text-secondary">
      Phường / Xã
    </label>

    <select
      className="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm text-text focus:outline-none focus:ring-2 focus:ring-primary"
      value={value.ward ?? ""}
      disabled={wardsLoading}
      onChange={(e) =>
        set({
          ward: e.target.value,
        })
      }
    >
      <option value="">
        {wardsLoading
          ? "Đang tải..."
          : "Tất cả phường/xã"}
      </option>

      {wards.map((w) => (
        <option
          key={w.code}
          value={w.name}
        >
          {w.name}
        </option>
      ))}
    </select>
  </div>
)}
```

Nếu `WardResponse` không có `code`:

```tsx
key={w.name}
```

---

# A8 — FE: `pages/FindClassPage.tsx`

Đường dẫn:

```text
src/pages/FindClassPage.tsx
```

Trong object `query`, thêm:

```ts
province: filter.province || undefined,
ward: filter.ward || undefined,
```

Ví dụ:

```ts
const query = {
  keyword: filter.keyword || undefined,
  subjectName: filter.subjectName || undefined,
  province: filter.province || undefined,
  ward: filter.ward || undefined,

  // các field khác
};
```

---

## A8.1 — `hasSearch`

Thêm:

```ts
filter.ward ||
```

Ví dụ:

```ts
const hasSearch =
  filter.subjectId ||
  filter.province ||
  filter.ward ||
  filter.teachingMode ||
  filter.educationLevels.length > 0 ||
  filter.min > 0 ||
  filter.max < rangeMax;
```

---

## A8.2 — Search tracking

Nếu `useSearchTracking` đang lưu filter:

```ts
filters: {
  // ...
  province: query.province,
  ward: query.ward,
}
```

---

# A9 — Kiểm tra Phần A

Chạy backend và frontend.

Sau đó:

### Case 1 — Không chọn phường

Request:

```text
GET /v1/posts?province=Thành%20phố%20Hồ%20Chí%20Minh
```

Kết quả:

```text
Các bài trong TP. Hồ Chí Minh
```

---

### Case 2 — Chọn phường

Ví dụ:

```text
province = Thành phố Hồ Chí Minh
ward = Phường Tân Phong
```

Request phải có:

```text
GET /v1/posts?province=Thành%20phố%20Hồ%20Chí%20Minh&ward=Phường%20Tân%20Phong
```

Kết quả chỉ gồm bài có:

```text
address.ward = Phường Tân Phong
```

---

### Case 3 — Đổi tỉnh

Ví dụ:

```text
TP. Hồ Chí Minh
Phường Tân Phong
```

đổi thành:

```text
Hà Nội
```

thì FE phải tự reset:

```text
ward = ""
```

và dropdown trở lại:

```text
Tất cả phường/xã
```

---

# PHẦN B — Vector Search cho Chatbot

## Mục tiêu

Chatbot hiện tại đã có:

```text
VectorStoreTutorIndexer
VectorStoreSemanticMatcher
```

và đã sử dụng:

```text
tutor_vector_store
```

cho matching gia sư.

Phần B **không tạo VectorStore mới**.

Tiếp tục sử dụng:

```text
tutor_vector_store
```

và phân biệt loại document bằng metadata:

```text
kind = TUTOR
```

hoặc:

```text
kind = POST
```

---

# B1 — Nguyên tắc dữ liệu Vector

Document cho bài đăng phải có metadata:

```text
kind = POST
postId = <UUID>
subjectName = <tên môn>
postType = FIND_STUDENT hoặc FIND_TUTOR
province = <tỉnh>
ward = <phường/xã>
```

Ví dụ:

```json
{
  "kind": "POST",
  "postId": "01a17bc4-d2b8-7451-b70d-97aefbf9fb49",
  "subjectName": "Toán",
  "postType": "FIND_STUDENT",
  "province": "Thành phố Hồ Chí Minh",
  "ward": "Phường Tân Phong"
}
```

Metadata `kind=POST` dùng để chatbot không lấy nhầm vector của tutor.

---

# B2 — Nội dung embedding của bài đăng

Không chỉ embedding title.

Nên ghép các thông tin có ý nghĩa tìm kiếm:

```text
Môn học: Toán
Tiêu đề: Gia sư Toán nhận dạy Tiểu học khu vực Phường Tân Phong
Nội dung: ...
Đối tượng: Tiểu học
Hình thức: ...
Địa điểm: Phường Tân Phong, Thành phố Hồ Chí Minh
```

Mục tiêu là câu:

```text
tìm gia sư Toán cho con yếu tư duy cần cô kiên nhẫn
```

có thể tìm được bài có nội dung tương tự dù bài đăng không chứa chính xác câu:

```text
yếu tư duy
cô kiên nhẫn
```

---

# B3 — Index bài đăng

Khi bài đăng được:

```text
CREATE
UPDATE
```

cần cập nhật vector tương ứng.

Metadata tối thiểu:

```text
kind = POST
postId = post.getId()
subjectName = post.getSubject().getName()
postType = post.getType().name()
province = post.getAddress().getProvince()
ward = post.getAddress().getWard()
```

---

# B4 — Không chặn transaction tạo bài

Embedding Ollama:

```text
bge-m3
```

không nên làm cho transaction tạo bài phải chờ vector.

Luồng nên là:

```text
User tạo bài
      |
      v
Save Post
      |
      v
Commit transaction
      |
      v
Index Vector
      |
      v
tutor_vector_store
```

Nếu embedding lỗi:

```text
Post vẫn được tạo thành công
```

và log cảnh báo:

```text
[POST_VECTOR]
```

---

# B5 — Chatbot search theo 2 tầng

Chatbot không nên bỏ search SQL hiện tại.

Luồng đúng:

```text
User message
      |
      v
AI trích filters
      |
      +---- subjectName
      +---- province
      +---- area
      +---- postType
      +---- educationLevel
      |
      v
PostSearcher
      |
      +---- SQL exact location
      |
      +---- SQL cùng tỉnh
      |
      +---- Vector semantic fallback
      |
      v
PostSummaryResponse
```

SQL vẫn là tầng ưu tiên để đảm bảo:

```text
subject
province
ward
postType
status
deadline
```

được kiểm soát chính xác.

Vector Search chỉ dùng để tìm bài **gần nghĩa** khi search thông thường không đủ.

---

# B6 — Reindex

Cần có cơ chế reindex toàn bộ bài đăng.

Sau khi chạy reindex:

```sql
select count(*)
from tutor_vector_store
where metadata->>'kind' = 'POST';
```

phải có dữ liệu.

Không bắt buộc số vector phải bằng tổng số bài trong DB nếu có các bài:

```text
DRAFT
REMOVED
CLOSED
expired
```

không được index.

---

# B7 — Semantic matching

Khi SQL không tìm được bài phù hợp về mặt location hoặc nội dung quá ít, chatbot có thể gọi:

```text
VectorStoreSemanticMatcher
```

với:

```text
kind = POST
```

để tránh lấy nhầm vector tutor.

Ví dụ query:

```text
tìm gia sư Toán cho con yếu tư duy cần cô kiên nhẫn
```

Vector search có thể tìm các bài có nội dung gần nghĩa.

---

# B8 — Filter metadata sau Vector Search

Vector search không được phép phá các filter business quan trọng.

Đặc biệt:

```text
postType
province
```

phải được giữ.

Ví dụ user:

```text
Tìm gia sư Toán ở Tân Phong TPHCM
```

thì không được vì semantic similarity cao mà trả:

```text
FIND_TUTOR
```

hoặc bài ở tỉnh khác.

Thứ tự ưu tiên:

```text
postType
    ↓
province
    ↓
ward
    ↓
semantic similarity
```

---

# B9 — `min-score`

`min-score` chỉ dùng cho semantic fallback.

Có thể bắt đầu:

```text
0.45
```

Nếu fallback trả quá nhiều bài lạc đề:

```text
0.50
```

hoặc:

```text
0.55
```

Nếu quá ít:

```text
0.40
```

Không nên dùng vector score để thay thế điều kiện:

```text
province
postType
status
deadline
```

---

# B10 — LocationNormalizationService cho Chatbot

## Đường dẫn

```text
src/main/java/org/group3/tutorlink/common/service/LocationNormalizationService.java
```

Service này phục vụ **chatbot**, không phải filter `GET /v1/posts`.

Mục tiêu:

```text
"Tân Phong"
"Phường Tân Phong"
"P. Tân Phong"
```

đều được xem là cùng một phường.

Class:

```java
package org.group3.tutorlink.common.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class LocationNormalizationService {

    private static final Pattern ACCENT_MARKS =
            Pattern.compile("\\p{M}+");

    /*
     * Chạy SAU khi bỏ dấu + viết thường.
     *
     * Ví dụ:
     * "phuong tan phong"
     * "xa my hoa"
     * "p. tan phong"
     * "p.12"
     * "p 12"
     */
    private static final Pattern WARD_PREFIX =
            Pattern.compile(
                    "^(?:(?:phuong|xa|thi tran|quan|huyen)\\s+"
                            + "|(?:p|x|tt|q|h)\\.\\s*"
                            + "|(?:p|q)\\s+(?=\\d))"
            );

    /**
     * Chuẩn hóa tên phường/xã để so sánh.
     *
     * "Tân Phong"          -> "tan phong"
     * "Phường Tân Phong"   -> "tan phong"
     * "P. Tân Phong"       -> "tan phong"
     * "Phường 12"          -> "12"
     */
    public String normalizeWardForSearch(String value) {
        String text = normalizeLocationText(value);

        return text == null
                ? null
                : WARD_PREFIX
                    .matcher(text)
                    .replaceFirst("")
                    .trim();
    }

    /**
     * So sánh hai tên phường/xã:
     * - không phân biệt dấu
     * - không phân biệt hoa thường
     * - không phân biệt tiền tố Phường/Xã/P./X.
     */
    public boolean isSameWard(String a, String b) {
        String first = normalizeWardForSearch(a);

        return first != null
                && first.equals(normalizeWardForSearch(b));
    }

    public String normalizeLocationText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        return removeAccents(value)
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
    }

    private String removeAccents(String value) {
        String decomposed =
                Normalizer.normalize(
                        value,
                        Normalizer.Form.NFD
                );

        return ACCENT_MARKS
                .matcher(decomposed)
                .replaceAll("")
                .replace('đ', 'd')
                .replace('Đ', 'D');
    }
}
```

---

# B11 — `PostSearcher` và location chatbot

## Đường dẫn

```text
src/main/java/org/group3/tutorlink/features/chatbot/service/PostSearcher.java
```

Chatbot dùng:

```java
private final LocationNormalizationService locationNormalizer;
```

Khi AI trả:

```json
{
  "province": "TP. Hồ Chí Minh",
  "area": "Tân Phong"
}
```

`PostSearcher` xử lý:

```text
province
    ↓
AddressService.normalize(...)
    ↓
Thành phố Hồ Chí Minh

area
    ↓
LocationNormalizationService
    ↓
tan phong
```

Sau đó so sánh với DB:

```text
Phường Tân Phong
    ↓
tan phong
```

nên:

```text
Tân Phong == Phường Tân Phong
```

---

# B12 — Luồng PostSearcher hiện tại

`PostSearcher` phải giữ nguyên nguyên tắc:

```text
1. Tìm đúng phường/xã + đúng tỉnh + đúng postType
2. Nếu không có → tìm các bài cùng tỉnh + đúng postType
3. Không được fallback sang tỉnh khác
4. Không được fallback sang postType khác
```

Ví dụ:

```text
User:
Tìm gia sư Toán tại Tân Phong TPHCM
```

AI:

```text
subjectName = Toán
province = TP. Hồ Chí Minh
area = Tân Phong
postType = FIND_STUDENT
```

DB:

```text
province = Thành phố Hồ Chí Minh
ward = Phường Tân Phong
postType = FIND_STUDENT
```

Kết quả:

```text
match exact area
```

vì:

```text
LocationNormalizationService.isSameWard(
    "Phường Tân Phong",
    "Tân Phong"
)
== true
```

---

# B13 — Không dùng fallback sai postType

Tuyệt đối không khôi phục logic kiểu:

```java
if (posts.isEmpty()) {
    posts = find(subjectOnly, null);
}
```

Cách này có thể khiến:

```text
FIND_STUDENT
```

bị fallback thành:

```text
FIND_TUTOR
```

và chatbot trả bài không đúng nhu cầu.

PostType phải được giữ xuyên suốt search.

---

# B14 — Prompt Chatbot

Prompt phải tiếp tục phân biệt:

```text
FIND_STUDENT
=
Tutor tạo bài và đang tìm học sinh / cơ hội dạy
```

và:

```text
FIND_TUTOR
=
Student tạo bài và đang tìm gia sư
```

Do business của TutorLink hiện tại:

```text
"Tìm gia sư Toán"
→ FIND_STUDENT
```

```text
"Cần người dạy kèm Lý"
→ FIND_STUDENT
```

```text
"Tìm học sinh học Toán"
→ FIND_TUTOR
```

```text
"Tìm lớp dạy Toán"
→ FIND_TUTOR
```

Không được đảo lại mapping này.

---

# B15 — Prompt location

Prompt chỉ cần yêu cầu AI trích location theo ngôn ngữ tự nhiên của user.

Ví dụ:

```text
"Tìm gia sư Toán tại Tân Phong TPHCM"
```

AI trả:

```json
{
  "province": "TP. Hồ Chí Minh",
  "area": "Tân Phong"
}
```

Không bắt buộc AI phải tự thêm:

```text
Phường
```

vào `area`.

Việc chuẩn hóa:

```text
Tân Phong
↓
tan phong
```

và:

```text
Phường Tân Phong
↓
tan phong
```

do backend xử lý.

Điều này tránh phụ thuộc vào cách AI viết location.

---

# B16 — Kiểm tra toàn bộ Chatbot

## Case 1 — Đúng phường

User:

```text
Tìm gia sư Toán tại Tân Phong TPHCM
```

Expected:

```text
subjectName = Toán
province = TP. Hồ Chí Minh
area = Tân Phong
postType = FIND_STUDENT
```

DB:

```text
province = Thành phố Hồ Chí Minh
ward = Phường Tân Phong
type = FIND_STUDENT
```

Expected:

```text
strict match
```

---

## Case 2 — Không có bài trong phường

Ví dụ:

```text
Tìm gia sư Vật lý tại Tân Phong TPHCM
```

Nếu không có bài:

```text
Phường Tân Phong
```

nhưng có bài:

```text
Phường 7
Phường 10
Phường Tân Quy
```

trong:

```text
Thành phố Hồ Chí Minh
```

thì được fallback:

```text
same province
same subject
same postType
```

và:

```text
relaxed = true
```

Message có thể nói:

```text
Mình chưa tìm thấy bài đăng ở Tân Phong, nhưng có một số bài phù hợp ở khu vực khác trong TP. Hồ Chí Minh.
```

Không nói:

```text
các bài cùng môn học
```

nếu backend thực tế đang fallback theo tỉnh.

---

# B17 — Vector fallback

Nếu SQL không tìm được bài phù hợp và semantic search được bật:

```text
SQL search
    ↓
0 kết quả
    ↓
Vector Search
    ↓
metadata.kind = POST
    ↓
lọc postType/province
    ↓
min-score
    ↓
PostSummaryResponse
```

Ví dụ:

```text
User:
tìm gia sư Toán cho con yếu tư duy cần cô kiên nhẫn
```

Vector search có thể tìm bài:

```text
Gia sư Toán tiểu học, kiên nhẫn, hỗ trợ học sinh mất gốc
```

dù không có đúng từ:

```text
yếu tư duy
```

---

# B18 — Khi Ollama tắt

Nếu Ollama không chạy:

```text
Vector Search
```

có thể lỗi.

Không được làm chatbot chết hoàn toàn.

Cần:

```text
try/catch
```

và fallback về search SQL hiện tại.

Log:

```text
[CHATBOT SEMANTIC] Vector search unavailable, fallback to SQL search
```

Kết quả vẫn phải trả được nếu SQL có dữ liệu.

---

# B19 — Kiểm tra Vector Store

Sau khi reindex:

```sql
select count(*)
from tutor_vector_store
where metadata->>'kind' = 'POST';
```

Kiểm tra metadata:

```sql
select
    metadata->>'kind' as kind,
    metadata->>'postId' as post_id,
    metadata->>'subjectName' as subject_name,
    metadata->>'postType' as post_type,
    metadata->>'province' as province,
    metadata->>'ward' as ward
from tutor_vector_store
where metadata->>'kind' = 'POST'
limit 20;
```

Phải thấy dạng:

```text
POST | UUID | Toán | FIND_STUDENT | Thành phố Hồ Chí Minh | Phường Tân Phong
```

---

# B20 — Test cuối

## Test A — FE filter

```text
Tỉnh:
Thành phố Hồ Chí Minh

Phường:
Phường Tân Phong
```

Network:

```text
GET /v1/posts?province=...&ward=...
```

Expected:

```text
Chỉ bài Phường Tân Phong
```

---

## Test B — Chatbot exact ward

```text
Tìm gia sư Toán tại Tân Phong TPHCM
```

Expected:

```text
FIND_STUDENT
Tân Phong
Thành phố Hồ Chí Minh
```

và match:

```text
Phường Tân Phong
```

---

## Test C — Chatbot fallback

```text
Tìm gia sư Vật lý tại Tân Phong TPHCM
```

Nếu không có bài ở Tân Phong nhưng có bài ở phường khác cùng TP:

```text
relaxed = true
```

Không được đổi:

```text
FIND_STUDENT
```

thành:

```text
FIND_TUTOR
```

---

## Test D — Semantic

```text
tìm gia sư Toán cho con yếu tư duy cần cô kiên nhẫn
```

Nếu có bài semantic gần nghĩa:

```text
Vector Search
→ kind = POST
→ đúng postType
→ đúng province
→ score >= min-score
```

thì bài gần nghĩa đứng trước.

---

# Thứ tự triển khai khuyến nghị

Làm đúng thứ tự:

```text
A1  PostSpecification.wardContains()
 ↓
A2  PostController
 ↓
A3  PostService
 ↓
A4  PostServiceImpl
 ↓
A5  PostFilter
 ↓
A6  LIST_KEYS
 ↓
A7  FilterSidebar
 ↓
A8  FindClassPage
 ↓
A9  Test GET /v1/posts
 ↓
B10 LocationNormalizationService
 ↓
B11 PostSearcher location matching
 ↓
B12 kiểm tra postType/fallback
 ↓
B13-B15 chatbot prompt + search rule
 ↓
B16 test chatbot location
 ↓
B1-B9 Vector indexing/search
 ↓
B17-B20 semantic fallback + test
```

---

# Lưu ý quan trọng

### 1. `GET /v1/posts` và Chatbot là hai luồng khác nhau

API public:

```text
province + ward
```

dùng giá trị canonical từ FE/API địa chỉ.

Chatbot:

```text
area
```

có thể là:

```text
Tân Phong
Gò Vấp
P.12
Phường 12
```

nên chatbot cần:

```text
LocationNormalizationService
```

---

### 2. Không đưa normalization chatbot vào `PostSpecification`

Không nên biến:

```java
wardContains(...)
```

thành một hàm xử lý quá nhiều trường hợp AI.

Giữ:

```java
wardContains()
```

đơn giản cho API public.

Để:

```java
LocationNormalizationService
```

xử lý input tự nhiên của chatbot.

---

### 3. Không hardcode danh sách phường/xã

Không viết:

```java
if ("Tân Phong".equals(area)) ...
```

hoặc:

```java
Map<String, String> WARDS = ...
```

Location phải lấy từ dữ liệu địa chỉ/API hiện có.

---

### 4. Không fallback toàn quốc

Nếu user nói:

```text
Tìm gia sư Toán ở Tân Phong TPHCM
```

mà không có bài:

```text
Tân Phong
```

thì chỉ fallback:

```text
TP. Hồ Chí Minh
```

Không được tự động tìm:

```text
Hà Nội
Đà Nẵng
Bình Dương
```

---

### 5. Không đổi postType khi fallback

Nếu AI xác định:

```text
postType = FIND_STUDENT
```

thì tất cả các tầng search phải giữ:

```text
FIND_STUDENT
```

Vector Search cũng phải lọc:

```text
postType = FIND_STUDENT
```

---

### 6. Vector Search không thay thế SQL filter

Vector Search dùng để bổ sung khả năng tìm theo nghĩa.

Các điều kiện business bắt buộc vẫn phải được đảm bảo:

```text
PUBLISHED
not expired
subject
postType
province
```

`ward` có thể được dùng làm điều kiện ưu tiên hoặc exact filter tùy tầng search.

---

# Kết quả kiến trúc cuối

```text
                    USER
                     |
                     v
              Chatbot / FindClass
                     |
          +----------+----------+
          |                     |
          v                     v
      GET /v1/posts         Chatbot
          |                     |
          v                     v
 province + ward          AI filters
                                |
                                v
                         PostSearcher
                                |
                    +-----------+-----------+
                    |                       |
                    v                       v
               SQL Search              Vector Search
                    |                       |
                    |                 kind = POST
                    |                       |
                    +-----------+-----------+
                                |
                                v
                       PostSummaryResponse
```

Mục tiêu cuối cùng:

```text
"Tân Phong"
        ↓
LocationNormalizationService
        ↓
"tan phong"
        ↓
"Phường Tân Phong"
        ↓
đúng bài đăng
```

và:

```text
"tìm gia sư Toán"
        ↓
postType = FIND_STUDENT
        ↓
SQL exact search
        ↓
không có
        ↓
same province
        ↓
không có / cần semantic
        ↓
Vector Search
        ↓
kind = POST
        ↓
đúng postType + province
```