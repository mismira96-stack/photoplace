package com.example.gallerysorter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;

/**
 * Dedicated translucent Activity for handling document picker and restore confirmation.
 * Completely isolates ActivityResult logic from MainActivity.
 */
public final class SettingsBackupActivity extends Activity {
    private static final int REQUEST_PICK_FILE = 9182;

    public static void launch(Context context) {
        Intent intent = new Intent(context, SettingsBackupActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            String[] mimeTypes = new String[]{"application/json", "text/plain", "application/octet-stream"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    Uri downloadUri = Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload%2FPhotoPlace");
                    intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, downloadUri);
                } catch (Exception ignored) {}
            }
            try {
                startActivityForResult(intent, REQUEST_PICK_FILE);
            } catch (Exception e) {
                try {
                    Intent fallback = new Intent(Intent.ACTION_GET_CONTENT);
                    fallback.setType("*/*");
                    fallback.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
                    startActivityForResult(fallback, REQUEST_PICK_FILE);
                } catch (Exception ex) {
                    MemoryActionDialog.show(this, "파일 선택 오류",
                            "파일 선택기를 열 수 없습니다.", "확인", this::finish, null, null);
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_FILE) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                Uri uri = data.getData();
                SettingsBackupController.RestoreResult result = SettingsBackupController.restoreFromUri(this, uri);
                if (result.success) {
                    MemoryActionDialog.show(this, "기록 복원 완료", result.message, "확인", this::finish, null, null);
                } else {
                    MemoryActionDialog.show(this, "복원 실패", result.message, "확인", this::finish, null, null);
                }
            } else {
                finish();
            }
        }
    }
}
