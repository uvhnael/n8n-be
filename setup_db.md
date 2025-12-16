# HƯỚNG DẪN CẤU HÌNH CLUSTER: MYSQL - PROXYSQL - HAPROXY

Tài liệu này tổng hợp các bước lệnh cần thiết để kích hoạt Replication và định tuyến sau khi chạy `docker-compose up`.

## BƯỚC 1: KHỞI TẠO MÔI TRƯỜNG
Chạy lệnh này ở terminal (máy host) để đảm bảo môi trường sạch sẽ và các container được dựng lên.

```bash
# 1. Xóa container và volume cũ (để tránh lỗi dữ liệu cũ)
docker compose down -v

# 2. Khởi chạy hệ thống
docker compose up -d --build

# 3. Đợi khoảng 60 giây để MySQL khởi động xong trước khi làm bước tiếp theo
echo "Đang chờ MySQL khởi động..."
sleep 60
```

## BƯỚC 2: CẤU HÌNH MYSQL PRIMARY

Tạo các user cần thiết cho Replication, Orchestrator và Prometheus Exporter.

```bash
docker exec -it mysql-primary mysql -uroot -proot_password -e "
-- User Replication
CREATE USER IF NOT EXISTS 'repl'@'%' IDENTIFIED WITH mysql_native_password BY 'repl_password';
GRANT REPLICATION SLAVE ON *.* TO 'repl'@'%';

-- User Orchestrator
CREATE USER IF NOT EXISTS 'orchestrator'@'%' IDENTIFIED BY 'orc_password';
GRANT SUPER, PROCESS, REPLICATION SLAVE, RELOAD ON *.* TO 'orchestrator'@'%';
GRANT SELECT ON mysql.slave_master_info TO 'orchestrator'@'%';

-- User cho Exporter (Prometheus)
CREATE USER IF NOT EXISTS 'exporter'@'%' IDENTIFIED BY 'exporter_password' WITH MAX_USER_CONNECTIONS 3;
GRANT PROCESS, REPLICATION CLIENT, SELECT ON *.* TO 'exporter'@'%';

CREATE USER IF NOT EXISTS 'monitor'@'%' IDENTIFIED BY 'monitor';
GRANT USAGE ON *.* TO 'monitor'@'%';

FLUSH PRIVILEGES;"
```

## BƯỚC 3: CẤU HÌNH MYSQL REPLICA

Kết nối Replica vào Primary để bắt đầu đồng bộ dữ liệu.

```bash
docker exec -it mysql-replica mysql -uroot -proot_password -e "STOP REPLICA; RESET SLAVE ALL; RESET MASTER; CHANGE REPLICATION SOURCE TO SOURCE_HOST='mysql-primary', SOURCE_USER='repl', SOURCE_PASSWORD='repl_password', SOURCE_AUTO_POSITION=1; START REPLICA;"
```

## BƯỚC 4: CẤU HÌNH PROXYSQL (MAIN)

Định tuyến câu lệnh: Ghi vào Primary, Đọc từ Replica.

```bash
docker exec -it proxysql mysql -uradmin -pradmin -h127.0.0.1 -P6032 -e "
-- Xóa cấu hình cũ (nếu có)
DELETE FROM mysql_users WHERE username='root';
DELETE FROM mysql_servers;
DELETE FROM mysql_query_rules;

-- Thêm user root
INSERT INTO mysql_users(username, password, default_hostgroup) VALUES ('root', 'root_password', 10);

-- Thêm MySQL servers
INSERT INTO mysql_servers(hostgroup_id, hostname, port) VALUES (10, 'mysql-primary', 3306);
INSERT INTO mysql_servers(hostgroup_id, hostname, port) VALUES (20, 'mysql-replica', 3306);

-- Thêm query rules (Ghi vào HG 10, Đọc từ HG 20)
INSERT INTO mysql_query_rules (rule_id, active, match_digest, destination_hostgroup, apply) 
VALUES (1, 1, '^SELECT.*FOR UPDATE$', 10, 1), (2, 1, '^SELECT', 20, 1);

UPDATE global_variables SET variable_value='8.0.33' WHERE variable_name='mysql-server_version';

-- Load và Save cấu hình
LOAD MYSQL USERS TO RUNTIME; SAVE MYSQL USERS TO DISK;
LOAD MYSQL SERVERS TO RUNTIME; SAVE MYSQL SERVERS TO DISK;
LOAD MYSQL QUERY RULES TO RUNTIME; SAVE MYSQL QUERY RULES TO DISK;
LOAD MYSQL VARIABLES TO RUNTIME; SAVE MYSQL VARIABLES TO DISK;

-- Cấu hình admin credentials cho exporter
UPDATE global_variables SET variable_value='radmin:radmin' WHERE variable_name='admin-admin_credentials';
LOAD ADMIN VARIABLES TO RUNTIME; SAVE ADMIN VARIABLES TO DISK;
"
```

## BƯỚC 5: (TÙY CHỌN) CẤU HÌNH PROXYSQL BACKUP

Nếu bạn muốn con proxysql-backup cũng hoạt động y hệt con chính, hãy chạy lại Bước 4 nhưng thay lệnh truy cập đầu tiên bằng:

```bash
docker exec -it proxysql-backup mysql -uradmin -pradmin -h127.0.0.1 -P6032 -e "
-- Xóa cấu hình cũ (nếu có)
DELETE FROM mysql_users WHERE username='root';
DELETE FROM mysql_servers;
DELETE FROM mysql_query_rules;

-- Thêm user root
INSERT INTO mysql_users(username, password, default_hostgroup) VALUES ('root', 'root_password', 10);

-- Thêm MySQL servers
INSERT INTO mysql_servers(hostgroup_id, hostname, port) VALUES (10, 'mysql-primary', 3306);
INSERT INTO mysql_servers(hostgroup_id, hostname, port) VALUES (20, 'mysql-replica', 3306);

-- Thêm query rules (Ghi vào HG 10, Đọc từ HG 20)
INSERT INTO mysql_query_rules (rule_id, active, match_digest, destination_hostgroup, apply) 
VALUES (1, 1, '^SELECT.*FOR UPDATE$', 10, 1), (2, 1, '^SELECT', 20, 1);

UPDATE global_variables SET variable_value='8.0.33' WHERE variable_name='mysql-server_version';


-- Load và Save cấu hình
LOAD MYSQL USERS TO RUNTIME; SAVE MYSQL USERS TO DISK;
LOAD MYSQL SERVERS TO RUNTIME; SAVE MYSQL SERVERS TO DISK;
LOAD MYSQL QUERY RULES TO RUNTIME; SAVE MYSQL QUERY RULES TO DISK;
LOAD MYSQL QUERY RULES TO RUNTIME; SAVE MYSQL QUERY RULES TO DISK;

-- Cấu hình admin credentials cho exporter
UPDATE global_variables SET variable_value='radmin:radmin' WHERE variable_name='admin-admin_credentials';
LOAD ADMIN VARIABLES TO RUNTIME; SAVE ADMIN VARIABLES TO DISK;
"
```

_(Các lệnh SQL bên trong y hệt bước 4)_

## BƯỚC 6: KIỂM TRA TOÀN HỆ THỐNG

Sau khi cấu hình xong, luồng đi sẽ như sau:

**App → localhost:3306 (HAProxy) → ProxySQL → MySQL Primary/Replica**

### Test kết nối từ ngoài

Dùng tool quản lý DB (DBeaver, Navicat) hoặc terminal kết nối thử:

```bash
# Kết nối vào cổng 3306 của Host (HAProxy)
mysql -h 127.0.0.1 -P 3306 -uroot -proot_password -e "SELECT @@hostname;"
```

Nếu chạy nhiều lần lệnh trên mà thấy Hostname trả về thay đổi giữa `mysql-primary` và `mysql-replica` (khi chạy câu SELECT) nghĩa là Load Balancing đã thành công!

---

## BƯỚC 7: XỬ LÝ ERRANT GTID (NẾU CÓ CẢNH BÁO)

Nếu Orchestrator báo **ErrantGTIDStructureWarning**, có nghĩa là có transaction chạy trên Replica mà không có trên Primary. Đây là cách fix:

### Cách 1: Reset GTID trên Replica (Khuyến nghị - Đơn giản nhất)

```bash
# 1. Dừng replication trên Replica
docker exec -it mysql-replica mysql -uroot -proot_password -e "
STOP REPLICA;
RESET MASTER;
SET GLOBAL GTID_PURGED = '4210e3db-d85c-11f0-bb28-26455b83e65e:1-130';
"

# 2. Cấu hình lại replication
docker exec -it mysql-replica mysql -uroot -proot_password -e "
CHANGE REPLICATION SOURCE TO
  SOURCE_HOST='mysql-primary',
  SOURCE_USER='repl',
  SOURCE_PASSWORD='repl_password',
  SOURCE_AUTO_POSITION=1;
START REPLICA;
"

# 3. Kiểm tra trạng thái
docker exec -it mysql-replica mysql -uroot -proot_password -e "SHOW REPLICA STATUS\G"
```

### Cách 2: Inject GTID vào Primary (Bỏ qua Errant GTID - Khuyến nghị khi Cách 1 không hiệu quả)

```bash
# 1. Xem GTID nào đang errant trên Replica
docker exec -it mysql-replica mysql -uroot -proot_password -e "SELECT @@gtid_executed;"
# Hoặc xem trên Orchestrator UI

# 2. Giả sử GTID errant là: abc123-...-...:1-5
# Ta sẽ inject GTID này vào Primary để Primary "giả vờ" đã chạy transaction đó

docker exec -it mysql-primary mysql -uroot -proot_password -e "
SET GTID_NEXT='abc123-...-...:1';
BEGIN; COMMIT;
SET GTID_NEXT='abc123-...-...:2';
BEGIN; COMMIT;
SET GTID_NEXT='abc123-...-...:3';
BEGIN; COMMIT;
SET GTID_NEXT='abc123-...-...:4';
BEGIN; COMMIT;
SET GTID_NEXT='abc123-...-...:5';
BEGIN; COMMIT;
SET GTID_NEXT='AUTOMATIC';
"

# 3. Sau đó replication sẽ tự bỏ qua các GTID này
# Kiểm tra lại trên Orchestrator - cảnh báo sẽ mất
```

**Lưu ý:** Thay `abc123-...-...` bằng UUID thật từ output của bước 1.

### Cách 3: Xóa hoàn toàn và rebuild từ đầu

```bash
# 1. Backup dữ liệu từ Primary
docker exec mysql-primary mysqldump -uroot -proot_password --all-databases --triggers --routines --events --single-transaction --master-data=2 > backup.sql

# 2. Restore vào Replica
docker exec -i mysql-replica mysql -uroot -proot_password < backup.sql

# 3. Cấu hình lại replication (như Bước 3)
docker exec -it mysql-replica mysql -uroot -proot_password -e "
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

### Nguyên nhân gây Errant GTID:

- ❌ Có câu lệnh INSERT/UPDATE/DELETE chạy trực tiếp trên Replica
- ❌ Có user khác (không phải từ replication) ghi dữ liệu vào Replica
- ❌ Replica không được set `read_only=ON`

### Phòng tránh:

Đảm bảo Replica luôn ở chế độ read-only:

```bash
docker exec -it mysql-replica mysql -uroot -proot_password -e "SET GLOBAL read_only = ON; SET GLOBAL super_read_only = ON;"
```

---

## Ghi chú cho Spring Boot

Trong file `application.properties`, bạn giữ nguyên cấu hình này là đúng:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/n8n_db?useSSL=false
```