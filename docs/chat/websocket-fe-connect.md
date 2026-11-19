# TutorLink: Hướng dẫn FE kết nối WebSocket (chat realtime)

Dành cho React/TypeScript (Redux Toolkit). Phía backend xem `websocket-handover.md`.

> Code mẫu trong tài liệu chưa được chạy thử. Hãy điều chỉnh tên store, action và hàm lấy token cho khớp dự án.

---

## 1. Tóm tắt kết nối

| Mục | Giá trị |
|---|---|
| Giao thức | STOMP over SockJS |
| URL | `{API_BASE}/ws`, ví dụ `http://localhost:8080/api/ws` (dùng **http/https**, không phải `ws://`) |
| Xác thực | Header STOMP `CONNECT`: `Authorization: Bearer <accessToken>` |
| Subscribe | **`/user/queue/events`** (một đích duy nhất, không kèm userId) |
| Gửi tin | Vẫn bằng REST `POST /v1/chat` (WS chỉ nhận) |
| Đánh dấu đã đọc | REST `PATCH /v1/chat/channels/{channelId}/read` |

Thiếu hoặc sai token thì server từ chối `CONNECT`.

## 2. Cài đặt

```bash
npm i @stomp/stompjs sockjs-client
npm i -D @types/sockjs-client
```

Với Vite, `sockjs-client` cần biến `global`. Thêm vào `vite.config.ts`:

```ts
export default defineConfig({
  define: { global: 'globalThis' },
  // ...
});
```

Biến môi trường:

```
VITE_API_BASE=http://localhost:8080/api
```

## 3. Kiểu dữ liệu

```ts
// realtime/types.ts
export type RealtimeEventType = 'MESSAGE_NEW' | 'MESSAGE_READ';

export interface RealtimeEnvelope<T = unknown> {
  type: RealtimeEventType;
  occurredAt: string; // ISO-8601
  data: T;
}

// Cùng cấu trúc với phần tử của GET /v1/chat/channels/{id}/messages
export interface MessageDto {
  id: string;
  chatChannelId: string;
  senderId: string;
  content: string;
  sentAt: string;
  mediaList: unknown[]; // dùng lại type MediaResponse đang có trong dự án
}

export interface MessageReadDto {
  channelId: string;
  readerId: string;
  readAt: string;
}

export type RealtimeEvent =
  | { type: 'MESSAGE_NEW'; occurredAt: string; data: MessageDto }
  | { type: 'MESSAGE_READ'; occurredAt: string; data: MessageReadDto };
```

## 4. Client dùng chung (singleton)

```ts
// realtime/realtimeClient.ts
import { Client, type StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import type { RealtimeEvent } from './types';

const USER_EVENTS = '/user/queue/events';

type Options = {
  /** Trả về access token còn hạn (tự refresh nếu cần). */
  getAccessToken: () => Promise<string>;
  onEvent: (event: RealtimeEvent) => void;
  /** Gọi mỗi lần (re)connect thành công: dùng để refetch dữ liệu bị lỡ. */
  onConnected?: (isReconnect: boolean) => void;
};

let client: Client | null = null;
let subscription: StompSubscription | null = null;
let connectedOnce = false;

export function startRealtime(opts: Options) {
  if (client?.active) return;

  client = new Client({
    webSocketFactory: () => new SockJS(`${import.meta.env.VITE_API_BASE}/ws`),
    reconnectDelay: 5000,

    // Chạy trước MỖI lần connect/reconnect: luôn dùng token mới.
    beforeConnect: async (c) => {
      const token = await opts.getAccessToken();
      c.connectHeaders = { Authorization: `Bearer ${token}` };
    },

    onConnect: () => {
      // Subscribe lại sau mỗi lần reconnect.
      subscription = client!.subscribe(USER_EVENTS, (msg) => {
        try {
          opts.onEvent(JSON.parse(msg.body) as RealtimeEvent);
        } catch (e) {
          console.error('[realtime] parse error', e);
        }
      });
      opts.onConnected?.(connectedOnce);
      connectedOnce = true;
    },

    onStompError: (frame) => console.error('[realtime] STOMP error', frame.headers['message']),
    onWebSocketClose: () => { subscription = null; },
  });

  client.activate();
}

export async function stopRealtime() {
  subscription?.unsubscribe();
  subscription = null;
  connectedOnce = false;
  await client?.deactivate();
  client = null;
}
```

## 5. Tích hợp Redux Toolkit

Slice mẫu (điều chỉnh theo state chat hiện có):

```ts
// features/chat/chatSlice.ts
import { createSlice, type PayloadAction } from '@reduxjs/toolkit';
import type { MessageDto, MessageReadDto } from '@/realtime/types';

interface ChatState {
  messagesByChannel: Record<string, MessageDto[]>;
  unreadCount: number;
  readUpTo: Record<string, string>; // channelId -> readAt (đối phương đã đọc)
}

const initialState: ChatState = { messagesByChannel: {}, unreadCount: 0, readUpTo: {} };

const chatSlice = createSlice({
  name: 'chat',
  initialState,
  reducers: {
    // Dùng cho CẢ kết quả REST gửi tin lẫn event MESSAGE_NEW: upsert theo id để không bị trùng.
    messageUpserted(state, { payload }: PayloadAction<MessageDto>) {
      const list = (state.messagesByChannel[payload.chatChannelId] ??= []);
      if (!list.some((m) => m.id === payload.id)) list.push(payload);
    },
    unreadIncremented(state) { state.unreadCount += 1; },
    unreadSet(state, { payload }: PayloadAction<number>) { state.unreadCount = payload; },
    partnerRead(state, { payload }: PayloadAction<MessageReadDto>) {
      state.readUpTo[payload.channelId] = payload.readAt;
    },
  },
});

export const { messageUpserted, unreadIncremented, unreadSet, partnerRead } = chatSlice.actions;
export default chatSlice.reducer;
```

Hook khởi động khi đã đăng nhập:

```tsx
// realtime/useRealtime.ts
import { useEffect } from 'react';
import { useAppDispatch, useAppSelector } from '@/app/hooks'; // theo dự án
import { startRealtime, stopRealtime } from './realtimeClient';
import { messageUpserted, unreadIncremented, partnerRead, unreadSet } from '@/features/chat/chatSlice';

export function useRealtime() {
  const dispatch = useAppDispatch();
  const me = useAppSelector((s) => s.auth.user);          // theo dự án
  const activeChannelId = useAppSelector((s) => s.chat.activeChannelId); // theo dự án

  useEffect(() => {
    if (!me) return;

    startRealtime({
      getAccessToken: () => getValidAccessToken(), // hàm của bạn: refresh nếu sắp hết hạn
      onEvent: (e) => {
        if (e.type === 'MESSAGE_NEW') {
          dispatch(messageUpserted(e.data));
          const fromPartner = e.data.senderId !== me.id;
          if (fromPartner) {
            if (e.data.chatChannelId === activeChannelId) {
              markChannelRead(e.data.chatChannelId);       // PATCH .../read
            } else {
              dispatch(unreadIncremented());
            }
          }
        } else if (e.type === 'MESSAGE_READ') {
          dispatch(partnerRead(e.data));
        }
      },
      onConnected: async (isReconnect) => {
        // Event không được replay: đồng bộ lại sau reconnect.
        if (isReconnect) await refetchChannels();          // GET /v1/chat/channels
        dispatch(unreadSet(await fetchUnreadCount()));     // GET /v1/chat/unread-count
      },
    });

    return () => { void stopRealtime(); };
  }, [me?.id]); // eslint-disable-line react-hooks/exhaustive-deps
}
```

Gọi `useRealtime()` **một lần** ở component gốc sau khi đăng nhập (ví dụ `App` hoặc layout đã bảo vệ route).

> `activeChannelId` trong closure có thể bị cũ khi đổi kênh. Nếu gặp, đọc giá trị mới nhất từ store (`store.getState()`) hoặc giữ nó trong `useRef`.

## 6. Quy tắc xử lý

1. **Dedupe theo `data.id`.** Người gửi nhận lại tin của chính mình qua `MESSAGE_NEW` (để đồng bộ nhiều tab), đồng thời REST `POST /v1/chat` cũng trả tin đó. `messageUpserted` ở trên đã xử lý.
2. **`MESSAGE_NEW` từ chính mình** không tăng unread.
3. **`MESSAGE_READ`** có nghĩa đối phương đã đọc kênh `channelId` tại `readAt`. Dùng để hiện trạng thái "đã xem".
4. **Mở một kênh:** gọi `PATCH /v1/chat/channels/{id}/read`; server sẽ push `MESSAGE_READ` cho đối phương.
5. **Sau mỗi reconnect:** refetch danh sách kênh và `unread-count` (không có replay).

## 7. Token hết hạn, reconnect, đăng xuất

- `beforeConnect` chạy mỗi lần (re)connect, nên chỉ cần `getAccessToken()` trả token còn hạn.
- Phiên WS **không bị server đóng khi token hết hạn**; token chỉ được kiểm tra lúc `CONNECT`. Khi mất kết nối và tự reconnect, token mới sẽ được dùng.
- **Đăng xuất:** gọi `stopRealtime()` (hook ở trên tự làm khi `me` thành `null`).
- Cần lưu ý khi refresh token: backend hiện có lỗi ở luồng `POST /v1/auth/refresh-token` (token cấp lúc login/refresh có thể bị báo user không tồn tại). Nếu refresh thất bại, `getAccessToken()` nên ném lỗi để reconnect dừng và chuyển người dùng về trang đăng nhập.

## 8. Kiểm tra nhanh

1. Đăng nhập hai tài khoản trên hai trình duyệt (một student, một tutor).
2. Mở DevTools, tab Network, lọc `ws` hoặc `websocket`: thấy kết nối tới `.../api/ws/...`.
3. Student gửi tin: tutor thấy tin xuất hiện không cần reload; student chỉ thấy **một** bản tin (không trùng).
4. Tutor mở kênh: student nhận `MESSAGE_READ`.
5. Tắt mạng vài giây rồi bật lại: client tự reconnect và refetch.

## 9. Lỗi thường gặp

| Triệu chứng | Nguyên nhân thường gặp |
|---|---|
| `global is not defined` | Chưa thêm `define: { global: 'globalThis' }` trong Vite |
| Kết nối bị đóng ngay sau `CONNECT` | Thiếu hoặc sai header `Authorization`, hoặc token hết hạn |
| 404 ở `/ws` | Quên context-path: URL phải là `{host}/api/ws` |
| Dùng `ws://` mà lỗi | SockJS nhận URL `http(s)://`, không dùng `ws://` |
| Không nhận event dù đã connect | Subscribe sai đích. Phải là `/user/queue/events` (không thêm userId) |
| Tin hiện hai lần | Chưa upsert theo `id` giữa kết quả REST và event `MESSAGE_NEW` |
| Subscribe bị từ chối | Chưa authenticated, hoặc đích không nằm trong danh sách cho phép của `MessageSecurityConfig` |
