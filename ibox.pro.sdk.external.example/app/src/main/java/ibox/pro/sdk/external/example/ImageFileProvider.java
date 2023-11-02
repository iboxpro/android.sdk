package ibox.pro.sdk.external.example;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;

public class ImageFileProvider extends FileProvider {
    private final String uri_mask = String.format("content://%s/external_files/", ImageFileProvider.class.getCanonicalName());

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        if (uri.toString().startsWith(uri_mask)) {
            String absolutePath = uri.toString().replace(uri_mask, "/storage/emulated/0/");
            MatrixCursor cursor = new MatrixCursor(new String[] { android.provider.MediaStore.MediaColumns.DATA });
            cursor.addRow(new Object[] { absolutePath });
            return cursor;
        }
        return super.query(uri, projection, selection, selectionArgs, sortOrder);
    }
}
