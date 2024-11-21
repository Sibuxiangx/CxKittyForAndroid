package com.termux.shared.termux.shell;

import android.app.Activity;
import android.util.SparseArray;
import android.widget.ArrayAdapter;


import com.termux.terminal.TerminalSession;

public class TermuxShellManager {
    private static int SHELL_ID = 0;

    /**
     * The foreground TermuxSessions which this service manages.
     * Note that this list is observed by an activity, like TermuxActivity.mTermuxSessionListViewController,
     * so any changes must be made on the UI thread and followed by a call to
     * {@link ArrayAdapter#notifyDataSetChanged()}.
     */
    public final SparseArray<TerminalSession> mTermuxSessions = new SparseArray<>();

    public TermuxShellManager() {
    }

    public TerminalSession createTermuxSession(Activity activity) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public TerminalSession getTermuxSession(int shellId) {
        return mTermuxSessions.get(shellId);
    }

    public void removeTermuxSession(int shellId) {
        mTermuxSessions.remove(shellId);
    }

    public static synchronized int getNextShellId() {
        return SHELL_ID++;
    }
}