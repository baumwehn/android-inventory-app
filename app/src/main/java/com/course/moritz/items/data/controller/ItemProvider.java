package com.course.moritz.items.data.controller;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.course.moritz.items.data.model.ItemContract;
import com.course.moritz.items.data.model.ItemContract.ItemEntry;
import com.course.moritz.items.data.persistence.ItemDbHelper;

public class ItemProvider extends ContentProvider {

    public static final String TAG = ItemProvider.class.getSimpleName();
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    private static final int ITEMS = 100;
    private static final int ITEM_ID = 101;

    static {
        sUriMatcher.addURI(ItemContract.CONTENT_AUTHORITY, ItemContract.PATH_ITEMS, ITEMS);
        sUriMatcher.addURI(ItemContract.CONTENT_AUTHORITY, ItemContract.PATH_ITEMS + "/#", ITEM_ID);
    }

    private ItemDbHelper mItemDbHelper;

    @Override
    public boolean onCreate() {
        if(mItemDbHelper == null){
            mItemDbHelper = new ItemDbHelper(getContext());
        }
        return true;
    }

    @Override
    public Cursor query(Uri uri,
                        String[] projection,
                        String selection,
                        String[] selectionArgs,
                        String sortOrder) {
        SQLiteDatabase db = mItemDbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            int match = sUriMatcher.match(uri);
            switch (match) {
                case ITEMS:
                    cursor = db.query(ItemContract.ItemEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
                    break;
                case ITEM_ID:
                    selection = ItemContract.ItemEntry._ID + "=?";
                    selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                    cursor = db.query(ItemContract.ItemEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
                    break;
                default:
                    throw new IllegalArgumentException();
            }
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Cannot query unknown URI " + uri, e);
        }
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case ITEMS:
                return ItemEntry.CONTENT_LIST_TYPE;
            case ITEM_ID:
                return ItemEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case ITEMS:
                return insertItem(uri, values);
            default:
                return null;
        }
    }

    @Override
    public int delete(Uri uri,
                      String selection,
                      String[] selectionArgs) {
        SQLiteDatabase db = mItemDbHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsDeleted = 0;
        switch (match) {
            case ITEMS:
                rowsDeleted = db.delete(ItemEntry.TABLE_NAME, null, null);
                break;
            case ITEM_ID:
                selection = ItemEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                rowsDeleted = db.delete(ItemEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                break;
        }
        if (rowsDeleted > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        db.close();
        Log.i(TAG, "close db now");
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri,
                      ContentValues contentValues,
                      String selection,
                      String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case ITEMS:
                return updateItem(uri, contentValues, selection, selectionArgs);
            case ITEM_ID:
                selection = ItemEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                return updateItem(uri, contentValues, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    private Uri insertItem(Uri uri, ContentValues values) {
        long id = -1;
        //validates data
        if (checkValues(values)) {
            SQLiteDatabase db = mItemDbHelper.getWritableDatabase();
            id = db.insert(ItemEntry.TABLE_NAME, null, values);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return ContentUris.withAppendedId(uri, id);
    }

    private boolean checkValues(ContentValues values) {
        try {
            for (String key : values.keySet()) {
                if (values.containsKey(key)) {
                    Object value = values.get(key);
                    if (!isValidValue(key, value)) {
                        throw new IllegalArgumentException(key);
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Bad Argument", e);
            Toast.makeText(getContext(), "Bad Insert with " + e.getMessage() + ". Please check data.", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    /**
     * Helper method that valides data.
     *
     * @param key
     * @param v   value
     */
    private <K, V> boolean isValidValue(String key, V v) {
        switch (key) {
            case ItemEntry.COLUMN_ITEM_NAME:
                return v instanceof String && !((String) v).isEmpty();
            case ItemEntry.COLUMN_ITEM_PRICE:
                return v instanceof Double && ((Double) v > 0);
            case ItemEntry.COLUMN_ITEM_QUANTITY:
                return v instanceof Integer && ((Integer) v >= 0);
            case ItemEntry.COLUMN_ITEM_SUPPLIER_NAME:
                return v instanceof String && !((String) v).isEmpty();
            case ItemEntry.COLUMN_ITEM_SUPPLIER_PHONE:
                return v instanceof String && !((String) v).isEmpty();
            default:
                return false;
        }
    }

    /**
     * Update items in the database with the given content values. Apply the changes to the rows
     * specified in the selection and selection arguments (which could be 0 or 1 or more items).
     * Return the number of rows that were successfully updated.
     */
    private int updateItem(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if (!checkValues(values) || values.size() == 0) {
            return 0;
        }
        SQLiteDatabase db = mItemDbHelper.getWritableDatabase();
        int rowsUpdated = db.update(ItemEntry.TABLE_NAME, values, selection, selectionArgs);
        if (rowsUpdated > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        db.close();
        Log.i(TAG, "close db now");
        return rowsUpdated;
    }
}
