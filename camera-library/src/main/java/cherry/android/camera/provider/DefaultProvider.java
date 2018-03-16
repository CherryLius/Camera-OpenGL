package cherry.android.camera.provider;

import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by ROOT on 2017/8/9.
 */

public class DefaultProvider implements IProvider {
    private Context mContext;
    private SimpleDateFormat mFormat;
    private Date mDate;

    public DefaultProvider(@NonNull Context context) {
        this.mContext = context;
        this.mFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        this.mDate = new Date();
    }

    @Override
    public String filename() {
        mDate.setTime(System.currentTimeMillis());
        String fileName = mFormat.format(mDate) + ".jpg";
        File parentFile = mContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (!parentFile.exists())
            parentFile.mkdirs();
        return new File(parentFile, fileName).getAbsolutePath();
    }
}
