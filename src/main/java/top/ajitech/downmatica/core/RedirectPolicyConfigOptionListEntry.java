package top.ajitech.downmatica.core;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

import java.net.http.HttpClient;

public enum RedirectPolicyConfigOptionListEntry implements IConfigOptionListEntry {
        NEVER("never", "downmatica.config.option.redirectPolicy.never", HttpClient.Redirect.NEVER),
        ALWAYS("always", "downmatica.config.option.redirectPolicy.always", HttpClient.Redirect.ALWAYS),
        NORMAL("normal", "downmatica.config.option.redirectPolicy.normal", HttpClient.Redirect.NORMAL);

        private final String name;
        private final String translationKey;
        private final HttpClient.Redirect redirectPolicy;

        RedirectPolicyConfigOptionListEntry(String name, String translationKey, HttpClient.Redirect redirectPolicy){
            this.name = name;
            this.translationKey = translationKey;
            this.redirectPolicy = redirectPolicy;
        }

        @Override
        public String getStringValue() {
            return name;
        }

        @Override
        public String getDisplayName() {
            return StringUtils.translate(translationKey);
        }

        @Override
        public IConfigOptionListEntry cycle(boolean forward) {
            int id = ordinal();
            if (forward) {
                if (++id >= values().length) {
                    id = 0;
                }
            } else {
                if (--id < 0) {
                    id = values().length - 1;
                }
            }
            return values()[id % values().length];
        }

        @Override
        public IConfigOptionListEntry fromString(String value) {
            for (RedirectPolicyConfigOptionListEntry redirectPolicyConfigOptionListEntry : RedirectPolicyConfigOptionListEntry.values()) {
                if (redirectPolicyConfigOptionListEntry.name.equals(value)) {
                    return redirectPolicyConfigOptionListEntry;
                }
            }
            return NORMAL;
        }

        public HttpClient.Redirect getRedirectPolicy() {
            return redirectPolicy;
        }
    }