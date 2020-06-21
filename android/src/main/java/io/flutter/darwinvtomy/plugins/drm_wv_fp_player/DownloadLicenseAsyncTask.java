package io.flutter.darwinvtomy.plugins.drm_wv_fp_player;

import android.os.AsyncTask;

import com.google.android.exoplayer2.drm.DrmInitData;
import com.google.android.exoplayer2.drm.DrmSession;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.drm.OfflineLicenseHelper;

public class DownloadLicenseAsyncTask extends AsyncTask<DrmInitData, Void, byte[]>
{
    private OfflineLicenseHelper<FrameworkMediaCrypto> mOfflineLicenseHelper;
    private LicenseInterface mLicenseInterface;

    DownloadLicenseAsyncTask(OfflineLicenseHelper<FrameworkMediaCrypto> offlineLicenseHelper, LicenseInterface licenseInterface)
    {
        mOfflineLicenseHelper = offlineLicenseHelper;
        mLicenseInterface = licenseInterface;
    }

    @Override
    protected byte[] doInBackground(DrmInitData... drmInitData)
    {
        try
        {
            return mOfflineLicenseHelper.downloadLicense(drmInitData[0]);
        }
        catch (DrmSession.DrmSessionException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onPostExecute(byte[] bytes)
    {
        super.onPostExecute(bytes);
        mLicenseInterface.onLicenseDownloaded(bytes);
    }

    public interface LicenseInterface
    {
        void onLicenseDownloaded(byte[] keySetId);
    }
}
