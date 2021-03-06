package protocol;

import android.view.ContextMenu;
import android.view.Menu;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.ui.activity.BaseActivity;
import ru.sawim.chat.Chat;
import ru.sawim.chat.ChatHistory;
import ru.sawim.comm.JLocale;
import ru.sawim.comm.StringConvertor;
import ru.sawim.icons.Icon;
import ru.sawim.icons.ImageList;
import ru.sawim.modules.history.HistoryStorage;
import ru.sawim.roster.RosterHelper;
import ru.sawim.roster.TreeNode;
import ru.sawim.ui.fragment.menu.MyMenu;

abstract public class Contact implements TreeNode {
    public static final ImageList serverListsIcons = ImageList.createImageList("/serverlists.png");

    protected String userId;
    private String name;
    private int groupId = Group.NOT_IN_GROUP;
    private int booleanValues;
    private byte status = StatusInfo.STATUS_OFFLINE;
    private String statusText = null;
    public short clientIndex = ClientInfo.CLI_NONE;
    private int xstatus = XStatusInfo.XSTATUS_NONE;
    private String xstatusText = null;
    String version = "";
    public long chaingingStatusTime = 0;
    public String avatarHash;
    private long lastMessageTransmitted;
    private boolean hasMessagesLeftOnServer = true;
    public String firstServerMsgId;

    public static int getStatusColor(byte status) {
        switch (status) {
            case StatusInfo.STATUS_OFFLINE:
                return SawimApplication.getInstance().getResources().getColor(R.color.status_offline);
            case StatusInfo.STATUS_ONLINE:
            case StatusInfo.STATUS_CHAT:
                return SawimApplication.getInstance().getResources().getColor(R.color.status_online);
            case StatusInfo.STATUS_AWAY:
            case StatusInfo.STATUS_NA:
                return SawimApplication.getInstance().getResources().getColor(R.color.status_away);
            case StatusInfo.STATUS_DND:
                return SawimApplication.getInstance().getResources().getColor(R.color.status_dnd);
        }
        return SawimApplication.getInstance().getResources().getColor(R.color.status_offline);
    }

    public final boolean isOnline() {
        return (StatusInfo.STATUS_OFFLINE != status);
    }

    public void setTimeOfChaingingStatus(long time) {
        chaingingStatusTime = time;
    }

    public String annotations = null;

    public boolean isPresence() {
        return false;
    }

    public byte subcontactsS() {
        return (byte) 0;
    }

    public boolean isConference() {
        return false;
    }

    public final String getUserId() {
        return userId;
    }

    public final String getName() {
        return name;
    }

    public void setName(String newName) {
        if (!StringConvertor.isEmpty(newName)) {
            name = newName;
        }
    }

    public final void setGroupId(int id) {
        groupId = id;
    }

    @Override
    public final int getGroupId() {
        return groupId;
    }

    public final void setGroup(Group group) {
        setGroupId((null == group) ? Group.NOT_IN_GROUP : group.getGroupId());
    }

    public String getDefaultGroupName() {
        return null;
    }

    public Icon getLeftIcon(Protocol p) {
        return p.getStatusInfo().getIcon(getStatusIndex());
    }

    public Protocol getProtocol() {
        return RosterHelper.getInstance().getProtocol();
    }

    public final void setXStatus(int index, String text) {
        xstatus = index;
        xstatusText = (XStatusInfo.XSTATUS_NONE == index) ? null : text;
    }

    public final int getXStatusIndex() {
        return xstatus;
    }

    public final String getXStatusText() {
        return xstatusText;
    }

    public void setClient(short clientNum, String ver) {
        clientIndex = clientNum;
        version = StringConvertor.notNull(ver);
    }

    public void setOfflineStatus() {
        if (isOnline()) {
            setTimeOfChaingingStatus(SawimApplication.getCurrentGmtTime());
        }
        setStatus(StatusInfo.STATUS_OFFLINE, null);
        setXStatus(XStatusInfo.XSTATUS_NONE, null);
        beginTyping(false);
    }

    public final byte getStatusIndex() {
        return status;
    }

    public final String getStatusText() {
        return statusText;
    }

    public final void setStatus(byte statusIndex, String text) {
        if (!isOnline() && (StatusInfo.STATUS_OFFLINE != statusIndex)) {
            setTimeOfChaingingStatus(SawimApplication.getCurrentGmtTime());
        }
        status = statusIndex;
        statusText = (StatusInfo.STATUS_OFFLINE == status) ? null : text;
    }

    public void activate(BaseActivity activity) {
        RosterHelper.getInstance().setCurrentContact(this);
        RosterHelper.getInstance().getProtocol().oldContactIdWithMessage = null;
    }

    public static final byte CONTACT_NO_AUTH = 1 << 1;
    private static final byte CONTACT_IS_TEMP = 1 << 3;

    public static final byte SL_VISIBLE = 1 << 4;
    public static final byte SL_INVISIBLE = 1 << 5;
    public static final byte SL_IGNORE = 1 << 6;

    private static final int TYPING = 1 << 8;

    public final void setBooleanValue(byte key, boolean value) {
        if (value) {
            booleanValues |= key;
        } else {
            booleanValues &= ~key;
        }
    }

    public final boolean isTemp() {
        return (booleanValues & CONTACT_IS_TEMP) != 0;
    }

    public final boolean isAuth() {
        return (booleanValues & CONTACT_NO_AUTH) == 0;
    }

    public final void setBooleanValues(byte vals) {
        booleanValues = (booleanValues & ~0xFF) | (vals & 0x7F);
    }

    public final byte getBooleanValues() {
        return (byte) (booleanValues & 0x7F);
    }

    public final void setTempFlag(boolean isTemp) {
        setBooleanValue(Contact.CONTACT_IS_TEMP, isTemp);
    }

    public final void beginTyping(boolean typing) {
        if (typing && isOnline()) {
            booleanValues |= TYPING;
        } else {
            booleanValues &= ~TYPING;
        }
    }

    public final boolean isTyping() {
        return (booleanValues & TYPING) != 0;
    }

    public final boolean hasChat() {
        return ChatHistory.instance.getChat(this) != null;
    }

    public final void updateChatState(final Chat chat) {
        if (null != chat) {
            RosterHelper.getInstance().getProtocol().getStorage()
                    .updateUnreadMessagesCount(RosterHelper.getInstance().getProtocol().getUserId(), getUserId(), (short) chat.getAllUnreadMessageCount());
        }
    }

    public final boolean inVisibleList() {
        return (booleanValues & SL_VISIBLE) != 0;
    }

    public final boolean inInvisibleList() {
        return (booleanValues & SL_INVISIBLE) != 0;
    }

    public final boolean inIgnoreList() {
        return (booleanValues & SL_IGNORE) != 0;
    }

    protected final void initPrivacyMenu(MyMenu menu) {
        if (!isTemp()) {
            int visibleList = inVisibleList()
                    ? R.string.rem_visible_list : R.string.add_visible_list;
            int invisibleList = inInvisibleList()
                    ? R.string.rem_invisible_list : R.string.add_invisible_list;
            int ignoreList = inIgnoreList()
                    ? R.string.rem_ignore_list : R.string.add_ignore_list;

            menu.add(JLocale.getString(visibleList), ContactMenu.USER_MENU_PS_VISIBLE);
            menu.add(JLocale.getString(invisibleList), ContactMenu.USER_MENU_PS_INVISIBLE);
            menu.add(JLocale.getString(ignoreList), ContactMenu.USER_MENU_PS_IGNORE);
        }
    }

    public String getMyName() {
        return null;
    }

    public boolean isSingleUserContact() {
        return true;
    }

    public boolean isVisibleInContactList() {
        return isOnline() || hasChat() || isTemp();
    }

    public final int getTextTheme() {
        if (isTemp()) {
            return R.attr.contact_temp;
        }
        if (hasChat()) {
            return R.attr.contact_with_chat;
        }
        if (isOnline()) {
            return R.attr.contact_online;
        }
        return R.attr.contact_offline;
    }

    public final String getText() {
        return name == null ? "" : name;
    }

    @Override
    public byte getType() {
        return CONTACT;
    }

    public final int getNodeWeight() {
        Chat chat = ChatHistory.instance.getChat(this);
        boolean hasChat = chat != null;
        if (hasChat) {
            if (0 < chat.getAuthRequestCounter()) {
                return 1;
            }
            if (0 < chat.getPersonalMessageCount()) {
                return 2;
            }
            if (0 < chat.getSysNoticeCounter()) {
                return 3;
            }
            if (0 < chat.getOtherMessageCount()) {
                return 4;
            }
        }
        if (!isSingleUserContact()) {
            return isOnline() ? 9 : 50;
        }
        if (RosterHelper.SORT_BY_NAME == SawimApplication.sortType) {
            return 20;
        }
        if (isOnline()) {
            if (hasChat) {
                return 10;
            }
            switch (SawimApplication.sortType) {
                case RosterHelper.SORT_BY_STATUS:
                    return 20 + StatusInfo.getWidth(getStatusIndex());
                case RosterHelper.SORT_BY_ONLINE:
                    return 20;
            }
        }
        if (isTemp()) {
            return 60;
        }
        return 51;
    }

    protected abstract void initManageContactMenu(MyMenu menu);

    protected void initContextMenu(ContextMenu contactMenu) {
        addChatItems(contactMenu);
        addGeneralItems(contactMenu);
    }

    public void addChatMenuItems(ContextMenu model) {
    }

    protected final void addChatItems(ContextMenu menu) {
        if (isSingleUserContact()) {
            if (!isAuth()) {
                menu.add(Menu.NONE, ContactMenu.USER_MENU_REQU_AUTH, Menu.NONE, R.string.requauth);
            }
        }
        if (isSingleUserContact() || isOnline()) {
            addChatMenuItems(menu);
        }
    }

    protected final void addGeneralItems(ContextMenu menu) {
        Protocol protocol = RosterHelper.getInstance().getProtocol();
        menu.add(Menu.NONE, ContactMenu.USER_MENU_USER_INFO, Menu.NONE, R.string.user_info);
        menu.add(Menu.NONE, ContactMenu.USER_MANAGE_CONTACT, Menu.NONE, R.string.manage);
        if (isOnline()) {
            menu.add(Menu.NONE, ContactMenu.USER_MENU_STATUSES, Menu.NONE, R.string.statuses);
        }
        if (protocol.getChat(this).getHistory().getHistorySize() > 0) {
            menu.add(Menu.NONE, ContactMenu.USER_MENU_DELETE_HISTORY, Menu.NONE, R.string.delete_history);
        }
        if (hasChat()) {
            menu.add(Menu.NONE, ContactMenu.USER_MENU_CLOSE_CHAT, Menu.NONE, R.string.close);
        }
    }

    public boolean setLastMessageTransmitted(long value) {
        long before = getLastMessageTransmitted();
        if (value - before > 1000) {
            lastMessageTransmitted = value;
            return true;
        } else {
            return false;
        }
    }

    public long getLastMessageTransmitted() {
        if (lastMessageTransmitted == 0) {
            return HistoryStorage.getHistory(getProtocol().getUserId(), getUserId()).getMessageTime(true);
        }
        return lastMessageTransmitted;
    }

    public void setHasMessagesLeftOnServer(boolean hasMessagesLeftOnServer) {
        this.hasMessagesLeftOnServer = hasMessagesLeftOnServer;
    }

    public boolean isHasMessagesLeftOnServer() {
        return hasMessagesLeftOnServer;
    }
}