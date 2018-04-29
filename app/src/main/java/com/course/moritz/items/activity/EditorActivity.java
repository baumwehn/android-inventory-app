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

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.NavUtils;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.course.moritz.items.R;
import com.course.moritz.items.data.model.ItemContract;
import com.course.moritz.items.data.model.ItemContract.ItemEntry;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.course.moritz.items.activity.InventoryActivity.PROJECTION;

public class EditorActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String TAG = EditorActivity.class.getSimpleName();
    private static final int ITEM_LOADER = 0;
    @BindView(R.id.edit_item_name)
    EditText mEditName;
    @BindView(R.id.edit_item_price)
    EditText mEditPrice;
    @BindView(R.id.edit_item_quantity)
    TextView mEditQuantity;
    @BindView(R.id.edit_item_supplier_name)
    EditText mEditSupplierName;
    @BindView(R.id.edit_item_supplier_phone)
    EditText mEditSupplierPhone;
    @BindView(R.id.btn_decrease)
    Button mButtonDecrease;
    @BindView(R.id.btn_increase)
    Button mButtonIncrease;
    @BindView(R.id.change_value_spinner)
    Spinner mSpinner;
    private Uri mCurrentItemUri;
    private boolean mItemHasChanged;
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mItemHasChanged = true;
            return false;
        }
    };

    @OnClick(R.id.btn_decrease)
    public void decrease(View v) {
        int quantity;
        int change = getChange();
        try {
            quantity = Integer.parseInt(mEditQuantity.getText().toString());
        } catch (NumberFormatException e) {
            quantity = 0;
        }
        if (quantity > 0) {
            quantity -= change;
            if(quantity < 0){
                quantity = 0;
            }
        }
        mEditQuantity.setText(String.valueOf(quantity));
    }

    private int getChange() {
        String item = String.valueOf(mSpinner.getSelectedItem());
        return Integer.parseInt(item);
    }

    @OnClick(R.id.btn_increase)
    public void increase(View v) {
        int quantity;
        int change = getChange();
        try {
            quantity = Integer.parseInt(mEditQuantity.getText().toString());
        } catch (NumberFormatException e) {
            quantity = 0;
        }
        quantity += change;
        mEditQuantity.setText(String.valueOf(quantity));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getIntent().getExtras();


        if (bundle != null && bundle.containsKey("title")) {
            setTitle(bundle.getString("title"));
            mCurrentItemUri = (Uri) bundle.get("uri");
            getSupportLoaderManager().initLoader(ITEM_LOADER, null, this);
        } else {
            setTitle(R.string.editor_activity_title_new_item);
            invalidateOptionsMenu();
        }
        setContentView(R.layout.activity_editor);
        ButterKnife.bind(this);


        mEditName.setOnTouchListener(mTouchListener);
        mEditPrice.setOnTouchListener(mTouchListener);
        mEditSupplierName.setOnTouchListener(mTouchListener);
        mEditSupplierPhone.setOnTouchListener(mTouchListener);
        mButtonDecrease.setOnTouchListener(mTouchListener);
        mButtonIncrease.setOnTouchListener(mTouchListener);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,R.array.value_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(adapter);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new item, hide the "Delete" menu item.
        if (mCurrentItemUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog
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

    @Override
    public void onBackPressed() {
        // If the item hasn't changed, continue with handling back button press
        if (!mItemHasChanged) {
            super.onBackPressed();
            return;
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };

        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    private Uri saveItem() {
        String nameString = mEditName.getText().toString().trim();
        String priceString = mEditPrice.getText().toString().trim();
        String quantityString = mEditQuantity.getText().toString().trim();
        String supplierNameString = mEditSupplierName.getText().toString().trim();
        String supplierPhoneString = mEditSupplierPhone.getText().toString().trim();

        double price = validatePrice(priceString);
        int quantity = validateQuantity(quantityString);

        ContentValues values = new ContentValues();
        values.put(ItemContract.ItemEntry.COLUMN_ITEM_NAME, nameString);
        values.put(ItemContract.ItemEntry.COLUMN_ITEM_PRICE, price);
        values.put(ItemContract.ItemEntry.COLUMN_ITEM_QUANTITY, quantity);
        values.put(ItemContract.ItemEntry.COLUMN_ITEM_SUPPLIER_NAME, supplierNameString);
        values.put(ItemContract.ItemEntry.COLUMN_ITEM_SUPPLIER_PHONE, supplierPhoneString);

        if (mCurrentItemUri == null) {
            return getContentResolver().insert(ItemEntry.CONTENT_URI, values);
        } else {
            int rowsAffected = getContentResolver().update(mCurrentItemUri, values, null, null);
            if (rowsAffected == 0) {
                Toast.makeText(this, getString(R.string.editor_update_item_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.editor_update_item_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }
        return null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                // Save item to database
                Uri uri = saveItem();
                // Exit activity
                if (uri == null) {
                    finish();
                    return true;
                }
                if (!uri.getPath().endsWith("-1")) {
                    finish();
                    return true;
                }
                return true;
            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                showDeleteConfirmationDialog();
                // Do nothing for now
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If the item hasn't changed, continue with navigating up to parent activity
                // which is the {@link InventoryActivity}.
                if (!mItemHasChanged) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                }

                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            }
                        };

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
            case R.id.action_call_supplier:
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", mEditSupplierPhone.getText().toString(), null));
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the item.
                deleteItem();
                finish();
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

    private void deleteItem() {
        int rowsDeleted = getContentResolver().delete(mCurrentItemUri, null, null);
        if (rowsDeleted == 1) {
            Toast.makeText(this, getString(R.string.editor_delete_item_successful),
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this, mCurrentItemUri, PROJECTION, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor.moveToFirst()) {
            int nameIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_NAME);
            int priceIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_PRICE);
            int quantityIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_QUANTITY);
            int supplierNameIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_SUPPLIER_NAME);
            int supplierPhoneIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_SUPPLIER_PHONE);

            String name = cursor.getString(nameIndex);
            double price = cursor.getDouble(priceIndex);
            int quantity = cursor.getInt(quantityIndex);
            String supplierName = cursor.getString(supplierNameIndex);
            String supplierPhone = cursor.getString(supplierPhoneIndex);

            mEditName.setText(name);
            mEditPrice.setText(String.valueOf(price));
            mEditQuantity.setText(String.valueOf(quantity));
            mEditSupplierName.setText(supplierName);
            mEditSupplierPhone.setText(supplierPhone);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    private int validateQuantity(String quantityString) {
        int quantity = 0;
        try {
            if (!quantityString.isEmpty()) {
                quantity = Integer.parseInt(quantityString);
            }
            if (quantity < 0) {
                quantity = 0;
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Bad Format", e);
        }
        return quantity;
    }

    private double validatePrice(String priceString) {
        double price = 0.0;
        try {
            if (!priceString.isEmpty()) {
                price = Double.valueOf(priceString);
            }
            if (price < 0) {
                price = 0;
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Bad Format", e);
        }
        return price;
    }
}
