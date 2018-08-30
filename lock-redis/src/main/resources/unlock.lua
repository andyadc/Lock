--
-- User: andy.an
-- Date: 2018/7/9
-- Time: 16:57
--
-- Unlock
-- del the key
--
if redis.call('get', KEY[1]) == ARGV[1] then
    return redis.call('del', KEYS[1])
else
    return 0
end
