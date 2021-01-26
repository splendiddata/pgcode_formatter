/*
 * Copyright (c) Splendid Data Product Development B.V. 2020
 *
 * This program is free software: You may redistribute and/or modify under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3 of the License, or (at Client's option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, Client should
 * obtain one via www.gnu.org/licenses/.
 */

package com.splendiddata.pgcode.formatter.util;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Contains a key that is to be resolved in a ResourceBundle
 *
 * @author Splendid Data Product Development B.V.
 */
public final class MsgKey implements Serializable {
    private static final long serialVersionUID = 2L;
    private static final transient ReadWriteLock CACHE_LOCK = new ReentrantReadWriteLock();
    private static final transient Map<String, com.splendiddata.pgcode.formatter.util.MsgKey> CACHE = new HashMap<>();

    private final String key;

    /**
     * Constructor
     * <p>
     * To obtain a MsgKey, please invoke {@link #valueOf(String)}
     * </p>
     *
     * @param key
     */
    private MsgKey(String key) {
        this.key = key;
    }

    /**
     * Returns the cached MsgKey for the key
     *
     * @param key
     *            Content of the key
     * @return MsgKey containing the key
     */
    public static com.splendiddata.pgcode.formatter.util.MsgKey valueOf(String key) {
        com.splendiddata.pgcode.formatter.util.MsgKey result;
        CACHE_LOCK.readLock().lock();
        try {
            result = CACHE.get(key);
        } finally {
            CACHE_LOCK.readLock().unlock();
        }
        if (result == null) {
            CACHE_LOCK.writeLock().lock();
            try {
                result = CACHE.get(key);
                if (result == null) {
                    result = new com.splendiddata.pgcode.formatter.util.MsgKey(key);
                    CACHE.put(key, result);
                }
            } finally {
                CACHE_LOCK.writeLock().unlock();
            }
        }
        return result;
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other instanceof com.splendiddata.pgcode.formatter.util.MsgKey) {
            return key.equals(((com.splendiddata.pgcode.formatter.util.MsgKey) other).key);
        }
        return false;
    }

    @Override
    public String toString() {
        return key;
    }
}
