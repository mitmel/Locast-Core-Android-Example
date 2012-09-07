package edu.mit.mobile.android.locast.locastcoreexample.data;

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

import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import edu.mit.mobile.android.content.ProviderUtils;
import edu.mit.mobile.android.content.UriPath;
import edu.mit.mobile.android.content.column.DBColumn;
import edu.mit.mobile.android.content.column.IntegerColumn;
import edu.mit.mobile.android.content.column.TextColumn;
import edu.mit.mobile.android.content.m2m.M2MManager;
import edu.mit.mobile.android.locast.data.Favoritable;
import edu.mit.mobile.android.locast.data.PrivatelyAuthorable;
import edu.mit.mobile.android.locast.data.SyncMap;
import edu.mit.mobile.android.locast.data.TaggableItem;

@UriPath(Collection.PATH)
public class Collection extends TaggableItem implements Favoritable.Columns,
        PrivatelyAuthorable.Columns {
    public final static String PATH = "collections";
    public final static Uri CONTENT_URI = ProviderUtils.toContentUri(ExampleProvider.AUTHORITY,
            PATH);

    public final static String SERVER_PATH = "collection/";

    @DBColumn(type = TextColumn.class)
    public static final String _TITLE = "title";

    @DBColumn(type = TextColumn.class)
    public static final String _DESCRIPTION = "description";

    @DBColumn(type = TextColumn.class)
    public static final String _CASTS_URI = "casts";

    @DBColumn(type = IntegerColumn.class)
    public static final String _CASTS_COUNT = "casts_count";

    @DBColumn(type = IntegerColumn.class)
    public static final String _FAVORITES_COUNT = "favorites_count";

    @DBColumn(type = TextColumn.class)
    public static final String _THUMBNAIL = "thumbnail";

    public final static String SORT_DEFAULT = _TITLE + " ASC";

    public static final String[] PROJECTION = {
        _ID,
        _AUTHOR,
        _AUTHOR_URI,
        _TITLE,
        _DESCRIPTION,
        _PUBLIC_URI,
        _CREATED_DATE,
        _MODIFIED_DATE,
        _THUMBNAIL,
        _DRAFT,
    };

    public Collection(Cursor c) {
        super(c);
    }

    @Override
    public Uri getContentUri() {
        return CONTENT_URI;
    }

    public static final M2MManager CASTS = new M2MManager(Cast.class);

    /**
     * @param collection
     * @return
     * @deprecated use {@link #CASTS}.{@link M2MManager#getUri(Uri)} instead.
     */
    @Deprecated
    public static Uri getCastsUri(Uri collection) {
        if (ContentUris.parseId(collection) == -1) {
            throw new IllegalArgumentException(collection
                    + " does not appear to be an collection item URI");
        }
        return Uri.withAppendedPath(collection, Cast.PATH);
    }

    @Override
    public SyncMap getSyncMap() {
        return SYNC_MAP;
    }

    public static final ItemSyncMap SYNC_MAP = new ItemSyncMap();

    public static class ItemSyncMap extends TaggableItemSyncMap {
        /**
         *
         */
        private static final long serialVersionUID = 6975192764581466901L;


        public ItemSyncMap() {
            super();

            putAll(Favoritable.SYNC_MAP);

            put(_DESCRIPTION,       new SyncFieldMap("description", SyncFieldMap.STRING, SyncItem.FLAG_OPTIONAL));
            put(_TITLE,             new SyncFieldMap("title", SyncFieldMap.STRING));
            put(_THUMBNAIL,         new SyncFieldMap("preview_image",   SyncFieldMap.STRING, SyncFieldMap.SYNC_FROM|SyncItem.FLAG_OPTIONAL));
            put(_CASTS_COUNT,       new SyncFieldMap("casts_count", SyncFieldMap.INTEGER, SyncFieldMap.SYNC_FROM));
            put(_FAVORITES_COUNT,   new SyncFieldMap("favorites", SyncFieldMap.INTEGER, SyncFieldMap.SYNC_FROM));
            put(_CASTS_URI,         new SyncChildRelation("casts", new SyncChildRelation.SimpleRelationship("casts"), false, SyncFieldMap.SYNC_FROM | SyncFieldMap.FLAG_OPTIONAL));

            remove(_PRIVACY);
        }
    }

    public Uri getChildDirUri(Uri parent, String relation) {
        if("casts".equals(relation)){
            return CASTS.getUri(parent);
        }
        return null;
    }
}
