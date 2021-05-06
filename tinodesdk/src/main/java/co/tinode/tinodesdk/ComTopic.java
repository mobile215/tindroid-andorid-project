package co.tinode.tinodesdk;

import co.tinode.tinodesdk.model.Description;
import co.tinode.tinodesdk.model.MetaSetDesc;
import co.tinode.tinodesdk.model.MsgServerMeta;
import co.tinode.tinodesdk.model.MsgSetMeta;
import co.tinode.tinodesdk.model.PrivateType;
import co.tinode.tinodesdk.model.ServerMessage;
import co.tinode.tinodesdk.model.Subscription;

/**
 * Communication topic: a P2P or Group.
 */
public class ComTopic<DP> extends Topic<DP,PrivateType,DP,PrivateType> {

    public ComTopic(Tinode tinode, Subscription<DP,PrivateType> sub) {
        super(tinode, sub);
    }

    public ComTopic(Tinode tinode, String name, Description<DP,PrivateType> desc) {
        super(tinode, name, desc);
    }

    public ComTopic(Tinode tinode, String name, Listener<DP,PrivateType,DP,PrivateType> l) {
        super(tinode, name, l);
    }

    public ComTopic(Tinode tinode, Listener l, boolean isChannel) {
        //noinspection unchecked
        super(tinode, l, isChannel);
    }

    public void setComment(String comment) {
        PrivateType p = super.getPriv();
        if (p == null) {
            p = new PrivateType();
        }
        p.setComment(comment);
        super.setPriv(p);
    }

    /**
     * Read comment from the Private field.
     * @return comment or null if comment or Private is not set.
     */
    public String getComment() {
        PrivateType p = super.getPriv();
        return p != null ? p.getComment() : null;
    }

    /**
     * Checks if the topic is archived. Not all topics support archiving.
     * @return true if the topic is archived, false otherwise.
     */
    @Override
    public boolean isArchived() {
        PrivateType p = super.getPriv();
        Boolean arch = (p != null ? p.isArchived() : Boolean.FALSE);
        return arch != null ? arch : false;
    }

    /**
     * Checks if the topic is archived. Not all topics support archiving.
     * @return true if the topic is archived, false otherwise.
     */
    public boolean isChannel() {
        return mName != null && isChannel(mName);
    }

    /**
     * Checks if given topic name is a name of a channel.
     * @param name name to check.
     * @return true if the name is a name of a channel, false otherwise.
     */
    public static boolean isChannel(String name) {
        return name.startsWith(Tinode.TOPIC_CHN_PREFIX);
    }

    /**
     * In P2P topics get peer's subscription.
     *
     * @return peer's subscription.
     */
    public Subscription<DP, PrivateType> getPeer() {
        if (isP2PType()) {
            return super.getSubscription(getName());
        }

        return null;
    }

    /**
     * Archive topic by issuing {@link Topic#setMeta} with priv set to {arch: true/false}.
     *
     * @throws NotSubscribedException if the client is not subscribed to the topic
     * @throws NotConnectedException if there is no connection to the server
     */
    public PromisedReply<ServerMessage> updateArchived(final boolean arch) {
        PrivateType priv = new PrivateType();
        priv.setArchived(arch);
        return setMeta(new MsgSetMeta<>(new MetaSetDesc<>(null, priv)));
    }

    public static class ComListener<DP> extends Listener<DP,PrivateType,DP,PrivateType> {
        /** {meta} message received */
        public void onMeta(MsgServerMeta<DP,PrivateType,DP,PrivateType> meta) {}
        /** Called by MeTopic when topic descriptor as contact is updated */
        public void onContUpdate(Subscription<DP,PrivateType> sub) {}
    }
}
