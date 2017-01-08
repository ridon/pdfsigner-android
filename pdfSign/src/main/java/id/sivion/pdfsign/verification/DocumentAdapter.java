package id.sivion.pdfsign.verification;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.spongycastle.tsp.TimeStampToken;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import id.sivion.pdfsign.R;

/**
 * Created by miftakhul on 29/11/16.
 */

public class DocumentAdapter extends RecyclerView.Adapter<DocumentAdapter.ViewHolder> {

    private List<SignInfo> signInfos = new ArrayList<>();
    private Context context;
    private SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

    public DocumentAdapter(Context context) {
        this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.document_detail, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String status;
        if (signInfos.get(position).isSignStatus()) {
            status = "Terverifikasi";
            holder.textSignStatus.setTextColor(ContextCompat.getColor(context, R.color.colorGreen));
        } else {
            status = "Tidak Terverifikasi";
            holder.textSignStatus.setTextColor(ContextCompat.getColor(context, R.color.colorRed));
        }

        TimeStampToken timeStampToken = signInfos.get(position).getTimesTamp();
        String timestamp = "";

        if (timeStampToken != null) {

            String time = format.format(timeStampToken.getTimeStampInfo().getGenTime());
            timestamp += time + "\n" +
                    "Ketepatan Waktu\n";

            String accuray = "";
            String seconds = "";
            String milis = "";
            String micros = "";
            if (timeStampToken.getTimeStampInfo().getGenTimeAccuracy() != null) {
                seconds = Integer.toString(timeStampToken.getTimeStampInfo().getGenTimeAccuracy().getSeconds());
                milis = Integer.toString(timeStampToken.getTimeStampInfo().getGenTimeAccuracy().getMillis());
                micros = Integer.toString(timeStampToken.getTimeStampInfo().getGenTimeAccuracy().getMicros());
            }

            accuray = seconds + " detik, " + milis + " mili detik, " + micros + "mikrodetik";

            timestamp += accuray;
        }


        holder.textSignStatus.setText(status);
        holder.textSignBy.setText(signInfos.get(position).getSignBy());
        holder.textLocation.setText(signInfos.get(position).getLocation());
        holder.textReason.setText(signInfos.get(position).getReason());
        holder.textLocalSignDate.setText(signInfos.get(position).getSignLocalTime());
        holder.textTimestamp.setText(timestamp);

        for (CertificateInfo cer : signInfos.get(position).getCertificateInfos()) {

            View v = LayoutInflater.from(context).inflate(R.layout.certificate_adapter, null);

            CertificateHolder cerHolder = new CertificateHolder(v);


            if (cer.isCertificateTrusted()) {
                cerHolder.certificateTrusted.setVisibility(View.VISIBLE);
            } else {
                cerHolder.certificateNotTrusted.setVisibility(View.VISIBLE);
            }

            if (cer.isCertificateVerified()) {
                cerHolder.certificateVerified.setVisibility(View.VISIBLE);
            } else {
                cerHolder.certificateNotVerified.setVisibility(View.VISIBLE);
            }

            if (cer.isCertificateValidity()) {
                cerHolder.certificateValid.setVisibility(View.VISIBLE);
            } else {
                cerHolder.certificateNotValid.setVisibility(View.VISIBLE);
            }

            cerHolder.textSerial.setText(cer.getSerial());
            cerHolder.textValidity.setText(cer.getValidity());
            cerHolder.textSubject.setText(cer.getSubject());
            cerHolder.textIssuer.setText(cer.getIssuer());
            cerHolder.textPublicKey.setText(cer.getPublicKey());
            cerHolder.textAlgorithm.setText(cer.getAlgorithm());
            cerHolder.textSha1Fingerprint.setText(cer.getFingerPrint());

            holder.certificates.addView(v);

        }


    }

    @Override
    public int getItemCount() {
        return signInfos.size();
    }


    public void addSignInfos(List<SignInfo> signInfos) {
        this.signInfos.addAll(signInfos);
        notifyDataSetChanged();
    }

    public void clear(){
        signInfos.clear();
        notifyDataSetChanged();
    }


    class ViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.text_sign_by)
        TextView textSignBy;
        @BindView(R.id.text_location)
        TextView textLocation;
        @BindView(R.id.text_sign_status)
        TextView textSignStatus;
        @BindView(R.id.text_reason)
        TextView textReason;
        @BindView(R.id.text_localSignDate)
        TextView textLocalSignDate;
        @BindView(R.id.text_timestamp)
        TextView textTimestamp;
        @BindView(R.id.certificates)
        LinearLayout certificates;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }


    }

    class CertificateHolder {
        @BindView(R.id.text_serial)
        TextView textSerial;
        @BindView(R.id.text_validity)
        TextView textValidity;
        @BindView(R.id.text_subject)
        TextView textSubject;
        @BindView(R.id.text_issuer)
        TextView textIssuer;
        @BindView(R.id.text_public_key)
        TextView textPublicKey;
        @BindView(R.id.text_algorithm)
        TextView textAlgorithm;
        @BindView(R.id.text_sha1_fingerprint)
        TextView textSha1Fingerprint;
        @BindView(R.id.validity_certificate_trusted)
        LinearLayout certificateTrusted;
        @BindView(R.id.validity_certificate_not_trusted)
        LinearLayout certificateNotTrusted;
        @BindView(R.id.validity_certificate_verified)
        LinearLayout certificateVerified;
        @BindView(R.id.validity_certificate_not_verified)
        LinearLayout certificateNotVerified;
        @BindView(R.id.validity_valid)
        LinearLayout certificateValid;
        @BindView(R.id.validity_not_valid)
        LinearLayout certificateNotValid;

        public CertificateHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }

}
