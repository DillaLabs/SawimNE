package ru.sawim.view;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import protocol.Contact;
import protocol.Group;
import protocol.Protocol;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.Scheme;
import ru.sawim.activities.BaseActivity;
import ru.sawim.activities.SawimActivity;
import ru.sawim.listener.OnUpdateRoster;
import ru.sawim.models.RosterAdapter;
import ru.sawim.roster.ProtocolBranch;
import ru.sawim.roster.RosterHelper;
import ru.sawim.roster.TreeNode;
import ru.sawim.widget.MyListView;
import ru.sawim.widget.roster.RosterViewRoot;

/**
 * Created by gerc on 31.12.2015.
 */
public class SearchContactFragment extends SawimFragment
        implements SearchView.OnQueryTextListener, OnUpdateRoster, Handler.Callback,
        MenuItemCompat.OnActionExpandListener, View.OnClickListener {

    public static final String TAG = SearchContactFragment.class.getSimpleName();

    private static final int UPDATE_PROGRESS_BAR = 0;
    private static final int UPDATE_ROSTER = 1;
    private static final int PUT_INTO_QUEUE = 2;

    RosterViewRoot rosterViewLayout;
    Handler handler;
    SearchView searchView;
    MenuItem searchMenuItem;

    public SearchContactFragment() {
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        handler = new Handler(this);

        MyListView listView = (MyListView) LayoutInflater.from(getContext()).inflate(R.layout.recycler_view, null);
        RosterAdapter rosterAdapter = new RosterAdapter();
        rosterAdapter.setType(RosterHelper.ALL_CONTACTS);
        listView.setAdapter(rosterAdapter);
        registerForContextMenu(listView);
        rosterAdapter.setOnItemClickListener(this);
        FrameLayout.LayoutParams listViewLP = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        listViewLP.gravity = Gravity.BOTTOM;
        listView.setLayoutParams(listViewLP);

        ProgressBar progressBar = new ProgressBar(getActivity(), null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.getProgressDrawable().setBounds(progressBar.getProgressDrawable().getBounds());
        FrameLayout.LayoutParams progressBarLP = new FrameLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        progressBarLP.setMargins(30, 0, 30, 1);
        progressBarLP.gravity = Gravity.TOP;
        progressBar.setLayoutParams(progressBarLP);
        progressBar.setVisibility(View.GONE);

        rosterViewLayout = new RosterViewRoot(getActivity(), progressBar, listView);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (rosterViewLayout.getParent() != null)
            ((ViewGroup) rosterViewLayout.getParent()).removeView(rosterViewLayout);

        return rosterViewLayout;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().supportInvalidateOptionsMenu();
        setHasOptionsMenu(true);

        ActionBar actionBar = ((BaseActivity) getActivity()).getSupportActionBar();
        actionBar.setDisplayShowCustomEnabled(false);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        getRosterAdapter().setOnItemClickListener(null);
        searchView.setOnQueryTextListener(null);
        MenuItemCompat.setOnActionExpandListener(searchMenuItem, null);
        unregisterForContextMenu(getListView());
        getListView().setAdapter(null);
        searchView = null;
        handler = null;
        rosterViewLayout = null;
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case UPDATE_PROGRESS_BAR:
                updateProgressBarSync();
                break;
            case UPDATE_ROSTER:
                updateRosterSync();
                break;
            case PUT_INTO_QUEUE:
                if (getRosterAdapter() != null) {
                    getRosterAdapter().putIntoQueue((Group) msg.obj);
                }
                break;
        }
        return false;
    }

    private int oldProgressBarPercent;
    private void updateProgressBarSync() {
        final Protocol p = RosterHelper.getInstance().getProtocol(0);
        if (RosterHelper.getInstance().getProtocolCount() == 1 && p != null) {
            byte percent = p.getConnectingProgress();
            if (oldProgressBarPercent != percent) {
                oldProgressBarPercent = percent;
                BaseActivity activity = (BaseActivity) getActivity();
                if (100 != percent) {
                    rosterViewLayout.getProgressBar().setVisibility(ProgressBar.VISIBLE);
                    rosterViewLayout.getProgressBar().setProgress(percent);
                } else {
                    rosterViewLayout.getProgressBar().setVisibility(ProgressBar.GONE);
                }
                if (100 == percent || 0 == percent) {
                    activity.supportInvalidateOptionsMenu();
                }
            }
        }
    }

    private void updateRosterSync() {
        RosterHelper.getInstance().updateOptions();
        if (getRosterAdapter() != null) {
            getRosterAdapter().refreshList();
        }
    }

    @Override
    public void updateProgressBar() {
        if (handler == null) return;
        handler.sendEmptyMessage(UPDATE_PROGRESS_BAR);
    }

    @Override
    public void updateRoster() {
        if (handler == null) return;
        handler.sendEmptyMessage(UPDATE_ROSTER);
    }

    @Override
    public void putIntoQueue(final Group g) {
        if (handler == null) return;
        handler.sendMessage(Message.obtain(handler, PUT_INTO_QUEUE, g));
    }

    public void update() {
        updateRosterSync();
        updateProgressBarSync();
    }

    @Override
    public void onPause() {
        super.onPause();
        RosterHelper.getInstance().setOnUpdateRoster(null);
    }

    @Override
    public void onResume() {
        super.onResume();
        getRosterAdapter().setType(RosterHelper.ALL_CONTACTS);
        RosterHelper.getInstance().setOnUpdateRoster(this);
        update();
        getActivity().supportInvalidateOptionsMenu();
        if (Scheme.isChangeTheme(Scheme.getSavedTheme())) {
            ((SawimActivity) getActivity()).recreateActivity();
        }
    }

    @Override
    public boolean hasBack() {
        return true;
    }

    @Override
    public void onPrepareOptionsMenu_(Menu menu) {
        getActivity().getMenuInflater().inflate(R.menu.menu_search_contact, menu);

        searchMenuItem = menu.findItem(R.id.action_search);
        searchView = (SearchView) MenuItemCompat.getActionView(searchMenuItem);
        searchView.setOnQueryTextListener(this);
        searchView.setIconifiedByDefault(false);

        MenuItemCompat.setOnActionExpandListener(searchMenuItem, this);
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem item) {
        // Do something when collapsed
        getRosterAdapter().filterData(null);
        return true; // Return true to collapse action view
    }

    @Override
    public boolean onMenuItemActionExpand(MenuItem item) {
        // Do something when expanded
        return true; // Return true to expand action view
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        String query = newText.toLowerCase();
        getRosterAdapter().filterData(query);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    private void openChat(Protocol p, Contact c, String sharingText) {
        getFragmentManager().popBackStack();
        c.activate((BaseActivity) getActivity(), p);
        if (SawimApplication.isManyPane()) {
            ChatView chatViewTablet = (ChatView) getActivity().getSupportFragmentManager()
                    .findFragmentById(R.id.chat_fragment);
            chatViewTablet.pause(chatViewTablet.getCurrentChat());
            chatViewTablet.openChat(p, c);
            chatViewTablet.setSharingText(sharingText);
            chatViewTablet.resume(chatViewTablet.getCurrentChat());
            update();
        } else {
            ChatView chatView = ChatView.newInstance(p.getUserId(), c.getUserId());
            chatView.setSharingText(sharingText);
            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, chatView, ChatView.TAG);
            transaction.addToBackStack(null);
            transaction.commit();
        }
    }

    @Override
    public void onClick(View v) {
        int position = (int) v.getTag();
        TreeNode treeNode = getRosterAdapter().getItem(position);
        if (treeNode.getType() == TreeNode.CONTACT) {
            openChat(((Contact) treeNode).getProtocol(), ((Contact) treeNode), null);
        }
        if (RosterHelper.getInstance().getCurrPage() != RosterHelper.ACTIVE_CONTACTS) {
            if (treeNode.getType() == TreeNode.PROTOCOL) {
                ProtocolBranch group = (ProtocolBranch) treeNode;
                RosterHelper roster = RosterHelper.getInstance();
                final int count = roster.getProtocolCount();
                int currProtocol = 0;
                for (int i = 0; i < count; ++i) {
                    Protocol p = roster.getProtocol(i);
                    if (p == null) return;
                    ProtocolBranch root = p.getProtocolBranch(i);
                    if (root.getGroupId() != group.getGroupId()) {
                        root.setExpandFlag(false);
                    } else {
                        currProtocol = i;
                    }
                }
                group.setExpandFlag(!group.isExpanded());
                update();
                getListView().smoothScrollToPosition(currProtocol);
            } else if (treeNode.getType() == TreeNode.GROUP) {
                Group group = RosterHelper.getInstance().getGroupWithContacts((Group) treeNode);
                group.setExpandFlag(!group.isExpanded());
                update();
            }
        }
    }

    public RosterAdapter getRosterAdapter() {
        if (rosterViewLayout.getMyListView() == null) return null;
        return (RosterAdapter) getListView().getAdapter();
    }

    public RecyclerView getListView() {
        return rosterViewLayout.getMyListView();
    }
}