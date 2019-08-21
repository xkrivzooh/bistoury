package qunar.tc.bistoury.commands.arthas;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qunar.tc.bistoury.agent.common.AgentConstants;
import qunar.tc.bistoury.agent.common.kv.KvDb;
import qunar.tc.bistoury.agent.common.kv.KvDbs;
import qunar.tc.bistoury.agent.common.util.AgentUtils;
import qunar.tc.bistoury.agent.common.util.NetWorkUtils;
import qunar.tc.bistoury.clientside.common.meta.MetaStore;
import qunar.tc.bistoury.clientside.common.meta.MetaStores;

import java.util.Optional;

/**
 * @author xkrivzooh
 * @since 2019/8/19
 */
public class ArthasTelnetPortHelper {

    private static final KvDb kvDb = KvDbs.getKvDb();

    private final static Logger logger = LoggerFactory.getLogger(ArthasTelnetPortHelper.class);

    public static int getTelnetPort(String nullableAppCode) {
        if (!AgentUtils.supporGetPidFromProxy()) {
            MetaStore sharedMetaStore = MetaStores.getSharedMetaStore();
            sharedMetaStore.put(AgentConstants.TELNET_CONNECT_PORT, String.valueOf(TelnetConstants.TELNET_CONNECTION_PORT));
            return TelnetConstants.TELNET_CONNECTION_PORT;
        }

        Preconditions.checkArgument(!Strings.isNullOrEmpty(nullableAppCode), "appCode必须不能为空");

        Optional<Integer> portOptional = getPortFromPersistentStore(nullableAppCode);
        if (!portOptional.isPresent()) {
            int availablePort = NetWorkUtils.getAvailablePort();
            saveToPersistentStore(nullableAppCode, String.valueOf(availablePort));
            logger.info("应用[{}]选择的telnet port为[{}]", nullableAppCode, availablePort);
            return availablePort;
        }

        Integer port = portOptional.get();
        logger.info("应用[{}]的telnet port为[{}]", nullableAppCode, port);
        return port;
    }

    public static void resetTelnetPort(String nullableAppCode) {
        if (!AgentUtils.supporGetPidFromProxy()) {
            return;
        }

        Preconditions.checkArgument(!Strings.isNullOrEmpty(nullableAppCode), "appCode必须不能为空");
        String key = buildKey(nullableAppCode);
        saveToPersistentStore(key, "");
    }

    private static void saveToPersistentStore(String nullableAppCode, String portAsString) {
        String key = buildKey(nullableAppCode);
        kvDb.put(key, portAsString);
    }

    private static Optional<Integer> getPortFromPersistentStore(String nullableAppCode) {
        String key = buildKey(nullableAppCode);
        String portAsString = kvDb.get(key);
        if (Strings.isNullOrEmpty(portAsString)) {
            return Optional.empty();
        }
        return Optional.of(Integer.parseInt(portAsString));
    }

    private static String buildKey(String nullableAppCode) {
        return AgentConstants.APP_PERSISTENT_STORE_BASE_PREFIX +
                AgentConstants.COLON +
                Strings.nullToEmpty(nullableAppCode) +
                AgentConstants.COLON +
                AgentConstants.TELNET_CONNECT_PORT;
    }
}
