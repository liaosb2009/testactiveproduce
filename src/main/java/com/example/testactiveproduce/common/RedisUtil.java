package com.example.testactiveproduce.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Member;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @ClassName:RedisUtil
 * @Description TODO
 * @Author liao
 * @Time 2019/9/24 18:19
 */

@Component
public class RedisUtil {

    @Autowired
    private static RedisTemplate<String, Object> redisTemplate;

    private static final Long SUCCESS = 1L;
    /**
     * 获取锁
     * @param lockKey
     * @param value
     * @param expireTime：单位-秒
     * @return
     */
    public static boolean getLock(String lockKey, String value, int expireTime){
        boolean ret = false;
        try{
            String script = "if redis.call('setNx',KEYS[1],ARGV[1]) then if " +
                    "redis.call('get',KEYS[1])==ARGV[1] then return redis.call('expire',KEYS[1],ARGV[2])" +
                    " else return 0 end end";
            RedisScript<String> redisScript = new DefaultRedisScript<>(script, String.class);
            Object result = redisTemplate.execute(redisScript,Collections.singletonList(lockKey),value,expireTime);

            if(SUCCESS.equals(result)){
                return true;
            }

        }catch(Exception e){
        }
        return ret;
    }


    /**
     * 释放锁
     * @param lockKey
     * @param value
     * @return
     */
    public static boolean releaseLockNew(String lockKey, String value){

        String script = "if redis.call('get', KEYS[1]) ==" +
                " ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

        RedisScript<String> redisScript = new DefaultRedisScript<>(script, String.class);
        Object result = redisTemplate.execute(redisScript, Collections.singletonList(lockKey),value);
        if(SUCCESS.equals(result)) {
            return true;
        }
        return false;
    }

    public static boolean setBing(String lockKey, String value){

        redisTemplate.opsForHash().put("1000","name","3");
        redisTemplate.opsForHash().put("1000","age","33");
        redisTemplate.opsForHash().put("1000","userid","3111");
        //匹配获取键值对，为获取全部键对ScanOptions.NONE,ScanOptions.scanOptions().match("map1").build();
       // 匹配获取键位map1的键值对,不能模糊匹配。
        //map1为name/age/userId
        //游标查询
        redisTemplate.opsForHash().scan("1000", ScanOptions.scanOptions().match("name").build());
        Map<Object, Object> entries = redisTemplate.opsForHash().entries("1000");


        //取交集
        redisTemplate.opsForSet().intersect("","");
        //取并集
        Set<Object> union = redisTemplate.opsForSet().union("", "");

        String key ="keyRedis:_";
        redisTemplate.opsForZSet().add(key,"redis",100);
        redisTemplate.opsForZSet().add(key,"ss",90);
        //返回前三名
        redisTemplate.opsForZSet().reverseRange(key,0,2);
        //超过55分到166的数量
        Long count = redisTemplate.opsForZSet().count(key, 55, 166);
        //获取有序集合的成员数
        redisTemplate.opsForZSet().zCard(key);
        //获取地理位置,按经纬度计算距离


        //半径100米的5个人==========================
        // 半径 100米
        //将自己的经纬度上传redis
        Point point = new Point(80.111,87.211);
        Long add = redisTemplate.opsForGeo().add("user_geo",new RedisGeoCommands.GeoLocation<>("user_key",point));

        Point point2 = new Point(20.111,17.211);
        Long adds = redisTemplate.opsForGeo().add("user_geo",new RedisGeoCommands.GeoLocation<>("user_key2",point2));

        Distance distance = new Distance(100, RedisGeoCommands.DistanceUnit.METERS);
        Circle circle = new Circle(point, distance);
        // 附近5个人
        RedisGeoCommands.GeoRadiusCommandArgs geoRadiusCommandArgs =
                RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs().includeDistance().limit(5);
        GeoResults<RedisGeoCommands.GeoLocation<Object>> user_geo =
                redisTemplate.opsForGeo().radius("user_geo", circle, geoRadiusCommandArgs);
        //key:user_geo,user_key到user_key2的距离
        Distance distance1 = redisTemplate.opsForGeo().distance("user_geo", "user_key", "user_key2");
        double value1 = distance1.getValue();
        return false;
    }


// =============================common============================

    /**
     * 指定缓存失效时间
     *
     * @param key 键
     * @param time 时间(秒)
     * @return
     */
    public boolean expire(String key, long time) {

        try {

            if (time > 0) {

                redisTemplate.expire(key, time, TimeUnit.SECONDS);
            }

            return true;

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }

    /**
     * 根据key 获取过期时间
     *
     * @param key 键 不能为null
     * @return 时间(秒) 返回0代表为永久有效
     */
    public long getExpire(String key) {

        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    /**
     * 判断key是否存在
     *
     * @param key 键
     * @return true 存在 false不存在
     */
    public boolean hasKey(String key) {

        try {

            return redisTemplate.hasKey(key);

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }

    /**
     * 删除缓存
     *
     * @param key 可以传一个值 或多个
     */
    @SuppressWarnings("unchecked")
    public void del(String... key) {

        if (key != null && key.length > 0) {

            if (key.length == 1) {

                redisTemplate.delete(key[0]);

            } else {

                redisTemplate.delete(CollectionUtils.arrayToList(key));
            }
        }
    }

// ============================String=============================

    /**
     * 普通缓存获取
     *
     * @param key 键
     * @return 值
     */
    public Object get(String key) {

        return key == null ? null : redisTemplate.opsForValue().get(key);
    }

    /**
     * 普通缓存放入
     *
     * @param key 键
     * @param value 值
     * @return true成功 false失败
     */
    public boolean set(String key, Object value) {

        try {

            redisTemplate.opsForValue().set(key, value);

            return true;

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }

    /**
     * 普通缓存放入并设置时间
     *
     * @param key 键
     * @param value 值
     * @param time 时间(秒) time要大于0 如果time小于等于0 将设置无限期
     * @return true成功 false 失败
     */
    public boolean set(String key, Object value, long time) {

        try {

            if (time > 0) {

                redisTemplate.opsForValue().set(key, value, time, TimeUnit.SECONDS);

            } else {

                set(key, value);
            }

            return true;

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }

    /**
     * 递增
     *
     * @param key 键
     * @param delta 要增加几(大于0)
     * @return
     */
    public long incr(String key, long delta) {

        if (delta < 0) {
            throw new RuntimeException("递增因子必须大于0");
        }

        return redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 递减
     *
     * @param key 键
     * @param delta 要减少几(小于0)
     * @return
     */
    public long decr(String key, long delta) {

        if (delta < 0) {

            throw new RuntimeException("递减因子必须大于0");
        }

        return redisTemplate.opsForValue().increment(key, -delta);
    }

// ================================Map=================================

    /**
     * HashGet
     *
     * @param key 键 不能为null
     * @param item 项 不能为null
     * @return 值
     */
    public Object hget(String key, String item) {

        return redisTemplate.opsForHash().get(key, item);
    }

    /**
     * 获取hashKey对应的所有键值
     *
     * @param key 键
     * @return 对应的多个键值
     */
    public Map<Object, Object> hmget(String key) {

        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * HashSet
     *
     * @param key 键
     * @param map 对应多个键值
     * @return true 成功 false 失败
     */
    public boolean hmset(String key, Map<String, Object> map) {

        try {

            redisTemplate.opsForHash().putAll(key, map);

            return true;

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }

    /**
     * HashSet 并设置时间
     *
     * @param key 键
     * @param map 对应多个键值
     * @param time 时间(秒)
     * @return true成功 false失败
     */
    public boolean hmset(String key, Map<String, Object> map, long time) {

        try {

            redisTemplate.opsForHash().putAll(key, map);

            if (time > 0) {

                expire(key, time);
            }

            return true;

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }

    /**
     * 向一张hash表中放入数据,如果不存在将创建
     *
     * @param key 键
     * @param item 项
     * @param value 值
     * @return true 成功 false失败
     */
    public boolean hset(String key, String item, Object value) {

        try {

            redisTemplate.opsForHash().put(key, item, value);

            return true;

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }

    /**
     * 向一张hash表中放入数据,如果不存在将创建
     *
     * @param key 键
     * @param item 项
     * @param value 值
     * @param time 时间(秒) 注意:如果已存在的hash表有时间,这里将会替换原有的时间
     * @return true 成功 false失败
     */
    public boolean hset(String key, String item, Object value, long time) {

        try {

            redisTemplate.opsForHash().put(key, item, value);

            if (time > 0) {

                expire(key, time);
            }

            return true;

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }

    /**
     * 删除hash表中的值
     *
     * @param key 键 不能为null
     * @param item 项 可以使多个 不能为null
     */
    public void hdel(String key, Object... item) {

        redisTemplate.opsForHash().delete(key, item);
    }

    /**
     * 判断hash表中是否有该项的值
     *
     * @param key 键 不能为null
     * @param item 项 不能为null
     * @return true 存在 false不存在
     */
    public boolean hHasKey(String key, String item) {

        return redisTemplate.opsForHash().hasKey(key, item);
    }

    /**
     * hash递增 如果不存在,就会创建一个 并把新增后的值返回
     *
     * @param key 键
     * @param item 项
     * @param by 要增加几(大于0)
     * @return
     */
    public double hincr(String key, String item, double by) {

        return redisTemplate.opsForHash().increment(key, item, by);
    }

    /**
     * hash递减
     *
     * @param key 键
     * @param item 项
     * @param by 要减少记(小于0)
     * @return
     */
    public double hdecr(String key, String item, double by) {

        return redisTemplate.opsForHash().increment(key, item, -by);
    }

// ============================set=============================

    /**
     * 根据key获取Set中的所有值
     *
     * @param key 键
     * @return
     */
    public Set<Object> sGet(String key) {

        try {

            return redisTemplate.opsForSet().members(key);

        } catch (Exception e) {

            e.printStackTrace();

            return null;
        }
    }

    /**
     * 根据value从一个set中查询,是否存在
     *
     * @param key 键
     * @param value 值
     * @return true 存在 false不存在
     */
    public boolean sHasKey(String key, Object value) {

        try {

            return redisTemplate.opsForSet().isMember(key, value);

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }

    /**
     * 将数据放入set缓存
     *
     * @param key 键
     * @param values 值 可以是多个
     * @return 成功个数
     */
    public long sSet(String key, Object... values) {

        try {

            return redisTemplate.opsForSet().add(key, values);

        } catch (Exception e) {

            e.printStackTrace();

            return 0;
        }
    }

    /**
     * 将set数据放入缓存
     *
     * @param key 键
     * @param time 时间(秒)
     * @param values 值 可以是多个
     * @return 成功个数
     */
    public long sSetAndTime(String key, long time, Object... values) {

        try {

            Long count = redisTemplate.opsForSet().add(key, values);

            if (time > 0){ expire(key, time);}

            return count;

        } catch (Exception e) {

            e.printStackTrace();

            return 0;
        }
    }

    /**
     * 获取set缓存的长度
     *
     * @param key 键
     * @return
     */
    public long sGetSetSize(String key) {

        try {

            return redisTemplate.opsForSet().size(key);

        } catch (Exception e) {

            e.printStackTrace();

            return 0;
        }
    }

    /**
     * 移除值为value的
     *
     * @param key 键
     * @param values 值 可以是多个
     * @return 移除的个数
     */
    public long setRemove(String key, Object... values) {

        try {

            Long count = redisTemplate.opsForSet().remove(key, values);

            return count;

        } catch (Exception e) {

            e.printStackTrace();

            return 0;
        }
    }

// ===============================list=================================

    /**
     * 获取list缓存的内容
     *
     * @param key 键
     * @param start 开始
     * @param end 结束 0 到 -1代表所有值
     * @return
     */
    public List<Object> lGet(String key, long start, long end) {

        try {

            return redisTemplate.opsForList().range(key, start, end);

        } catch (Exception e) {

            e.printStackTrace();

            return null;
        }
    }

    /**
     * 获取list缓存的长度
     *
     * @param key 键
     * @return
     */
    public long lGetListSize(String key) {

        try {

            return redisTemplate.opsForList().size(key);

        } catch (Exception e) {

            e.printStackTrace();

            return 0;
        }
    }

    /**
     * 通过索引 获取list中的值
     *
     * @param key 键
     * @param index 索引 index>=0时， 0 表头，1 第二个元素，依次类推；index<0时，-1，表尾，-2倒数第二个元素，依次类推
     * @return
     */
    public Object lGetIndex(String key, long index) {

        try {

            return redisTemplate.opsForList().index(key, index);

        } catch (Exception e) {

            e.printStackTrace();

            return null;
        }
    }

    /**
     * 将list放入缓存
     *
     * @param key 键
     * @param value 值
     * @param
     * @return
     */
    public boolean lSet(String key, Object value) {

        try {

            redisTemplate.opsForList().rightPush(key, value);

            return true;

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }

    /**
     * 将list放入缓存
     *
     * @param key 键
     * @param value 值
     * @param time 时间(秒)
     * @return
     */
    public boolean lSet(String key, Object value, long time) {

        try {

            redisTemplate.opsForList().rightPush(key, value);

            if (time > 0) expire(key, time);

            return true;

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }

    /**
     * 将list放入缓存
     *
     * @param key 键
     * @param value 值

     * @return
     */
    public boolean lSet(String key, List<Object> value) {

        try {

            redisTemplate.opsForList().rightPushAll(key, value);

            return true;

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }

    /**
     * 将list放入缓存
     *
     * @param key 键
     * @param value 值
     * @param time 时间(秒)
     * @return
     */
    public boolean lSet(String key, List<Object> value, long time) {

        try {

            redisTemplate.opsForList().rightPushAll(key, value);

            if (time > 0){
                expire(key, time);
            }

            return true;

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }

    /**
     * 根据索引修改list中的某条数据
     *
     * @param key 键
     * @param index 索引
     * @param value 值
     * @return
     */
    public boolean lUpdateIndex(String key, long index, Object value) {

        try {

            redisTemplate.opsForList().set(key, index, value);

            return true;

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }

    /**
     * 移除N个值为value
     *
     * @param key 键
     * @param count 移除多少个
     * @param value 值
     * @return 移除的个数
     */
    public long lRemove(String key, long count, Object value) {

        try {

            Long remove = redisTemplate.opsForList().remove(key, count, value);

            return remove;

        } catch (Exception e) {

            e.printStackTrace();

            return 0;
        }
    }
}