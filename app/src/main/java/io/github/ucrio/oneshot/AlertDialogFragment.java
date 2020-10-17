package io.github.ucrio.oneshot;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

public class AlertDialogFragment extends DialogFragment {

    ClassifierActivity context;
    private int type;
    private int max1;
    private int max2;

    public AlertDialogFragment(ClassifierActivity context, int type, int max1, int max2) {
        super();
        this.context = context;
        this.type = type;
        this.max1 = max1;
        this.max2 = max2;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String title = getString(R.string.alert_title);
        String quit = getString(R.string.alert_quit);
        String cont = getString(R.string.alert_continue);
        String msg1 = getString(R.string.alert_msg1, max1);
        String msg2 = getString(R.string.alert_msg2, max2);

        AlertDialog.Builder alert = new AlertDialog.Builder(context)
                .setTitle(title)
                .setNegativeButton(quit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        context.finish();
                    }
                });

        if (type == 0) {
            alert = alert.setMessage(msg1).setPositiveButton(cont, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    context.pause(false);
                    context.readyForNextImage();
                }
            });
        } else {
            alert = alert.setMessage(msg2);
        }

        this.setCancelable(false);

        return alert.create();
    }

    @Override
    public void onPause() {
        super.onPause();
        dismiss();
    }
}