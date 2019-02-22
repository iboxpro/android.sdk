package ibox.pro.sdk.external.example;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import java.lang.ref.WeakReference;

public abstract class CommonAsyncTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {
    private WeakReference<Context> context;
    private ProgressDialog progressDialog;

    public CommonAsyncTask(Context context) {
        this.context = new WeakReference<>(context);
        progressDialog = new ProgressDialog(context);
    }

    public Context getContext() {
        return context.get();
    }

    @Override
    protected void onPreExecute() {
        progressDialog.setMessage(getContext().getString(R.string.progress));
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    @Override
    protected void onCancelled() {
        progressDialog.dismiss();
    }

    @Override
    protected void onPostExecute(Result result) {
        super.onPostExecute(result);
        progressDialog.dismiss();
    }
}
