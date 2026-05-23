-- 修正单日在线时长异常偏大（旧版心跳未封顶导致；执行一次即可）
-- 将单日累计秒数限制在 24 小时（86400 秒）以内
UPDATE t_user_daily_login_duration
SET total_seconds = 86400
WHERE total_seconds > 86400;
