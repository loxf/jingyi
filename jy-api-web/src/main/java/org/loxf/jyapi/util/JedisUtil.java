package org.loxf.jyapi.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisException;

import java.io.*;
import java.util.List;

/**
 * Jedis Cache 工具类
 */
public class JedisUtil {

    private Logger logger = LoggerFactory.getLogger(JedisUtil.class);

    private String nameSpace;

    private JedisPool jedisPool;

    public JedisUtil(JedisPool jedisPool, String nameSpace) {
        this.jedisPool = jedisPool;
        this.nameSpace = nameSpace;
    }

    /**
     * 获取缓存
     *
     * @param key 键
     * @return 值
     */
    public String get(String key) {
        String value = null;
        Jedis jedis = null;
        try {
            jedis = getResource();
            if (jedis.exists(getRuleKey(key))) {
                value = jedis.get(getRuleKey(key));
                value = StringUtils.isNotBlank(value)
                        && !"nil".equalsIgnoreCase(value) ? value : null;
                logger.debug("get {} = {}", getRuleKey(key), value);
            }
        } catch (Exception e) {
            logger.warn("get {} = {}", getRuleKey(key), value, e);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return value;
    }

    /**
     * 设置缓存(永久)
     *
     * @param key
     * @param value
     * @return
     */
    public String set(String key, String value) {
        return set(key, value, 0);
    }

    /**
     * 设置缓存
     *
     * @param key          键
     * @param value        值
     * @param cacheSeconds 超时时间，0为不超时
     * @return
     */
    public String set(String key, String value, int cacheSeconds) {
        String result = null;
        Jedis jedis = null;
        try {
            jedis = getResource();
            result = jedis.set(getRuleKey(key), value);
            if (cacheSeconds != 0) {
                jedis.expire(getRuleKey(key), cacheSeconds);
            }
            logger.debug("set {} = {}", getRuleKey(key), value);
        } catch (Exception e) {
            logger.warn("set {} = {}", getRuleKey(key), value, e);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return result;
    }


    /**
     * 删除缓存
     *
     * @param key 键
     * @return
     */
    public long del(String key) {
        long result = 0;
        Jedis jedis = null;
        try {
            jedis = getResource();
            if (jedis.exists(getRuleKey(key))) {
                result = jedis.del(getRuleKey(key));
                logger.debug("del {}", getRuleKey(key));
            } else {
                logger.debug("del {} not exists", getRuleKey(key));
            }
        } catch (Exception e) {
            logger.warn("del {}", getRuleKey(key), e);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return result;
    }

    public void rpush(String key, String value) {
        Jedis jedis = null;
        try {
            if (StringUtils.isBlank(key) || StringUtils.isBlank(value)) {
                return;
            }
            jedis = getResource();
            jedis.rpush(getRuleKey(key), value);
        } catch (Exception e) {
            logger.error("rpush {} = {}", getRuleKey(key), value, e);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    public String lpop(String key) {
        Jedis jedis = null;
        try {
            if (StringUtils.isBlank(key)) {
                return null;
            }
            jedis = getResource();
            return jedis.lpop(getRuleKey(key));
        } catch (Exception e) {
            logger.error("lpop {}", getRuleKey(key), e);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return null;
    }

    /**
     * 缓存是否存在
     *
     * @param key 键
     * @return
     */
    public boolean exists(String key) {
        boolean result = false;
        Jedis jedis = null;
        try {
            jedis = getResource();
            result = jedis.exists(getRuleKey(key));
        } catch (Exception e) {
            logger.warn("exists {}", getRuleKey(key), e);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return result;
    }

    /**
     * 获取资源
     *
     * @return
     * @throws JedisException
     */
    private Jedis getResource() throws JedisException {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
        } catch (JedisException e) {
            logger.warn("getResource.", e);
            throw e;
        }
        return jedis;
    }

    /**
     * 从map缓存中获取对象
     *
     * @return
     * @throws Exception
     */
    public Object getFromMap(String type, String key) {
        Jedis jedis = getJedis();
        try {
            List<byte[]> data = jedis.hmget(type.getBytes(), getRuleKey(key).getBytes());
            if (data == null || data.isEmpty()) {
                return null;
            }
            return deserialize(data.get(0));
        } finally {
            if (jedis != null) {
                jedis.close();//返回连接池
            }
        }
    }

    public Boolean putMap(String type, String key, Object data) throws Exception {
        if (data == null) {
            return false;
        }
        Jedis jedis = getJedis();
        try {
            jedis.hset(type.getBytes(), getRuleKey(key).getBytes(), serialize(data));
            return true;
        } finally {
            if (jedis != null) {
                jedis.close();//返回连接池
            }
        }
    }

    public Boolean delFromMap(String type, String key) throws Exception {
        Jedis jedis = getJedis();
        try {
            jedis.hdel(type.getBytes(), getRuleKey(key).getBytes());
            return true;
        } finally {
            if (jedis != null) {
                jedis.close();//返回连接池
            }
        }
    }

    private static byte[] serialize(Object value) {
        if (value == null) {
            return null;
        }
        byte[] result = null;
        ByteArrayOutputStream bos = null;
        ObjectOutputStream os = null;
        try {
            bos = new ByteArrayOutputStream();
            os = new ObjectOutputStream(bos);
            os.writeObject(value);
            os.close();
            bos.close();
            result = bos.toByteArray();
        } catch (IOException e) {
            throw new IllegalArgumentException("Non-serializable object", e);
        } finally {
            close(os);
            close(bos);
        }
        return result;
    }

    private static Object deserialize(byte[] in) {
        Object result = null;
        ByteArrayInputStream bis = null;
        ObjectInputStream is = null;
        try {
            if (in != null) {
                bis = new ByteArrayInputStream(in);
                is = new ObjectInputStream(bis);
                result = (Object) is.readObject();
                is.close();
                bis.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            close(is);
            close(bis);
        }
        return result;
    }

    private static void close(InputStream is) {
        if (is != null) {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void close(OutputStream is) {
        if (is != null) {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 执行锁（等待timeout时长来获取锁，timeout<0时，默认等待1分钟）
     *
     * @param lockName      锁名
     * @param code          CODE[建议生成一个随机不容易重复的值，用于日志查看，可以为空]
     * @param expireMSecond 锁的有效时间（毫秒）
     * @param timeout       等待获取锁的时间 需要大于等于0
     * @param runnable      需要执行的业务
     * @return 值
     */
    public void lock(String lockName, String code, long expireMSecond, long timeout, Runnable runnable) {
        if (expireMSecond < 0) {
            throw new RuntimeException("timeout不能小于0.");
        }
        Jedis jedis = null;
        try {
            jedis = getResource();
            JedisLock lock = null;
            if (timeout < 0) {
                lock = new JedisLock(jedis, getRuleKey(lockName));
            } else {
                lock = new JedisLock(jedis, getRuleKey(lockName), timeout);
            }
            if (lock.acquire(expireMSecond)) {
                logger.debug(getRuleKey(lockName) + "[" + code + "]" + "成功获取锁---------------------");
                try {
                    // 业务逻辑
                    runnable.run();// 不异步
                } catch (Exception e) {
                    logger.warn("lock {} [{}] failed", getRuleKey(lockName), code, e);
                } finally {
                    lock.release();
                    lock = null;
                    logger.debug(getRuleKey(lockName) + "[" + code + "]" + "释放锁---------------------");
                }
            } else {
                logger.debug(getRuleKey(lockName) + "[" + code + "]" + "未获取锁---------------------");
            }
        } catch (Exception e) {
            logger.warn("lock {} [{}] failed", getRuleKey(lockName), code, e);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    /**
     * 执行锁(不等待获取锁)
     *
     * @param lockName      锁名
     * @param code          CODE[建议生成一个随机不容易重复的值，用于日志查看，可以为空]
     * @param expireMSecond 锁的有效时间（毫秒）
     * @param runnable      需要执行的业务
     * @return 值
     */
    public void lock(String lockName, String code, long expireMSecond, Runnable runnable) {
        lock(lockName, code, expireMSecond, 0, runnable);
    }

    /**
     * 设置缓存 set not exists
     *
     * @param key          键
     * @param value        值
     * @param cacheSeconds 超时时间，0为不超时
     * @return
     */
    public long setnx(String key, String value, int cacheSeconds) {
        Jedis jedis = null;
        long result = 0;
        try {
            jedis = getResource();
            result = jedis.setnx(getRuleKey(key), value);
            if (cacheSeconds != 0) {
                jedis.expire(getRuleKey(key), cacheSeconds);
            }
            logger.debug("setnx {} = {}", getRuleKey(key), value);
        } catch (Exception e) {
            logger.warn("setnx {} = {}", getRuleKey(key), value, e);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return result;
    }

    /**
     * 设置缓存 set not exists
     *
     * @param key   键
     * @param value 值
     * @return
     */
    public String getSet(String key, String value) {
        Jedis jedis = null;
        String result = null;
        try {
            jedis = getResource();
            result = jedis.getSet(getRuleKey(key), value);
            logger.debug("getSet {} = {}", getRuleKey(key), value);
        } catch (Exception e) {
            logger.warn("getSet {} = {}", getRuleKey(key), value, e);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return result;
    }

    /**
     * 连接池中获取连接
     *
     * @return
     */
    private Jedis getJedis() {
        Jedis jedis = jedisPool.getResource();
        return jedis;
    }

    public JedisPool getJedisPool() {
        return jedisPool;
    }

    public void setJedisPool(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    public String getNameSpace() {
        return nameSpace;
    }

    public void setNameSpace(String nameSpace) {
        this.nameSpace = nameSpace;
    }

    private String getRuleKey(String key) {
        return nameSpace + "_" + key;
    }
}
