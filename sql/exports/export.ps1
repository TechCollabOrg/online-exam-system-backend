mysqldump `
  --host=127.0.0.1 --port=3306 --user=mysql --password=mysql `
  --default-character-set=utf8mb4 `
  --single-transaction --routines --triggers --events `
  --set-gtid-purged=OFF --add-drop-database `
  --databases db_exam `
  --result-file="...\exports\db_exam_full.sql"