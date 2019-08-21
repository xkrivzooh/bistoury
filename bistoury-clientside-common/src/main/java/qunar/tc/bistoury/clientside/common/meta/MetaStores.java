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

package qunar.tc.bistoury.clientside.common.meta;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import java.util.Date;
import java.util.Map;

/**
 * @author zhenyu.nie created on 2019 2019/1/10 15:40
 */
public class MetaStores {

    private static final MetaStore sharedMetaStore = new DefaultMetaStore(Maps.newConcurrentMap());

    private static final Map<String, MetaStore> appMetaStores = Maps.newConcurrentMap();

    public static MetaStore getSharedMetaStore() {
        return sharedMetaStore;
    }

    public static MetaStore getAppMetaStore(String appCode) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(appCode), "appCode不能为空");
        appMetaStores.putIfAbsent(appCode, new AppFirstMetaStore(sharedMetaStore, new DefaultMetaStore()));
        return appMetaStores.get(appCode);
    }


    private static class AppFirstMetaStore implements MetaStore {
        private final MetaStore sharedMetaStore;
        private final MetaStore appMetaStore;

        public AppFirstMetaStore(MetaStore sharedMetaStore, MetaStore appMetaStore) {
            Preconditions.checkNotNull(sharedMetaStore);
            if (appMetaStore == null) {
                appMetaStore = new DefaultMetaStore();
            }
            this.sharedMetaStore = sharedMetaStore;
            this.appMetaStore = appMetaStore;
        }


        @Override
        public void update(Map<String, String> attrs) {
            this.appMetaStore.update(attrs);
            this.sharedMetaStore.update(attrs);
        }

        @Override
        public void put(String key, String value) {
            this.appMetaStore.put(key, value);
            this.sharedMetaStore.put(key, value);
        }

        @Override
        public Map<String, String> getAgentInfo() {
            Map<String, String> result = Maps.newHashMap();
            Map<String, String> agentInfo = this.sharedMetaStore.getAgentInfo();
            for (Map.Entry<String, String> entry : agentInfo.entrySet()) {
                result.put(entry.getKey(), entry.getValue());
            }
            agentInfo = this.appMetaStore.getAgentInfo();
            for (Map.Entry<String, String> entry : agentInfo.entrySet()) {
                result.put(entry.getKey(), entry.getValue());
            }
            return result;
        }

        @Override
        public String getStringProperty(String name) {
            if (this.appMetaStore.containsKey(name)) {
                return this.appMetaStore.getStringProperty(name);
            }
            return this.sharedMetaStore.getStringProperty(name);
        }

        @Override
        public String getStringProperty(String name, String def) {
            if (this.appMetaStore.containsKey(name)) {
                return this.appMetaStore.getStringProperty(name);
            }
            return this.sharedMetaStore.getStringProperty(name, def);
        }

        @Override
        public boolean getBooleanProperty(String name) {
            if (this.appMetaStore.containsKey(name)) {
                return this.appMetaStore.getBooleanProperty(name);
            }
            return this.sharedMetaStore.getBooleanProperty(name);
        }

        @Override
        public boolean getBooleanProperty(String name, boolean def) {
            if (this.appMetaStore.containsKey(name)) {
                return this.appMetaStore.getBooleanProperty(name);
            }
            return this.sharedMetaStore.getBooleanProperty(name, def);
        }

        @Override
        public Date getDateProperty(String name) {
            if (this.appMetaStore.containsKey(name)) {
                return this.appMetaStore.getDateProperty(name);
            }
            return this.sharedMetaStore.getDateProperty(name);
        }

        @Override
        public Integer getIntegerProperty(String name) {
            if (this.appMetaStore.containsKey(name)) {
                return this.appMetaStore.getIntegerProperty(name);
            }
            return this.sharedMetaStore.getIntegerProperty(name);
        }

        @Override
        public int getIntProperty(String name) {
            if (this.appMetaStore.containsKey(name)) {
                return this.appMetaStore.getIntProperty(name);
            }
            return this.sharedMetaStore.getIntProperty(name);
        }

        @Override
        public int getIntProperty(String name, int def) {
            if (this.appMetaStore.containsKey(name)) {
                return this.appMetaStore.getIntProperty(name);
            }
            return this.sharedMetaStore.getIntProperty(name, def);
        }

        @Override
        public long getLongProperty(String name) {
            if (this.appMetaStore.containsKey(name)) {
                return this.appMetaStore.getLongProperty(name);
            }
            return this.sharedMetaStore.getLongProperty(name);
        }

        @Override
        public long getLongProperty(String name, long def) {
            if (this.appMetaStore.containsKey(name)) {
                return this.appMetaStore.getLongProperty(name);
            }
            return this.sharedMetaStore.getLongProperty(name, def);
        }

        @Override
        public float getFloatProperty(String name) {
            if (this.appMetaStore.containsKey(name)) {
                return this.appMetaStore.getFloatProperty(name);
            }
            return this.sharedMetaStore.getFloatProperty(name);
        }

        @Override
        public float getFloatProperty(String name, float def) {
            if (this.appMetaStore.containsKey(name)) {
                return this.appMetaStore.getFloatProperty(name);
            }
            return this.sharedMetaStore.getFloatProperty(name, def);
        }

        @Override
        public double getDoubleProperty(String name) {
            if (this.appMetaStore.containsKey(name)) {
                return this.appMetaStore.getDoubleProperty(name);
            }
            return this.sharedMetaStore.getDoubleProperty(name);
        }

        @Override
        public double getDoubleProperty(String name, double def) {
            if (this.appMetaStore.containsKey(name)) {
                return this.appMetaStore.getDoubleProperty(name);
            }
            return this.sharedMetaStore.getDoubleProperty(name, def);
        }

        @Override
        public boolean containsKey(String key) {
            return this.appMetaStore.containsKey(key) || this.sharedMetaStore.containsKey(key);
        }
    }

}
