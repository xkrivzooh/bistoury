/*
 * Copyright (C) 2019 Qunar, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package qunar.tc.bistoury.commands.arthas.telnet;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.commons.net.telnet.TelnetClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qunar.tc.bistoury.agent.common.pid.PidUtils;
import qunar.tc.bistoury.commands.arthas.ArthasEntity;
import qunar.tc.bistoury.commands.arthas.ArthasTelnetPortHelper;
import qunar.tc.bistoury.commands.arthas.TelnetConstants;
import qunar.tc.bistoury.common.BistouryConstants;

import java.io.IOException;
import java.util.Objects;

/**
 * @author zhenyu.nie created on 2018 2018/10/15 19:07
 */
public abstract class AbstractTelnetStore implements TelnetStore {

    private static final Logger logger = LoggerFactory.getLogger(AbstractTelnetStore.class);

    private static final int MAX_ILLEGAL_VERSION_COUNT = 2;

    private enum CheckVersion {
        check, notCheck
    }

    private static final LoadingCache<ArthasEntityCacheKey, ArthasEntity> ARTHAS_ENTITY_CACHE = CacheBuilder.newBuilder()
            .build(new CacheLoader<ArthasEntityCacheKey, ArthasEntity>() {
                @Override
                public ArthasEntity load(ArthasEntityCacheKey cacheKey) throws Exception {
                    return new ArthasEntity(cacheKey.getNullableAppCode(), cacheKey.getPid());
                }
            });


    protected AbstractTelnetStore() {
    }

    @Override
    public Telnet getTelnet(String nullableAppCode, int pid) throws Exception {
        int illegalVersionCount = 0;
        while (illegalVersionCount < MAX_ILLEGAL_VERSION_COUNT) {
            try {
                TelnetClient client = doGetTelnet(nullableAppCode, pid);
                return createTelnet(client, CheckVersion.check);
            } catch (IllegalVersionException e) {
                sleepSec(3);
                illegalVersionCount++;
            }
        }
        logger.error("illegal version can not resolved");
        throw new IllegalVersionException();
    }

    private void sleepSec(int seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private Telnet createTelnet(TelnetClient client, CheckVersion checkVersion) throws IOException {
        Telnet telnet = doCreateTelnet(client);
        String version = telnet.getVersion();
        if (checkVersion == CheckVersion.check && versionIllegal(version)) {
            return doWithIllegalVersion(telnet, version);
        } else {
            return telnet;
        }
    }

    private Telnet doWithIllegalVersion(Telnet telnet, String version) {
        logger.warn("bistoury version illegal, current [{}], get [{}]", BistouryConstants.CURRENT_VERSION, version);
        try {
            telnet.write(BistouryConstants.SHUTDOWN_COMMAND);
        } catch (Exception e) {
            // ignore
        } finally {
            telnet.close();
        }
        throw new IllegalVersionException();
    }

    private boolean versionIllegal(String version) {
        return !BistouryConstants.CURRENT_VERSION.equals(version);
    }

    protected abstract Telnet doCreateTelnet(TelnetClient client) throws IOException;

    private synchronized TelnetClient doGetTelnet(String nullableAppCode, int pid) {
        TelnetClient client = tryGetClient(nullableAppCode, pid);
        if (client != null) {
            return client;
        }

        try {
            try {
                return createClient(nullableAppCode, pid);
            } catch (Exception e) {
                return forceCreateClient(nullableAppCode, pid);
            }
        } catch (Exception e) {
            resetClient(nullableAppCode, pid);
            ArthasTelnetPortHelper.resetTelnetPort(nullableAppCode);
            throw new IllegalStateException("can not init bistoury, " + e.getMessage(), e);
        }
    }

    private TelnetClient tryGetClient(String nullableAppCode, int pid) {
        try {
            return createClient(nullableAppCode, pid);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Telnet tryGetTelnet(String nullableAppCode) throws Exception {
        int pid = PidUtils.getPid(nullableAppCode);
        TelnetClient client = tryGetClient(nullableAppCode, pid);
        if (client != null) {
            return createTelnet(client, CheckVersion.notCheck);
        }
        return null;
    }

    private void resetClient(String nullableAppCode, int pid) {
        ArthasEntityCacheKey cacheKey = new ArthasEntityCacheKey(nullableAppCode, pid);
        ARTHAS_ENTITY_CACHE.invalidate(cacheKey);
    }

    private TelnetClient createClient(String nullableAppCode, int pid) throws IOException {
        ArthasEntityCacheKey cacheKey = new ArthasEntityCacheKey(nullableAppCode, pid);
        if (ARTHAS_ENTITY_CACHE.asMap().containsKey(cacheKey)) {
            return createClient(nullableAppCode);
        } else {
            return forceCreateClient(nullableAppCode, pid);
        }
    }

    private TelnetClient forceCreateClient(String nullableAppCode, int pid) throws IOException {
        startArthas(nullableAppCode, pid);
        return createClient(nullableAppCode);
    }

    private TelnetClient createClient(String nullableAppCode) throws IOException {
        int pid = PidUtils.getPid(nullableAppCode);
        startArthas(nullableAppCode, pid);

        TelnetClient client = new TelnetClient();
        client.setConnectTimeout(TelnetConstants.TELNET_CONNECT_TIMEOUT);
        client.connect(TelnetConstants.TELNET_CONNECTION_IP, ArthasTelnetPortHelper.getTelnetPort(nullableAppCode));
        return client;
    }

    private void startArthas(String nullableAppCode, int pid) {
        ArthasEntity arthasEntity = ARTHAS_ENTITY_CACHE.getUnchecked(new ArthasEntityCacheKey(nullableAppCode, pid));
        arthasEntity.start();
    }

    private static final class ArthasEntityCacheKey {
        private final String nullableAppCode;
        private final int pid;

        ArthasEntityCacheKey(String nullableAppCode, int pid) {
            this.nullableAppCode = nullableAppCode;
            this.pid = pid;
        }

        public String getNullableAppCode() {
            return nullableAppCode;
        }

        public int getPid() {
            return pid;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ArthasEntityCacheKey that = (ArthasEntityCacheKey) o;
            return pid == that.pid &&
                    Objects.equals(nullableAppCode, that.nullableAppCode);
        }

        @Override
        public int hashCode() {
            return Objects.hash(nullableAppCode, pid);
        }
    }
}
