-- e.g.
-- redis-cli --eval pull_task.lua <SortedSet key> , <current timestamp> <limit> <default delay>

local result = {}
local score = tonumber(ARGV[1])
local tmp = redis.call('ZRANGE', KEYS[1], 0, ARGV[2], 'WITHSCORES')
for i=1, #tmp, 2 do
    if tonumber(tmp[i+1]) <= score then
        table.insert(result, tmp[i])
        redis.call('ZINCRBY', KEYS[1], ARGV[3], tmp[i])
    end
end
return result