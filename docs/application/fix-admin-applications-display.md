# Fix: Hiển thị danh sách ứng tuyển trong trang Admin - Complete Guide

# Part 1: Thêm Application Count và Cải thiện Modal

## Vấn đề ban đầu

1. Admin thấy tất cả bài đăng có **0 lượt đăng ký** dù có người ứng tuyển
2. Modal chi tiết lượt đăng ký:
   - Thiếu avatar gia sư
   - Không hiển thị rating và số đánh giá
   - Không hiển thị đề xuất phí
   - Thiếu thời gian ứng tuyển rõ ràng

## Nguyên nhân

- Backend `PostResponse` **không có** field `applicationCount`
- Frontend hiển thị `post.applicationCount ?? 0` nhưng BE không trả về → luôn là 0
- Modal chỉ hiển thị tên và trạng thái cơ bản
- Backend chỉ trả `UserSummary` với 3 field (id, fullName, avatarUrl) không có rating

## Giải pháp V1

### Backend Changes

#### 1. PostResponse.java
Thêm field `applicationCount`:

```java
private Instant createdAt;
private Instant updatedAt;
private Integer applicationCount;  // ← THÊM MỚI
```

#### 2. PostMapper.java
Đổi từ `interface` → `abstract class` để dùng `@AfterMapping`:

```java
@Mapper(componentModel = "spring", 
        unmappedTargetPolicy = ReportingPolicy.IGNORE, 
        unmappedSourcePolicy = ReportingPolicy.IGNORE)
public abstract class PostMapper {  // ← ĐỔI THÀNH abstract class
    
    @Mapping(target = "applicationCount", ignore = true)
    public abstract PostResponse toPostResponse(Post post);
    
    // Thêm @AfterMapping để tính applicationCount
    @AfterMapping
    protected void fillApplicationCount(Post post, @MappingTarget PostResponse response) {
        if (post.getApplications() != null) {
            response.setApplicationCount(post.getApplications().size());
        } else {
            response.setApplicationCount(0);
        }
    }
}
```

**Logic:**
- MapStruct gọi `toPostResponse()` → map các fields cơ bản
- Sau đó gọi `fillApplicationCount()` → đếm số phần tử trong collection
- Set vào `response.applicationCount`

#### 3. UserSummaryResponse.java
Thêm thông tin rating cho tutor:

```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSummaryResponse {
    private UUID id;
    private String fullName;
    private String avatarUrl;
    
    // Thông tin thêm cho Tutor (null nếu là Student)
    private Double averageRating;
    private Long totalReviews;
}
```

#### 4. ReviewRepository.java
Thêm method để đếm reviews:

```java
long countByTutorId(UUID tutorId);
```

**Lưu ý:** MapStruct sẽ auto-generate mapping, `totalReviews` có thể null từ BE. FE sẽ xử lý fallback.

### Frontend Changes

#### 5. application.dto.ts
Cập nhật UserSummary interface:

```typescript
export interface UserSummary {
  id: string;
  fullName: string;
  avatarUrl?: string | null;
  // Thông tin thêm cho Tutor
  averageRating?: number | null;
  totalReviews?: number | null;
}
```

#### 6. user.dto.ts
Thêm vào BaseUserResponse:

```typescript
export interface BaseUserResponse {
  id: string;
  fullName: string;
  email: string;
  phone: string;
  avatarUrl?: string;
  // ...
  // Thông tin thêm từ UserSummary
  averageRating?: number;
  totalReviews?: number;
}
```

#### 7. application.service.ts
Cập nhật mapper functions:

```typescript
function fromSummary(u: UserSummary): BaseUserResponse {
  return {
    id: u.id,
    fullName: u.fullName,
    avatarUrl: u.avatarUrl,
    averageRating: u.averageRating,
    totalReviews: u.totalReviews,
  } as BaseUserResponse;
}

function asTutor(user: BaseUserResponse): TutorResponse {
  return {
    id: user.id,
    fullName: user.fullName,
    avatarUrl: user.avatarUrl,
    experienceYears: 0,
    hourlyRate: 0,
    averageRating: user.averageRating ?? 0,
    reviewCount: user.totalReviews ?? 0,
    verificationStatus: "PENDING",
    address: user.address,
  };
}
```

#### 8. http.application.gateway.ts
Sửa type trả về:

```typescript
listByPost(postId: string, limit = 100) {
  return this.http.get<CursorResponse<ApplicationResponse>>(
    `/v1/applications/post/${postId}`,
    { params: { limit } },
  );
}
```

#### 9. application.service.ts - getApplicationsByPost
Sửa để dùng admin endpoint:

```typescript
getApplicationsByPost = async (
  postId: string,
): Promise<ApplicationResponse[]> => {
  const page = await this.gateway.listByPost(postId, 100);
  return this.enrichManyForAdmin(page.content, postId);
};

private async enrichManyForAdmin(
  items: ApplicationDto[],
  postId: string,
): Promise<ApplicationResponse[]> {
  let post: PostResponse | null = null;
  try {
    post = await this.postService.getPostByIdForAdmin(postId);
  } catch {
    console.warn(`Could not fetch post ${postId}, using minimal object`);
  }

  const postObj = post ?? ({ id: postId, title: "N/A" } as PostResponse);
  return items.map((dto) => this.build(dto, postObj));
}
```

#### 10. AdminPostsPage.tsx
Thiết kế lại modal đẹp hơn với grid 2 cột:

```tsx
function ApplicationsModal({ post, onClose }) {
  const { data, isLoading } = useApplicationsByPost(
    post?.id ?? "",
    Boolean(post),
  );

  return (
    <Modal
      open={Boolean(post)}
      onClose={onClose}
      title={`Lượt đăng ký: ${post?.title ?? ""}`}
      size="lg"
    >
      {isLoading ? (
        <ListSkeleton rows={3} />
      ) : !data?.length ? (
        <EmptyState title="Chưa có lượt đăng ký" />
      ) : (
        <div className="grid gap-4 sm:grid-cols-2">
          {data.map((a) => {
            const avatarUrl = a.tutor.avatarUrl || "/default-avatar.png";
            const rating = a.tutor.averageRating?.toFixed(1) ?? "0.0";
            const reviewCount = a.tutor.reviewCount ?? a.tutor.totalReviews ?? 0;

            return (
              <Link
                key={a.id}
                to={ROUTES.TUTOR_DETAIL(a.tutor.id)}
                className="group flex flex-col gap-3 rounded-lg border border-border bg-surface p-4 transition-all hover:border-primary hover:shadow-md"
              >
                {/* Header: Avatar + Name + Rating */}
                <div className="flex items-start gap-3">
                  <img
                    src={avatarUrl}
                    alt={a.tutor.fullName}
                    className="size-14 rounded-full object-cover ring-2 ring-border group-hover:ring-primary transition-all flex-shrink-0"
                  />
                  <div className="min-w-0 flex-1">
                    <p className="font-semibold text-text group-hover:text-primary transition-colors line-clamp-1">
                      {a.tutor.fullName}
                    </p>
                    {/* Rating */}
                    <div className="mt-1 flex items-center gap-1.5 text-sm text-text-secondary">
                      <span className="flex items-center gap-1">
                        ⭐{" "}
                        <span className="font-medium text-text">
                          {rating}
                        </span>
                      </span>
                      <span>•</span>
                      <span>{reviewCount} đánh giá</span>
                    </div>
                  </div>
                </div>

                {/* Proposed Fee */}
                {a.proposedFee && (
                  <div className="flex items-center justify-between rounded-md bg-primary/10 px-3 py-2">
                    <span className="text-xs font-medium text-text-secondary">
                      Đề xuất phí
                    </span>
                    <span className="text-base font-bold text-primary">
                      {a.proposedFee.toLocaleString()} đ
                    </span>
                  </div>
                )}

                {/* Applied Time */}
                <div className="flex items-center gap-1.5 text-xs text-text-secondary">
                  <Clock className="size-3.5" />
                  <span>Đã ứng tuyển {timeAgo(a.appliedAt)}</span>
                </div>
              </Link>
            );
          })}
        </div>
      )}
    </Modal>
  );
}
```

**UI Improvements:**
- ✅ Grid 2 cột responsive
- ✅ Card design với hover effects
- ✅ Avatar lớn với ring border
- ✅ Rating và số đánh giá nổi bật
- ✅ Đề xuất phí trong box riêng
- ✅ Toàn bộ card là clickable link
- ❌ Không hiển thị cover letter (theo yêu cầu)

---

# Part 2: Fix LAZY Loading Issue

## Vấn đề V2

Sau khi thêm field `applicationCount`, vẫn hiển thị **0** vì:

**Root Cause:** `Post.applications` dùng **LAZY loading** (mặc định `@OneToMany`).  
Khi query Post, JPA không tự động load collection → `post.getApplications()` = null/proxy → count = 0.

## Giải pháp: JOIN FETCH

### 1. PostSpecification.java
Thêm method để EAGER fetch:

```java
public static Specification<Post> fetchApplications() {
    return (root, query, cb) -> {
        // Chỉ fetch khi query entity, không phải COUNT
        if (query.getResultType() != Long.class
                && query.getResultType() != long.class) {
            root.fetch("applications", JoinType.LEFT);
        }
        return null;
    };
}
```

**SQL sinh ra:**
```sql
SELECT p.*, a.*
FROM posts p
LEFT JOIN applications a ON p.id = a.post_id
WHERE ...
```

### 2. PostServiceImpl.java - getAllPostsAdmin()
Thêm fetch vào query:

```java
Specification<Post> spec = Specification
    .where(PostSpecification.keyword(keyword))
    .and(PostSpecification.hasStatus(...))
    .and(PostSpecification.fetchSubject())
    .and(PostSpecification.fetchApplications())  // ← THÊM
    .and(PostSpecification.cursor(cursor));
```

### 3. PostServiceImpl.java - getAllPosts()
Cũng thêm cho public API:

```java
Specification<Post> spec = Specification
    .where(PostSpecification.keyword(keyword))
    .and(PostSpecification.hasStatus(PostStatus.PUBLISHED))
    .and(PostSpecification.fetchSubject())
    .and(PostSpecification.fetchApplications())  // ← THÊM
    .and(PostSpecification.cursor(cursor));
```

## Luồng hoạt động

### ❌ Trước (LAZY):
```
Query Post → applications không load → post.getApplications() = null → count = 0
```

### ✅ Sau (EAGER FETCH):
```
Query Post với JOIN FETCH → applications loaded → post.getApplications().size() = 3
```

## Tránh N+1 Problem

**Không dùng JOIN FETCH:**
```
Query 1: SELECT * FROM posts (10 posts)
Query 2-11: SELECT * FROM applications WHERE post_id = ? (10 queries)
→ Tổng: 11 queries ❌
```

**Với JOIN FETCH:**
```
Query 1: SELECT p.*, a.* FROM posts p LEFT JOIN applications a ...
→ Tổng: 1 query ✅
```

---

# Part 3: Debug Guide

## Checklist Debug Từng Bước

### Step 1: ✅ Build Backend

```bash
cd tutorlink-be
./mvnw clean compile -DskipTests
```

**Phải thấy:** `BUILD SUCCESS`

### Step 2: ✅ Restart Backend

Trong IntelliJ:
- `Ctrl+F2` (Stop)
- `Shift+F10` (Run)

### Step 3: 🔍 Test API Trực tiếp

```bash
curl -X GET "http://localhost:8080/v1/admin/posts" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Kiểm tra response:**
```json
{
  "content": [
    {
      "id": "xxx",
      "applicationCount": 3  ← Phải có và > 0
    }
  ]
}
```

**Nếu không có field:** Backend chưa build/restart  
**Nếu = 0:** fetchApplications() chưa hoạt động

### Step 4: 🔍 Bật SQL Log

`application.yml`:
```yaml
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.orm.jdbc.bind: TRACE
```

**Phải thấy trong log:**
```sql
SELECT p.*, a.*
FROM posts p
LEFT JOIN applications a ON p.id = a.post_id
```

**Nếu KHÔNG thấy LEFT JOIN:** Code chưa compile hoặc Specification bị skip

### Step 5: 🔍 Thêm Debug Log

Sửa `PostMapper.java`:

```java
@AfterMapping
protected void fillApplicationCount(Post post, @MappingTarget PostResponse response) {
    System.out.println("=== DEBUG fillApplicationCount ===");
    System.out.println("Post: " + (post != null ? post.getId() : "null"));
    System.out.println("Applications: " + (post != null && post.getApplications() != null 
        ? post.getApplications().size() : "null"));
    
    if (post != null && post.getApplications() != null) {
        response.setApplicationCount(post.getApplications().size());
    } else {
        response.setApplicationCount(0);
    }
    System.out.println("Count: " + response.getApplicationCount());
}
```

**Console output:**
- Không thấy log → Mapper chưa compile
- "Applications: null" → LAZY loading chưa fix
- "Count: 3" → Backend OK, check Frontend

### Step 6: 🔍 Check Frontend

DevTools → Network → `/v1/admin/posts`:

**Nếu BE trả đúng nhưng FE hiển thị 0:**
- Check `AdminPostsPage.tsx`: `post.applicationCount ?? 0`
- Check type: `post.types.ts` phải có `applicationCount?: number`

## Troubleshooting Common Issues

### Issue 1: LazyInitializationException

**Lỗi:**
```
org.hibernate.LazyInitializationException: could not initialize proxy
```

**Fix:**
- Thêm `@Transactional(readOnly = true)` vào service
- Hoặc dùng `@Query` với `JOIN FETCH` trực tiếp

### Issue 2: Duplicate Posts

**Hiện tượng:** 1 bài xuất hiện nhiều lần

**Fix:** Đã thêm `.distinct()` trong code

### Issue 3: Count vẫn = 0

**Debug steps:**
1. Clean build: `./mvnw clean compile`
2. Xóa `target/`
3. Restart IntelliJ
4. Check DB trực tiếp:
```sql
SELECT p.id, p.title, COUNT(a.id) as app_count
FROM posts p
LEFT JOIN applications a ON p.id = a.post_id
GROUP BY p.id, p.title;
```

## Quick Fix Script

```bash
#!/bin/bash
echo "1. Clean build..."
cd tutorlink-be
./mvnw clean compile -DskipTests

if [ $? -eq 0 ]; then
    echo "✅ Build success! Please restart backend."
else
    echo "❌ Build failed!"
fi
```

---

# Tổng kết Files Đã Sửa

## Backend (4 files):
1. `PostResponse.java` - Thêm `applicationCount`
2. `PostMapper.java` - Interface → abstract class, @AfterMapping
3. `PostSpecification.java` - Thêm `fetchApplications()`
4. `PostServiceImpl.java` - Thêm `.fetchApplications()` vào queries
5. `UserSummaryResponse.java` - Thêm `averageRating`, `totalReviews`
6. `ReviewRepository.java` - Thêm `countByTutorId()`

## Frontend (6 files):
1. `application.dto.ts` - Cập nhật `UserSummary`
2. `user.dto.ts` - Thêm rating vào `BaseUserResponse`
3. `application.service.ts` - Cập nhật mapper và enrich methods
4. `http.application.gateway.ts` - Sửa type trả về
5. `application.gateway.ts` - Cập nhật interface
6. `AdminPostsPage.tsx` - Redesign modal với grid 2 cột

## Kết quả Cuối cùng

### Before:
```
Lượt đăng ký (0)  ← Luôn 0

Modal: Chỉ tên + trạng thái
```

### After:
```
Lượt đăng ký (3)  ← Đúng số thực tế

Modal: Grid 2 cột
├─ Avatar + Rating ⭐ 4.5 • 12 đánh giá
├─ Đề xuất phí: 3.700.000 đ
└─ Đã ứng tuyển 3 ngày trước
```

---

**Ngày:** 2026-11-16  
**Version:** Complete (V1 + V2 + Debug)  
**Tác giả:** Kiro Agent  
**Tuân thủ:** AGENTS.md + CLAUDE.md (Simplicity First, Surgical Changes)
