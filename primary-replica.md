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

## Bước 4: Orchestrator tự động Failover

Lúc này nếu gọi API POST (INSERT), App sẽ có lỗi tạm thời trong vài giây.

**Orchestrator sẽ tự động:**

1. Phát hiện Primary chết (health check fail)
2. Chọn Replica tốt nhất để thăng cấp
3. Tự động promote `mysql-replica` thành Primary mới:
   - `STOP REPLICA`
   - `RESET REPLICA ALL`
   - `SET GLOBAL read_only = OFF`
4. Cập nhật topology trong UI

**Kiểm tra Orchestrator UI:** Truy cập `http://localhost:3000` để xem quá trình failover.

## Bước 5: Kiểm tra hệ thống sau Failover

**5.1. Kiểm tra trạng thái mysql-replica (đã thành Primary mới):**

```bash
docker exec -it mysql-replica mysql -uroot -proot_password -e "SHOW MASTER STATUS; SELECT @@read_only;"
```

**Kết quả mong đợi:**

- `read_only` = `0` (OFF)
- Có binary log position mới

**5.2. Test GHI lại (Thành công):**

Gọi API POST (INSERT) → App lưu dữ liệu thành công!

**Hiện trạng:** `mysql-replica` đã thành Primary mới, xử lý cả ĐỌC và GHI. Hệ thống đã tự động vượt qua sự cố!

---

## Phần 1: Khôi phục Primary cũ thành Replica (Để đồng bộ dữ liệu)

Sau khi test xong, bây giờ ta sẽ biến Primary cũ thành Replica để đồng bộ dữ liệu từ Primary mới (mysql-replica hiện tại).

### Bước 1: Start lại mysql-primary (Server cũ)

```bash
docker start mysql-primary
```

### Bước 2: Biến Primary cũ thành Replica tạm thời để đồng bộ dữ liệu

**2.1. Cấu hình Primary cũ để replicate từ Replica hiện tại (đang là Primary mới)**

Vào mysql-primary:

```bash
docker exec -it mysql-primary mysql -uroot -proot_password
```

Chạy các lệnh sau:

```sql
-- Dừng replication nếu đang chạy
STOP REPLICA;

-- Reset replica status
RESET REPLICA ALL;

-- Cấu hình để replicate từ mysql-replica (hiện đang là Primary)
CHANGE REPLICATION SOURCE TO
  SOURCE_HOST='mysql-replica',
  SOURCE_PORT=3306,
  SOURCE_USER='replicator',
  SOURCE_PASSWORD='replicator_password',
  SOURCE_AUTO_POSITION=1;

-- Bật chế độ Read-Only trên Primary cũ (bảo vệ dữ liệu)
SET GLOBAL read_only = ON;

-- Start replication
START REPLICA;

-- Kiểm tra trạng thái
SHOW REPLICA STATUS\G
```

**Kiểm tra:** Đợi cho đến khi `Seconds_Behind_Source` = 0, nghĩa là đã đồng bộ xong.

### Bước 3: Cấu hình ProxySQL để thêm mysql-primary vào nhóm đọc (Hostgroup 20)

Bây giờ mysql-primary đã thành Replica, ta thêm nó vào nhóm đọc để chia tải:

```bash
docker exec -it proxysql mysql -u admin -padmin -h 127.0.0.1 -P 6032
```

```sql
-- Thêm mysql-primary vào nhóm đọc (Hostgroup 20)
INSERT INTO mysql_servers(hostgroup_id, hostname, port) VALUES (20, 'mysql-primary', 3306);

-- Load và save
LOAD MYSQL SERVERS TO RUNTIME;
SAVE MYSQL SERVERS TO DISK;

-- Kiểm tra
SELECT hostgroup_id, hostname, port, status FROM mysql_servers;
```

**Kết quả hiện tại:**

- `mysql-replica` → Hostgroup 10 (GHI) và Hostgroup 20 (ĐỌC)
- `mysql-primary` → Hostgroup 20 (ĐỌC - Replica của mysql-replica)

**Trạng thái:** Hệ thống đang chạy với `mysql-replica` là Primary, `mysql-primary` là Replica. Cả 2 đều xử lý READ.

---

## Phần 2: Đổi vai trò ngược lại về như ban đầu

Bây giờ ta sẽ đưa `mysql-primary` về làm Primary chính và `mysql-replica` về làm Replica như ban đầu.

### Bước 1: Đổi vai trò ngược lại (Replica → Read-Only, Primary cũ → Primary chính)

**1.1. Dừng ghi dữ liệu vào Replica hiện tại (chuẩn bị chuyển quyền)**

```bash
docker exec -it mysql-replica mysql -uroot -proot_password -e "SET GLOBAL read_only = ON; FLUSH TABLES WITH READ LOCK;"
```

**1.2. Đợi Primary cũ đồng bộ hoàn toàn**

```bash
docker exec -it mysql-primary mysql -uroot -proot_password -e "SHOW REPLICA STATUS\G" | grep "Seconds_Behind_Source"
```

Đợi đến khi `Seconds_Behind_Source: 0`.

**1.3. Dừng replication trên Primary cũ và chuyển về chế độ Primary**

```bash
docker exec -it mysql-primary mysql -uroot -proot_password
```

```sql
-- Dừng replica
STOP REPLICA;

-- Reset replica configuration
RESET REPLICA ALL;

-- Tắt Read-Only, cho phép ghi
SET GLOBAL read_only = OFF;

-- Kiểm tra trạng thái
SHOW MASTER STATUS;
```

**1.4. Cấu hình lại Replica để replicate từ Primary chính (như ban đầu)**

```bash
docker exec -it mysql-replica mysql -uroot -proot_password
```

```sql
-- Unlock tables nếu đang lock
UNLOCK TABLES;

-- Dừng replica
STOP REPLICA;

-- Reset replica
RESET REPLICA ALL;

-- Cấu hình lại replicate từ mysql-primary
CHANGE REPLICATION SOURCE TO
  SOURCE_HOST='mysql-primary',
  SOURCE_PORT=3306,
  SOURCE_USER='replicator',
  SOURCE_PASSWORD='replicator_password',
  SOURCE_AUTO_POSITION=1;

-- Bật chế độ Read-Only
SET GLOBAL read_only = ON;

-- Start replication
START REPLICA;

-- Kiểm tra
SHOW REPLICA STATUS\G
```

### Bước 2: Cập nhật cấu hình ProxySQL về trạng thái ban đầu

```bash
docker exec -it proxysql mysql -u admin -padmin -h 127.0.0.1 -P 6032
```

```sql
-- Xóa cấu hình hiện tại
DELETE FROM mysql_servers;

-- Thêm lại Primary và Replica đúng vai trò
INSERT INTO mysql_servers(hostgroup_id, hostname, port) VALUES (10, 'mysql-primary', 3306);
INSERT INTO mysql_servers(hostgroup_id, hostname, port) VALUES (20, 'mysql-replica', 3306);

-- Load và save cấu hình
LOAD MYSQL SERVERS TO RUNTIME;
SAVE MYSQL SERVERS TO DISK;

-- Kiểm tra
SELECT * FROM mysql_servers;
```

### Bước 3: Kiểm tra hệ thống hoạt động bình thường

**3.1. Kiểm tra trạng thái Replication:**

```bash
# Trên Primary
docker exec -it mysql-primary mysql -uroot -proot_password -e "SHOW MASTER STATUS;"

# Trên Replica
docker exec -it mysql-replica mysql -uroot -proot_password -e "SHOW REPLICA STATUS\G" | grep "Seconds_Behind_Source"
```

**3.2. Test ứng dụng:**

- Gọi API POST (INSERT) → Dữ liệu ghi vào `mysql-primary`
- Gọi API GET (SELECT) → Dữ liệu đọc từ `mysql-replica`
- Kiểm tra dữ liệu đồng bộ giữa 2 server

**Kết quả:** Hệ thống đã về trạng thái Primary-Replica ban đầu! ✅

---

## Tóm tắt quy trình

1. **Primary chết** → Replica tự động xử lý READ
2. **Failover thủ công** → Chuyển Replica thành Primary mới để xử lý WRITE
3. **Khôi phục Primary cũ** → Start lại, biến thành Replica tạm để đồng bộ data
4. **Đổi vai trò ngược lại** → Primary cũ về vị trí chính, Replica về vị trí phụ
5. **Cập nhật ProxySQL** → Trỏ đúng routing rules
6. **Kiểm tra** → Đảm bảo replication hoạt động bình thường
