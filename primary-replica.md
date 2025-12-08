# Kịch bản Test: Primary chết, thăng cấp Replica cứu App

## Bước 1: Kiểm tra trạng thái bình thường

Đảm bảo Spring Boot đang chạy và kết nối được. Bạn hãy thử gọi API lấy dữ liệu (GET) và ghi dữ liệu (POST).

## Bước 2: "Giết" Primary

Chạy lệnh dừng container Primary:

```bash
docker stop mysql-primary
```

## Bước 3: Test ĐỌC (Vẫn sống)

Lúc này, bạn vào App hoặc Postman gọi API GET (SELECT).

**Kết quả:** Vẫn chạy bình thường.

**Giải thích:** ProxySQL thấy Primary chết (SHUNNED), nó tự động đẩy query SELECT sang `mysql-replica`.

## Bước 4: Test GHI (Sẽ lỗi) và Cứu (Failover)

Lúc này nếu gọi API POST (INSERT), App sẽ báo lỗi JDBC connection hoặc Read-only mode.

Bây giờ chúng ta sẽ làm thao tác "Cứu hộ" (Failover) thủ công:

### 1. Tắt chế độ Read-Only trên Replica (Biến nó thành Primary mới)

```bash
docker exec -it mysql-replica mysql -uroot -proot_password -e "STOP REPLICA; SET GLOBAL read_only = OFF;"
```

### 2. Cấu hình lại ProxySQL để trỏ GHI về Replica

Vào Admin ProxySQL:

```bash
docker exec -it proxysql mysql -u admin -padmin -h 127.0.0.1 -P 6032
```

Chạy các lệnh sau để chuyển `mysql-replica` từ nhóm đọc (20) sang nhóm ghi (10):

```sql
-- Xóa replica khỏi nhóm đọc (tạm thời) hoặc giữ nguyên cũng được, nhưng quan trọng là thêm vào nhóm ghi
-- Ở đây mình sẽ xóa primary cũ (đã chết) khỏi nhóm ghi
DELETE FROM mysql_servers WHERE hostgroup_id=10;

-- Thêm Replica vào nhóm ghi (Hostgroup 10)
INSERT INTO mysql_servers(hostgroup_id, hostname, port) VALUES (10, 'mysql-replica', 3306);

-- Load lại cấu hình
LOAD MYSQL SERVERS TO RUNTIME;
SAVE MYSQL SERVERS TO DISK;
```

## Bước 5: Test GHI lại (Thành công)

Bây giờ bạn quay lại Postman gọi API POST (INSERT).

**Kết quả:** App lưu dữ liệu thành công!

**Hiện trạng:** Lúc này `mysql-replica` đang gánh cả Đọc và Ghi. Hệ thống đã vượt qua sự cố chết Primary.

---

## Cách khôi phục lại như cũ (Sau khi test xong)

Sau khi test xong, để đưa hệ thống về trạng thái Primary-Replica chuẩn ban đầu, bạn làm như sau:

### 1. Start lại mysql-primary

```bash
docker start mysql-primary
```

### 2. Reset lại Replica về chế độ Read-Only và bật lại replication

```bash
docker exec -it mysql-replica mysql -uroot -proot_password -e "SET GLOBAL read_only = ON; START REPLICA;"
```

> **Lưu ý:** Dữ liệu bạn vừa ghi vào Replica lúc test sẽ chưa có trên Primary, trong thực tế cần đồng bộ ngược lại, nhưng ở môi trường test thì có thể bỏ qua.

### 3. Reset cấu hình ProxySQL về chuẩn (Primary=10, Replica=20)

```bash
docker exec -it proxysql mysql -u admin -padmin -h 127.0.0.1 -P 6032
```

```sql
DELETE FROM mysql_servers; -- Xóa hết làm lại cho sạch
INSERT INTO mysql_servers(hostgroup_id, hostname, port) VALUES (10, 'mysql-primary', 3306);
INSERT INTO mysql_servers(hostgroup_id, hostname, port) VALUES (20, 'mysql-replica', 3306);
LOAD MYSQL SERVERS TO RUNTIME;
SAVE MYSQL SERVERS TO DISK;
```
