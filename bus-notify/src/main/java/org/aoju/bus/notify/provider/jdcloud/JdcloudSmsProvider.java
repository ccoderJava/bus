package org.aoju.bus.notify.provider.jdcloud;

import org.aoju.bus.notify.Context;
import org.aoju.bus.notify.magic.Message;
import org.aoju.bus.notify.provider.netease.NeteaseProvider;

/**
 * 京东云短信
 *
 * @author Kimi Liu
 * @version 6.3.2
 * @since JDK1.8+
 */
public class JdcloudSmsProvider extends NeteaseProvider<JdcloudSmsProperty, Context> {

    public JdcloudSmsProvider(Context properties) {
        super(properties);
    }

    @Override
    public Message send(JdcloudSmsProperty entity) {
        return null;
    }

}
