package nerd.tuxmobil.fahrplan.congress;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialogCompat;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

interface OnCertAccepted {

    void cert_accepted();
}

public class CertificateDialogFragment extends DialogFragment {

    private OnCertAccepted listener;

    private X509Certificate[] chain;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        listener = (OnCertAccepted) activity;
    }

    private static void showErrorDialog(final int msgResId, final Activity ctx,
            final Object... args) {
        new MaterialDialogCompat.Builder(ctx).setTitle(
                ctx.getString(R.string.dlg_invalid_certificate_could_not_apply))
                .setMessage(ctx.getString(msgResId, args))
                .setPositiveButton(ctx.getString(R.string.OK),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                    int which) {
                            }
                        }).show();
    }

    private static String getFingerPrint(X509Certificate cert) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            return "SHA-1 error";
        }
        byte[] der;
        try {
            der = cert.getEncoded();
        } catch (CertificateEncodingException e) {
            return "Reading CERT error";
        }
        md.update(der);
        byte[] digest = md.digest();
        StringBuilder hash = new StringBuilder();

        for (int i = 0; i < digest.length; i++) {
            hash.append(String.format("%02x", (0xFF & digest[i])));
            if (i < (digest.length - 1)) {
                hash.append(" ");
            }
        }
        return hash.toString();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        chain = TrustManagerFactory.getLastCertChain();
        String exMessage = "Unknown Error";

        Exception ex = ((Exception) CustomHttpClient.getSSLException());
        if (ex != null) {
            if (ex.getCause() != null) {
                if (ex.getCause().getCause() != null) {
                    exMessage = ex.getCause().getCause().getMessage();

                } else {
                    exMessage = ex.getCause().getMessage();
                }
            } else {
                exMessage = ex.getMessage();
            }
        }

        StringBuffer chainInfo = new StringBuffer(100);
        int chain_len = (chain == null ? 0 : chain.length);
        for (int i = 0; i < chain_len; i++) {
            // display certificate chain information
            chainInfo.append("Certificate chain[" + i + "]:\n");
            chainInfo.append("Subject: " + chain[i].getSubjectDN().toString()).append("\n");
            chainInfo.append("Issuer: " + chain[i].getIssuerDN().toString()).append("\n");
            chainInfo.append("Issued On: " + String.format("%02d.%02d.%04d",
                    chain[i].getNotBefore().getDate(),
                    chain[i].getNotBefore().getMonth() + 1,
                    chain[i].getNotBefore().getYear() + 1900)).append("\n");
            chainInfo.append("Expires On: " + String.format("%02d.%02d.%04d",
                    chain[i].getNotAfter().getDate(),
                    chain[i].getNotAfter().getMonth() + 1,
                    chain[i].getNotAfter().getYear() + 1900)).append("\n");
            chainInfo.append("SHA1 Fingerprint: " + getFingerPrint(chain[i])).append("\n");
        }

        MaterialDialogCompat.Builder builder = new MaterialDialogCompat.Builder(getActivity())
                .setTitle(getString(R.string.dlg_invalid_certificate_title))
                .setCancelable(true)
                .setPositiveButton(getString(android.R.string.yes),

                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    if (chain != null) {
                                        TrustManagerFactory.addCertificateChain(chain);
                                    }
                                    if (listener != null) {
                                        listener.cert_accepted();
                                    }
                                } catch (CertificateException e) {
                                    showErrorDialog(R.string.dlg_certificate_message_fmt,
                                            getActivity(),
                                            e.getMessage() == null ? "" : e.getMessage());
                                }
                            }
                        })
                .setNegativeButton(getString(android.R.string.no),

                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                    int which) {
                            }
                        });

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View msgView = inflater.inflate(R.layout.cert_dialog, null);
        ((TextView) msgView.findViewById(R.id.cert)).setText(
                getString(R.string.dlg_certificate_message_fmt, exMessage) + "\n\n" + chainInfo
                        .toString());
        builder.setView(msgView);
        return builder.create();
    }

}
