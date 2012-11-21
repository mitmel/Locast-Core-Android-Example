package edu.mit.mobile.android.locast.example.data;

/*
 * Copyright (C) 2011-2012  MIT Mobile Experience Lab
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import edu.mit.mobile.android.content.ProviderUtils;
import edu.mit.mobile.android.content.UriPath;
import edu.mit.mobile.android.content.column.DBColumn;
import edu.mit.mobile.android.content.column.IntegerColumn;
import edu.mit.mobile.android.content.column.TextColumn;
import edu.mit.mobile.android.content.m2m.M2MManager;
import edu.mit.mobile.android.locast.data.Authorable;
import edu.mit.mobile.android.locast.data.Favoritable;
import edu.mit.mobile.android.locast.data.JsonSyncableItem;
import edu.mit.mobile.android.locast.data.SyncMap;
import edu.mit.mobile.android.locast.data.Titled;
import edu.mit.mobile.android.locast.example.R;

@UriPath(Collection.PATH)
public class Collection extends JsonSyncableItem implements Favoritable.Columns,
        Authorable.Columns, Titled.Columns {
    public final static String PATH = "collection";

    public final static Uri CONTENT_URI = ProviderUtils
            .toContentUri(LocastProvider.AUTHORITY, PATH);

    public final static String SERVER_PATH = "collection/";

    @DBColumn(type = TextColumn.class)
    public static final String COL_CASTS_URI = "casts";

    @DBColumn(type = IntegerColumn.class)
    public static final String COL_CASTS_COUNT = "casts_count";

    @DBColumn(type = IntegerColumn.class)
    public static final String _FAVORITES_COUNT = "favorites_count";

    public final static String SORT_DEFAULT = COL_TITLE + " ASC";

    public Collection(Cursor c) {
        super(c);
    }

    @Override
    public Uri getContentUri() {
        return CONTENT_URI;
    }

    public static final M2MManager CASTS = new M2MManager(Cast.class);

    @Override
    public SyncMap getSyncMap() {
        return SYNC_MAP;
    }

    public static final ItemSyncMap SYNC_MAP = new ItemSyncMap();

    public static final String TYPE_DIR = "vnd.android.cursor.dir/vnd.edu.mit.mobile.android.locast.example.collection";
    public static final String TYPE_ITEM = "vnd.android.cursor.item/vnd.edu.mit.mobile.android.locast.example.collection";

    public static class ItemSyncMap extends JsonSyncableItem.ItemSyncMap {
        /**
         *
         */
        private static final long serialVersionUID = 6975192764581466901L;

        public ItemSyncMap() {
            super();

            putAll(Favoritable.SYNC_MAP);
            putAll(Titled.SYNC_MAP);
            putAll(Authorable.SYNC_MAP);

            put(COL_CASTS_COUNT, new SyncFieldMap("casts_count", SyncFieldMap.INTEGER,
                    SyncFieldMap.SYNC_FROM));
            put(_FAVORITES_COUNT, new SyncFieldMap("favorites", SyncFieldMap.INTEGER,
                    SyncFieldMap.SYNC_FROM));
            put(COL_CASTS_URI, new SyncChildRelation("casts",
                    new SyncChildRelation.SimpleRelationship("casts"), false,
                    SyncFieldMap.SYNC_FROM | SyncFieldMap.FLAG_OPTIONAL));

            // TODO load path
        }
    }

    public Uri getChildDirUri(Uri parent, String relation) {
        if ("casts".equals(relation)) {
            return CASTS.getUri(parent);
        }
        return null;
    }

    public static CharSequence getTitle(Context context, Cursor c) {
        CharSequence title = c.getString(c.getColumnIndexOrThrow(COL_TITLE));
        if (title == null || title.length() == 0) {
            title = context.getText(R.string.untitled);
        }

        return title;
    }
}
