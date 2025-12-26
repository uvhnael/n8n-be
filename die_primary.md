# KỊCH BẢN TEST: FAILOVER TỰ ĐỘNG (ORCHESTRATOR + SCRIPT)

**Mục tiêu:** Chứng minh rằng khi Primary chết, Orchestrator sẽ tự gọi script để cập nhật ProxySQL mà không cần con người can thiệp.

## Giai đoạn 1: Kiểm tra trạng thái "Bình yên"

Trước khi phá, hãy chắc chắn hệ thống đang đúng chuẩn.

### ProxySQL đang trỏ đúng

Kiểm tra cả 2 ProxySQL (main và backup) xem Writer (HG 10) có phải là `mysql-primary` không.

```bash
# Kiểm tra ProxySQL Main
docker exec -it proxysql mysql -uradmin -pradmin -h127.0.0.1 -P6032 -e "SELECT * FROM runtime_mysql_servers WHERE hostgroup_id=10;"

# Kiểm tra ProxySQL Backup
docker exec -it proxysql-backup mysql -uradmin -pradmin -h127.0.0.1 -P6032 -e "SELECT * FROM runtime_mysql_servers WHERE hostgroup_id=10;"
```

**Kết quả mong đợi:** Cả 2 đều có Hostname là `mysql-primary`.

### Orchestrator Topology

Truy cập http://localhost:3000. Cấu trúc phải là: `mysql-primary (Master) -> mysql-replica (Slave)`.

## Giai đoạn 2: Tấn công (Kill Primary)

Chúng ta sẽ giả lập sự cố sập server.

### Hành động duy nhất bạn làm:

```bash
docker exec mysql-primary mysqladmin -uroot -proot_password shutdown
```

**Sau đó: BUÔNG TAY KHỎI BÀN PHÍM!** Không chạy lệnh SQL nào cả. Hãy để Orchestrator làm việc.

## Giai đoạn 3: Giám sát Tự động hóa (The Magic Show)

Trong khoảng 10-30 giây tiếp theo, hãy quan sát các nơi sau để xem script hoạt động.

### 1. Xem Log của Orchestrator (Quan trọng nhất)

Đây là nơi bạn biết script failover có được gọi hay không.

```bash
docker logs -f orchestrator
```

Bạn cần tìm dòng log kiểu như:

```
Executing post-failover processes: /usr/local/bin/scripts/proxysql-failover.sh mysql-primary mysql-replica
```

Nếu thấy dòng này, nghĩa là Orchestrator đã phát hiện lỗi và kích hoạt script của bạn.

### 2. Kiểm tra lại ProxySQL

Sau khi log trên chạy xong, hãy kiểm tra lại cả 2 ProxySQL xem đã đổi chủ chưa.

```bash
docker exec -it proxysql mysql -uradmin -pradmin -h127.0.0.1 -P6032 -e "SELECT * FROM runtime_mysql_servers WHERE hostgroup_id=10;"

docker exec -it proxysql-backup mysql -uradmin -pradmin -h127.0.0.1 -P6032 -e "SELECT * FROM runtime_mysql_servers WHERE hostgroup_id=10;"
```

**Kết quả mong đợi:**

- Hostname bây giờ PHẢI LÀ: `mysql-replica` (trên cả 2)
- `mysql-primary` đã biến mất khỏi nhóm 10 (trên cả 2)

### 3. Kiểm tra Ứng dụng (Spring Boot)

Thử gọi API ghi dữ liệu. Nó sẽ hoạt động bình thường (có thể bị lỗi timeout trong khoảng 5-10s lúc đang chuyển đổi, sau đó tự thông).

## Giai đoạn 4: Dọn dẹp chiến trường (Hồi sinh Primary cũ)

Automation script thường chỉ lo cứu hệ thống (Failover), chứ không tự động sửa node chết (Recovery) khi nó sống lại (để tránh node cũ nhập nhằng dữ liệu). Bạn cần làm thủ công bước này để đưa `mysql-primary` quay lại làm lính (Replica).

### 1. Bật lại container cũ

```bash
docker start mysql-primary
```
### 2. Biến nó thành Replica (Lính)

Vì `mysql-replica` giờ đang là Vua, `mysql-primary` phải phục tùng.

```bash
docker exec -it mysql-primary mysql -uroot -proot_password -e "SET GLOBAL read_only=ON; SET GLOBAL super_read_only=ON;"

docker exec -it mysql-primary mysql -uroot -proot_password -e "
STOP REPLICA;
RESET MASTER;
CHANGE REPLICATION SOURCE TO
  SOURCE_HOST='mysql-replica',
  SOURCE_USER='repl',
  SOURCE_PASSWORD='repl_password',
  SOURCE_AUTO_POSITION=1;
START REPLICA;
"
```
### 3. Cập nhật cả 2 ProxySQL (Nhóm đọc - HG 20)

Vì lúc nãy `mysql-replica` đã thành Master (HG 10), nó không nên ở nhóm đọc nữa. Đồng thời thêm `mysql-primary` (giờ là Replica) vào nhóm đọc.

```bash
docker exec -it proxysql mysql -uradmin -pradmin -h127.0.0.1 -P6032 -e "
    DELETE FROM mysql_servers WHERE hostname='mysql-replica' AND hostgroup_id=20;
    INSERT INTO mysql_servers (hostgroup_id, hostname, port) 
    SELECT 20, 'mysql-primary', 3306 
    WHERE NOT EXISTS (SELECT 1 FROM mysql_servers WHERE hostname='mysql-primary' AND hostgroup_id=20);
    LOAD MYSQL SERVERS TO RUNTIME;
    SAVE MYSQL SERVERS TO DISK;
"

docker exec -it proxysql-backup mysql -uradmin -pradmin -h127.0.0.1 -P6032 -e "
    DELETE FROM mysql_servers WHERE hostname='mysql-replica' AND hostgroup_id=20;
    INSERT INTO mysql_servers (hostgroup_id, hostname, port) 
    SELECT 20, 'mysql-primary', 3306 
    WHERE NOT EXISTS (SELECT 1 FROM mysql_servers WHERE hostname='mysql-primary' AND hostgroup_id=20);
    LOAD MYSQL SERVERS TO RUNTIME;
    SAVE MYSQL SERVERS TO DISK;
"
```


## Giai đoạn 5: Reset về ban đầu (Switchover - Có kế hoạch)

Lúc này hệ thống đang chạy ổn: `mysql-replica` là Master, `mysql-primary` là Slave. Để quay ngược lại như cũ (Primary làm Master):

### Bước 1: Dừng replication trên mysql-primary (đang là Slave)

```bash
docker exec -it mysql-primary mysql -uroot -proot_password -e "STOP REPLICA;"
```

### Bước 2: Đợi mysql-replica sync hết dữ liệu

```bash
docker exec -it mysql-replica mysql -uroot -proot_password -e "SHOW PROCESSLIST;"
```

### Bước 3: Promote mysql-primary thành Master

```bash
docker exec -it mysql-primary mysql -uroot -proot_password -e "
RESET REPLICA ALL;
SET GLOBAL read_only=OFF;
SET GLOBAL super_read_only=OFF;
"
```

### Bước 4: Biến mysql-replica thành Replica

```bash
docker exec -it mysql-replica mysql -uroot -proot_password -e "
SET GLOBAL read_only=ON;
SET GLOBAL super_read_only=ON;
STOP REPLICA;
RESET MASTER;
CHANGE REPLICATION SOURCE TO
  SOURCE_HOST='mysql-primary',
  SOURCE_USER='repl',
  SOURCE_PASSWORD='repl_password',
  SOURCE_AUTO_POSITION=1;
START REPLICA;
"
```

### Bước 5: Kiểm tra replication status

```bash
docker exec -it mysql-replica mysql -uroot -proot_password -e "SHOW REPLICA STATUS\G" | grep -E "Running|Seconds_Behind"
```

**Kết quả mong đợi:** `Replica_IO_Running: Yes` và `Replica_SQL_Running: Yes`

### Bước 6: Update cả 2 ProxySQL về cấu hình ban đầu

```bash
docker exec -it proxysql mysql -uradmin -pradmin -h127.0.0.1 -P6032 -e "
DELETE FROM mysql_servers;
INSERT INTO mysql_servers (hostgroup_id, hostname, port) VALUES (10, 'mysql-primary', 3306);
INSERT INTO mysql_servers (hostgroup_id, hostname, port) VALUES (20, 'mysql-replica', 3306);
LOAD MYSQL SERVERS TO RUNTIME;
SAVE MYSQL SERVERS TO DISK;
"

docker exec -it proxysql-backup mysql -uradmin -pradmin -h127.0.0.1 -P6032 -e "
DELETE FROM mysql_servers;
INSERT INTO mysql_servers (hostgroup_id, hostname, port) VALUES (10, 'mysql-primary', 3306);
INSERT INTO mysql_servers (hostgroup_id, hostname, port) VALUES (20, 'mysql-replica', 3306);
LOAD MYSQL SERVERS TO RUNTIME;
SAVE MYSQL SERVERS TO DISK;
"
```

### Bước 7: Kiểm tra cả 2 ProxySQL

```bash
docker exec -it proxysql mysql -uradmin -pradmin -h127.0.0.1 -P6032 -e "SELECT hostgroup_id, hostname, status FROM runtime_mysql_servers;"

docker exec -it proxysql-backup mysql -uradmin -pradmin -h127.0.0.1 -P6032 -e "SELECT hostgroup_id, hostname, status FROM runtime_mysql_servers;"
```

**Kết quả mong đợi (trên cả 2):**
- `mysql-primary` ở hostgroup 10 (Writer)
- `mysql-replica` ở hostgroup 20 (Reader)

**Nếu thấy mysql-primary xuất hiện ở cả hostgroup 20 với status OFFLINE_HARD**, cleanup trên cả 2:

```bash
docker exec -it proxysql mysql -uradmin -pradmin -h127.0.0.1 -P6032 -e "
DELETE FROM mysql_servers WHERE hostname='mysql-primary' AND hostgroup_id=20;
LOAD MYSQL SERVERS TO RUNTIME;
SAVE MYSQL SERVERS TO DISK;
SELECT hostgroup_id, hostname, status FROM runtime_mysql_servers;
"

docker exec -it proxysql-backup mysql -uradmin -pradmin -h127.0.0.1 -P6032 -e "
DELETE FROM mysql_servers WHERE hostname='mysql-primary' AND hostgroup_id=20;
LOAD MYSQL SERVERS TO RUNTIME;
SAVE MYSQL SERVERS TO DISK;
SELECT hostgroup_id, hostname, status FROM runtime_mysql_servers;
"
```

**Kết quả sau cleanup:**
```
+--------------+---------------+--------+
| hostgroup_id | hostname      | status |
+--------------+---------------+--------+
| 10           | mysql-primary | ONLINE |
| 20           | mysql-replica | ONLINE |
+--------------+---------------+--------+
```

### Bước 8: Test kết nối

```bash
mysql -h 127.0.0.1 -P 3306 -uroot -proot_password -e "SELECT @@hostname;"
```

**Kết quả:** `mysql-replica` ✅ (Đúng! Vì câu SELECT được route vào Reader)

```bash
mysql -h 127.0.0.1 -P 3306 -uroot -proot_password -e "SELECT @@hostname FOR UPDATE;"
```

**Kết quả:** `mysql-primary` ✅ (Đúng! Vì SELECT FOR UPDATE được route vào Writer)

```bash
mysql -h 127.0.0.1 -P 3306 -uroot -proot_password n8n_db -e "
CREATE TABLE IF NOT EXISTS test_table (id INT, name VARCHAR(50));
INSERT INTO test_table VALUES (1, 'test');
SELECT @@hostname AS 'Written to';
DROP TABLE test_table;
"
```

**Kết quả:** `mysql-primary` ✅ (Đúng! Vì INSERT được route vào Writer)

### Tóm tắt kết quả:

| Loại câu lệnh | Route đến | Hostgroup | Kết quả |
|---------------|-----------|-----------|---------|
| `SELECT` | mysql-replica | HG 20 (Reader) | ✅ |
| `SELECT FOR UPDATE` | mysql-primary | HG 10 (Writer) | ✅ |
| `INSERT/UPDATE/DELETE` | mysql-primary | HG 10 (Writer) | ✅ |

**Hệ thống đã quay về trạng thái ban đầu hoàn toàn!** ✅

---

## Kiểm tra cuối cùng trên Orchestrator

Truy cập http://localhost:3000 và xác nhận:
- ✅ `mysql-primary` là **Master** (có mũi tên đi ra)
- ✅ `mysql-replica` là **Slave** (có mũi tên đi vào)
- ✅ Không có cảnh báo ErrantGTID

---

## BONUS: Test Failover khi không có Reader

Để test xem hệ thống có tự động fallback sang Writer khi không có Reader không:

### Xóa mysql-replica khỏi hostgroup 20 (Reader) trên cả 2 ProxySQL

```bash
docker exec -it proxysql mysql -uradmin -pradmin -h127.0.0.1 -P6032 -e "
DELETE FROM mysql_servers WHERE hostname='mysql-replica' AND hostgroup_id=20;
LOAD MYSQL SERVERS TO RUNTIME;
SAVE MYSQL SERVERS TO DISK;
SELECT hostgroup_id, hostname, status FROM runtime_mysql_servers;
"

docker exec -it proxysql-backup mysql -uradmin -pradmin -h127.0.0.1 -P6032 -e "
DELETE FROM mysql_servers WHERE hostname='mysql-replica' AND hostgroup_id=20;
LOAD MYSQL SERVERS TO RUNTIME;
SAVE MYSQL SERVERS TO DISK;
SELECT hostgroup_id, hostname, status FROM runtime_mysql_servers;
"
```

**Kết quả sau khi xóa (trên cả 2):**
```
+--------------+---------------+--------+
| hostgroup_id | hostname      | status |
+--------------+---------------+--------+
| 10           | mysql-primary | ONLINE |
+--------------+---------------+--------+
```

### Test câu SELECT (giờ sẽ fallback sang mysql-primary)

```bash
mysql -h 127.0.0.1 -P 3306 -uroot -proot_password -e "SELECT @@hostname;"
```

**Kết quả:** `mysql-primary` ✅ (ProxySQL tự động fallback vì không có Reader nào available)

### Khôi phục lại mysql-replica vào hostgroup 20 trên cả 2 ProxySQL

```bash
docker exec -it proxysql mysql -uradmin -pradmin -h127.0.0.1 -P6032 -e "
INSERT INTO mysql_servers (hostgroup_id, hostname, port) VALUES (20, 'mysql-replica', 3306);
LOAD MYSQL SERVERS TO RUNTIME;
SAVE MYSQL SERVERS TO DISK;
SELECT hostgroup_id, hostname, status FROM runtime_mysql_servers;
"

docker exec -it proxysql-backup mysql -uradmin -pradmin -h127.0.0.1 -P6032 -e "
INSERT INTO mysql_servers (hostgroup_id, hostname, port) VALUES (20, 'mysql-replica', 3306);
LOAD MYSQL SERVERS TO RUNTIME;
SAVE MYSQL SERVERS TO DISK;
SELECT hostgroup_id, hostname, status FROM runtime_mysql_servers;
"
```