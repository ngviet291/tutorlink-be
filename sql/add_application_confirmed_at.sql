-- Chạy một lần trên cơ sở dữ liệu hiện có trước khi khởi động backend mới.
-- Đơn cũ chưa lưu thời điểm xác nhận; truy vấn thống kê tạm dùng
-- transaction_date của giao dịch phí thành công, không sửa dữ liệu lịch sử.
ALTER TABLE applications
    ADD COLUMN IF NOT EXISTS confirmed_at TIMESTAMP WITH TIME ZONE;
