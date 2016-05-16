package ru.sawim.ui.fragment;

import android.support.v7.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import protocol.Protocol;
import ru.sawim.R;
import ru.sawim.comm.StringConvertor;
import ru.sawim.io.BlobStorage;
import ru.sawim.io.DatabaseHelper;
import ru.sawim.ui.adapter.XStatusesAdapter;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 19.05.13
 * Time: 15:45
 * To change this template use File | Settings | File Templates.
 */
public class XStatusesFragment extends DialogFragment {

    private XStatusesAdapter statusesAdapter;
    protected String[] xst_titles = new String[100];
    protected String[] xst_descs = new String[100];
    private Protocol protocol;

    public XStatusesFragment init(Protocol p) {
        protocol = p;
        load();
        return this;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context context = getActivity();

        View dialogView = LayoutInflater.from(context).inflate(R.layout.statuses_view, null);
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setView(dialogView);
        dialogBuilder.setTitle(R.string.ms_xstatus_menu);
        statusesAdapter = new XStatusesAdapter(protocol);
        final ListView lv = (ListView) dialogView.findViewById(R.id.statuses_view);
        statusesAdapter.setSelectedItem(protocol.getProfile().xstatusIndex + 1);
        lv.setAdapter(statusesAdapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, final int position, long l) {
                if (position == 0) {
                    setXStatus(-1, "", "");
                    dismiss();
                    return;
                }

                final View dialogXStatusText = LayoutInflater.from(context).inflate(R.layout.xtraz_text, null);
                final EditText editTitle = (EditText) dialogXStatusText.findViewById(R.id.xstatus_title_edit);
                final EditText editDesciption = (EditText) dialogXStatusText.findViewById(R.id.xstatus_description_edit);
                AlertDialog.Builder dialogXStatusTextBuilder = new AlertDialog.Builder(getActivity());
                dialogXStatusTextBuilder.setView(dialogXStatusText);
                dialogXStatusTextBuilder.setTitle(protocol.getXStatusInfo().getName(position - 1));
                dialogXStatusTextBuilder.setPositiveButton(getString(R.string.save), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        setXStatus(position - 1, editTitle.getText().toString(), editDesciption.getText().toString());
                        statusesAdapter.setSelectedItem(position - 1);
                        XStatusesFragment.this.dismiss();
                    }
                });
                dialogXStatusTextBuilder.setNegativeButton(android.R.string.cancel, null);
                editTitle.setText(xst_titles[position - 1]);
                editDesciption.setText(xst_descs[position - 1]);
                dialogXStatusTextBuilder.create().show();
            }
        });
        return dialogBuilder.create();
    }

    private void load() {
        try {
            BlobStorage storage = new BlobStorage(DatabaseHelper.TABLE_ANSWERER);
            storage.loadXStatuses(xst_titles, xst_descs);
            storage.close();
        } catch (Exception ignored) {
        }
    }

    private final void setXStatus(int xstatus, String title, String desc) {
        if (0 <= xstatus) {
            xst_titles[xstatus] = StringConvertor.notNull(title);
            xst_descs[xstatus] = StringConvertor.notNull(desc);
            try {
                BlobStorage storage = new BlobStorage(DatabaseHelper.TABLE_ANSWERER);
                storage.saveXStatuses(xst_titles, xst_descs);
                storage.close();
            } catch (Exception ignored) {
            }
        }
        protocol.setXStatus(xstatus, title, desc);
    }
}