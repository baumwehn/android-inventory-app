/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.course.moritz.items.activity;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.course.moritz.items.ItemCursorAdapter;
import com.course.moritz.items.R;
import com.course.moritz.items.data.model.ItemContract.ItemEntry;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnItemClick;

public class InventoryActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String[] PROJECTION = new String[]{
            ItemEntry._ID,
            ItemEntry.COLUMN_ITEM_NAME,
            ItemEntry.COLUMN_ITEM_PRICE,
            ItemEntry.COLUMN_ITEM_QUANTITY,
            ItemEntry.COLUMN_ITEM_SUPPLIER_NAME,
            ItemEntry.COLUMN_ITEM_SUPPLIER_PHONE
    };

    public static final String TAG = InventoryActivity.class.getSimpleName();
    private static final int ITEM_LOADER = 0;

    ItemCursorAdapter mCursorAdapter;

    @BindView(R.id.item_list_view)
    ListView itemListView;
    @BindView(R.id.empty_view)
    View emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventroy);
        ButterKnife.bind(this);

        itemListView.setEmptyView(emptyView);
        mCursorAdapter = new ItemCursorAdapter(this, null);
        itemListView.setAdapter(mCursorAdapter);
        getSupportLoaderManager().initLoader(ITEM_LOADER, null, this);
    }

    @OnClick(R.id.fab)
    public void onClick(View v) {
        Intent intent = new Intent(InventoryActivity.this, EditorActivity.class);
        startActivity(intent);
    }

    @OnItemClick(R.id.item_list_view)
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = new Intent(InventoryActivity.this, EditorActivity.class);
        intent.putExtra("title", getString(R.string.editor_activity_title_edit_item));
        intent.putExtra("uri", ContentUris.withAppendedId(ItemEntry.CONTENT_URI, id));
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_catalog, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_insert_dummy_data:
                insertItem();
                return true;
            case R.id.action_delete_all_entries:
                showDeleteAllConfirmationDialog();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void deleteItems() {
        int rowsDeletes = getContentResolver().delete(ItemEntry.CONTENT_URI, null, null);
        Log.v(TAG, rowsDeletes + "rows deletes from item database");
    }

    private void insertItem() {
        ContentValues values = new ContentValues();
        values.put(ItemEntry.COLUMN_ITEM_NAME, "Donuts");
        values.put(ItemEntry.COLUMN_ITEM_PRICE, 1.5);
        values.put(ItemEntry.COLUMN_ITEM_QUANTITY, 99);
        values.put(ItemEntry.COLUMN_ITEM_SUPPLIER_NAME, "Jerry's Bakery");
        values.put(ItemEntry.COLUMN_ITEM_SUPPLIER_PHONE, "12345678");
        getContentResolver().insert(ItemEntry.CONTENT_URI, values);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this, ItemEntry.CONTENT_URI, PROJECTION, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        mCursorAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCursorAdapter.swapCursor(null);
    }

    private void showDeleteAllConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_all_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete all items.
                deleteItems();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the item.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

}
