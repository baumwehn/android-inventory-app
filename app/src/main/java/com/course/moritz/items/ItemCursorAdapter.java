package com.course.moritz.items;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.course.moritz.items.data.model.ItemContract.ItemEntry;


public class ItemCursorAdapter extends CursorAdapter {

    private static final String TAG = ItemCursorAdapter.class.getSimpleName();

    public ItemCursorAdapter(Context context, Cursor cursor) {
        super(context, cursor, 0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    @Override
    public void bindView(View view, final Context context, final Cursor cursor) {
        final TextView nameTextView = (TextView) view.findViewById(R.id.list_item_name);
        final TextView priceTextView = (TextView) view.findViewById(R.id.list_item_price);
        final TextView quantityTextView = (TextView) view.findViewById(R.id.list_item_quantity);
        final TextView supplierNameTextView = (TextView) view.findViewById(R.id.list_item_supplier_name);
        final TextView supplierPhoneTextView = (TextView) view.findViewById(R.id.list_item_supplier_phone);

        final Button button = (Button) view.findViewById(R.id.btn_sale);

        String name = cursor.getString(cursor.getColumnIndexOrThrow(ItemEntry.COLUMN_ITEM_NAME));
        double price = cursor.getDouble(cursor.getColumnIndexOrThrow(ItemEntry.COLUMN_ITEM_PRICE));
        int quantity = cursor.getInt(cursor.getColumnIndexOrThrow(ItemEntry.COLUMN_ITEM_QUANTITY));
        String supplierName = cursor.getString(cursor.getColumnIndexOrThrow(ItemEntry.COLUMN_ITEM_SUPPLIER_NAME));
        String supplierPhone = cursor.getString(cursor.getColumnIndexOrThrow(ItemEntry.COLUMN_ITEM_SUPPLIER_PHONE));

        nameTextView.setText(name);
        priceTextView.setText(String.valueOf(price));
        quantityTextView.setText(String.valueOf(quantity));
        supplierNameTextView.setText(supplierName);
        supplierPhoneTextView.setText(supplierPhone);

        final int position = cursor.getPosition();
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cursor.moveToPosition(position);
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(ItemEntry._ID));
                Log.i(TAG, "id: " + id);

                int quantity;
                try {
                    quantity = Integer.parseInt(quantityTextView.getText().toString());
                } catch (NumberFormatException e) {
                    quantity = 0;
                }
                if (quantity > 0) {
                    quantity -= 1;
                }
                quantityTextView.setText(String.valueOf(quantity));

                ContentValues values = new ContentValues();
                values.put(ItemEntry.COLUMN_ITEM_QUANTITY, quantity);

                Uri uri = ContentUris.withAppendedId(ItemEntry.CONTENT_URI, id);
                context.getContentResolver().update(uri, values, null, null);
            }
        });
    }
}
